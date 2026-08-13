package com.swmansion.enriched.textinput.styles

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.swmansion.enriched.common.EnrichedAlignmentMapping
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.common.ForceRedrawSpan
import com.swmansion.enriched.textinput.EnrichedTextInputView
import com.swmansion.enriched.textinput.spans.EnrichedInputAlignmentSpan
import com.swmansion.enriched.textinput.spans.EnrichedSpans
import com.swmansion.enriched.textinput.utils.getParagraphBounds
import com.swmansion.enriched.textinput.utils.getSafeSpanBoundaries

class AlignmentStyles(
  private val view: EnrichedTextInputView,
) {
  fun setAlignment(alignmentString: String) {
    val selection = view.selection ?: return
    val spannable = view.text as? SpannableStringBuilder ?: return
    val alignment = EnrichedAlignmentMapping.cssToAlignment(alignmentString)
    var redrawStart = 0
    var redrawEnd = 0

    view.runAsATransaction {
      val (selectionStart, selectionEnd) = selection.getParagraphSelection()
      // Mirrors iOS: when the selection touches a list, the alignment is
      // applied to all contiguous items of that list.
      val start = expandToListBoundary(spannable, selectionStart, backward = true)
      var end = expandToListBoundary(spannable, selectionEnd, backward = false)
      redrawStart = start
      redrawEnd = end

      for (span in spannable.getSpans(start, end, EnrichedInputAlignmentSpan::class.java)) {
        spannable.removeSpan(span)
      }

      if (alignment == null) return@runAsATransaction

      var paragraphStart = start
      while (paragraphStart <= end) {
        val (pStart, pEnd) = spannable.getParagraphBounds(paragraphStart)
        var spanEnd = pEnd

        // Paragraph spans need at least one character to anchor on
        if (pStart == pEnd) {
          spannable.insert(pStart, EnrichedConstants.ZWS_STRING)
          spanEnd = pStart + 1
          end += 1
          redrawEnd = end
        }

        setAlignmentSpan(spannable, pStart, spanEnd, EnrichedInputAlignmentSpan(alignment, view.htmlStyle))

        paragraphStart = spanEnd + 1
      }
    }

    forceRedraw(spannable, redrawStart, redrawEnd)
    selection.validateStyles()
  }

  fun afterTextChanged(
    s: Editable,
    endCursorPosition: Int,
    previousTextLength: Int,
  ) {
    val isBackspace = s.length < previousTextLength
    val (pStart, pEnd) = s.getParagraphBounds(endCursorPosition)
    val spans = s.getSpans(pStart, pEnd, EnrichedInputAlignmentSpan::class.java)

    if (spans.isEmpty()) {
      if (!isBackspace) {
        continueAlignmentOnNewParagraph(s, endCursorPosition, pStart, pEnd)
      }
      return
    }

    val sortedSpans = spans.sortedBy { s.getSpanStart(it) }

    // After merging paragraphs, the first paragraph defines the alignment.
    // Spans pulled in from the removed paragraph are dropped unless the merged
    // paragraph already had an alignment span anchored at its start.
    if (isBackspace && s.getSpanStart(sortedSpans.first()) > pStart) {
      for (span in sortedSpans) {
        s.removeSpan(span)
      }
      return
    }

    // Re-anchor spans to whole paragraphs. This extends a span while typing
    // within an aligned paragraph and splits a span that grew across a newly
    // typed newline into per-paragraph spans.
    val coveredParagraphs = mutableSetOf<Int>()
    for (span in sortedSpans) {
      val spanStart = s.getSpanStart(span)
      val spanEnd = s.getSpanEnd(span)
      s.removeSpan(span)
      if (spanStart < 0 || spanEnd < 0) continue

      var cursor = spanStart
      while (cursor <= spanEnd && cursor <= s.length) {
        val (paragraphStart, paragraphEnd) = s.getParagraphBounds(cursor)
        if (paragraphEnd > paragraphStart && coveredParagraphs.add(paragraphStart)) {
          setAlignmentSpan(s, paragraphStart, paragraphEnd, EnrichedInputAlignmentSpan(span.alignment, view.htmlStyle))
        }
        cursor = paragraphEnd + 1
      }
    }
  }

  // Mirrors the iOS typing attributes behaviour: pressing enter at the end of
  // an aligned paragraph carries the alignment over to the new paragraph.
  private fun continueAlignmentOnNewParagraph(
    s: Editable,
    endCursorPosition: Int,
    pStart: Int,
    pEnd: Int,
  ) {
    // Only continue right after a newline was typed (the cursor sits at the
    // beginning of a fresh paragraph).
    if (pStart != endCursorPosition || pStart == 0 || s[pStart - 1] != '\n') return

    val (prevStart, prevEnd) = s.getParagraphBounds(pStart - 1)
    val previousSpan =
      s
        .getSpans(prevStart, prevEnd, EnrichedInputAlignmentSpan::class.java)
        .firstOrNull() ?: return

    var spanEnd = pEnd
    if (pStart == pEnd) {
      s.insert(pStart, EnrichedConstants.ZWS_STRING)
      spanEnd = pStart + 1
    }

    setAlignmentSpan(s, pStart, spanEnd, EnrichedInputAlignmentSpan(previousSpan.alignment, view.htmlStyle))
  }

  private fun setAlignmentSpan(
    spannable: Spannable,
    start: Int,
    end: Int,
    span: EnrichedInputAlignmentSpan,
  ) {
    var spanEnd = end
    if (spanEnd < spannable.length && spannable[spanEnd] == '\n') {
      spanEnd += 1
    }

    val (safeStart, safeEnd) = spannable.getSafeSpanBoundaries(start, spanEnd)
    spannable.setSpan(span, safeStart, safeEnd, Spanned.SPAN_PARAGRAPH)
  }

  private fun forceRedraw(
    spannable: Spannable,
    start: Int,
    end: Int,
  ) {
    if (spannable.isEmpty()) return

    var safeStart = start.coerceIn(0, spannable.length)
    var safeEnd = end.coerceIn(safeStart, spannable.length)

    if (safeStart == safeEnd) {
      if (safeEnd < spannable.length) {
        safeEnd += 1
      } else if (safeStart > 0) {
        safeStart -= 1
      }
    }

    if (safeStart == safeEnd) return

    val (spanStart, spanEnd) = spannable.getSafeSpanBoundaries(safeStart, safeEnd)
    if (spanStart == spanEnd) return

    val redrawSpan = ForceRedrawSpan()
    spannable.setSpan(redrawSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.removeSpan(redrawSpan)

    view.invalidate()
    view.requestLayout()
  }

  private fun getListTypeAt(
    spannable: Spannable,
    start: Int,
    end: Int,
  ): Class<*>? {
    for ((_, config) in EnrichedSpans.listSpans) {
      if (spannable.getSpans(start, end, config.clazz).isNotEmpty()) return config.clazz
    }

    return null
  }

  private fun expandToListBoundary(
    spannable: Spannable,
    index: Int,
    backward: Boolean,
  ): Int {
    val (pStart, pEnd) = spannable.getParagraphBounds(index)
    val listType = getListTypeAt(spannable, pStart, pEnd) ?: return if (backward) pStart else pEnd

    if (backward) {
      var boundary = pStart
      while (boundary > 0) {
        val (prevStart, prevEnd) = spannable.getParagraphBounds(boundary - 1)
        if (getListTypeAt(spannable, prevStart, prevEnd) != listType) break
        boundary = prevStart
      }
      return boundary
    }

    var boundary = pEnd
    while (boundary < spannable.length) {
      val (nextStart, nextEnd) = spannable.getParagraphBounds(boundary + 1)
      if (getListTypeAt(spannable, nextStart, nextEnd) != listType) break
      boundary = nextEnd
    }
    return boundary
  }
}
