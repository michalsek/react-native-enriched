package com.swmansion.enriched.textinput.styles

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import com.swmansion.enriched.common.CustomStyle
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.common.EnrichedSpanFlags
import com.swmansion.enriched.textinput.EnrichedTextInputView
import com.swmansion.enriched.textinput.spans.EnrichedInputCustomStyleSpan
import com.swmansion.enriched.textinput.utils.getParagraphBounds
import com.swmansion.enriched.textinput.utils.safelyInsertZWS
import org.json.JSONObject

class CustomStyles(
  private val view: EnrichedTextInputView,
) {
  private var deletedParagraphStyle: CustomStyle? = null
  private var pendingStyle: CustomStyle? = null
  private var pendingPosition: Int? = null

  fun setStyle(
    styleJSON: String,
    collapsedSelectionMode: String,
  ) {
    val selection = view.selection ?: return
    val spannable = view.text as Spannable
    val (selectionStart, selectionEnd) = selection.getInlineSelection()
    val (paragraphStart, paragraphEnd) = spannable.getParagraphBounds(selectionStart)
    val isCollapsed = selectionStart == selectionEnd
    val isInEmptyParagraph = isCollapsed && isZeroWidthSpaceOnly(spannable, paragraphStart, paragraphEnd)
    val (start, end) =
      if (isCollapsed && !isInEmptyParagraph && collapsedSelectionMode == "paragraph") {
        Pair(paragraphStart, paragraphEnd)
      } else {
        Pair(selectionStart, selectionEnd)
      }

    val json = runCatching { JSONObject(styleJSON) }.getOrNull() ?: return

    val hasFg = json.has("foregroundColor")
    val hasBg = json.has("backgroundColor")
    val hasFontSize = json.has("fontSize")
    val hasFontFamily = json.has("fontFamily")
    val hasLetterSpacing = json.has("letterSpacing")
    val hasLineHeight = json.has("lineHeight")

    val fgColor = if (hasFg && !json.isNull("foregroundColor")) json.getInt("foregroundColor") else null
    val bgColor = if (hasBg && !json.isNull("backgroundColor")) json.getInt("backgroundColor") else null
    val fontSize =
      if (hasFontSize && !json.isNull("fontSize")) {
        json.getDouble("fontSize").toFloat().takeIf { it > 0f }
      } else {
        null
      }
    val fontFamily =
      if (hasFontFamily && !json.isNull("fontFamily")) {
        json.getString("fontFamily").trim().takeIf { it.isNotEmpty() }
      } else {
        null
      }
    val letterSpacing =
      if (hasLetterSpacing && !json.isNull("letterSpacing")) {
        json.getDouble("letterSpacing").toFloat().takeIf { it != 0f }
      } else {
        null
      }
    val lineHeight =
      if (hasLineHeight && !json.isNull("lineHeight")) {
        json.getDouble("lineHeight").toFloat().takeIf { it > 0f }
      } else {
        null
      }

    val styleUpdater: (CustomStyle) -> CustomStyle = { oldStyle ->
      CustomStyle(
        foregroundColor = if (hasFg) fgColor else oldStyle.foregroundColor,
        backgroundColor = if (hasBg) bgColor else oldStyle.backgroundColor,
        fontSize = if (hasFontSize) fontSize else oldStyle.fontSize,
        fontFamily = if (hasFontFamily) fontFamily else oldStyle.fontFamily,
        letterSpacing = if (hasLetterSpacing) letterSpacing else oldStyle.letterSpacing,
        lineHeight = if (hasLineHeight) lineHeight else oldStyle.lineHeight,
      )
    }

    if (isInEmptyParagraph && setEmptyParagraphStyle(spannable, paragraphStart, paragraphEnd, styleUpdater)) {
      selection.validateStyles()
      return
    }

    if (start == end) {
      val merged = styleUpdater(view.spanState?.customStyle ?: CustomStyle())
      view.spanState?.setCustomStyle(
        merged.foregroundColor,
        merged.backgroundColor,
        merged.fontSize,
        merged.fontFamily,
        merged.letterSpacing,
        merged.lineHeight,
      )
    } else {
      applyCustomStyleSpan(view.text as Spannable, start, end, styleUpdater)
      view.selection.validateStyles()
    }
  }

  private fun setEmptyParagraphStyle(
    spannable: Spannable,
    paragraphStart: Int,
    paragraphEnd: Int,
    styleUpdater: (CustomStyle) -> CustomStyle,
  ): Boolean {
    val ownStyle = getParagraphStyle(spannable, paragraphStart, includeNewline(spannable, paragraphEnd))
    val style = styleUpdater(ownStyle ?: view.spanState?.customStyle ?: CustomStyle())
    var contentEnd = paragraphEnd

    if (paragraphStart == paragraphEnd) {
      if (style.isEmpty()) {
        val newlineEnd = includeNewline(spannable, paragraphEnd)
        if (newlineEnd > paragraphStart) {
          applyCustomStyleSpan(spannable, paragraphStart, newlineEnd) { CustomStyle() }
        }
        return false
      }

      val builder = spannable as? SpannableStringBuilder ?: return false
      builder.safelyInsertZWS(paragraphStart)
      contentEnd = paragraphStart + 1
      if (view.selectionStart == paragraphStart && view.selectionEnd == paragraphStart) {
        view.setSelection(contentEnd)
      }
    }

    setParagraphStyle(spannable, paragraphStart, includeNewline(spannable, contentEnd), style)
    return true
  }

  fun extendStyleOverAnchor(
    spannable: Spannable,
    anchor: Int,
    fallbackStyle: CustomStyle?,
  ) {
    val styledEnd = includeNewline(spannable, anchor + 1)
    val style = getParagraphStyle(spannable, anchor, styledEnd) ?: fallbackStyle ?: return
    if (style.isEmpty()) return

    setParagraphStyle(spannable, anchor, styledEnd, style)
  }

  fun pendingStyleAt(position: Int): CustomStyle? {
    if (pendingPosition != position) {
      pendingStyle = null
      pendingPosition = null
    }
    return pendingStyle
  }

  fun beforeTextChanged(
    s: CharSequence,
    start: Int,
    count: Int,
    after: Int,
  ) {
    deletedParagraphStyle = null
    val spannable = s as? Spannable ?: return
    val end = start + count
    if (count == 0 || after != 0 || end > s.length) return

    val paragraphStart = spannable.getParagraphBounds(start).first
    val isWholeParagraph = isZeroWidthSpaceOnly(s, paragraphStart, start) && (end == s.length || s[end] == '\n')
    val isVisibleContent = (start until end).none { s[it] == '\n' } && !isZeroWidthSpaceOnly(s, start, end)
    if (isWholeParagraph && isVisibleContent) {
      deletedParagraphStyle = getParagraphStyle(spannable, start, start + 1)
    }
  }

  private fun restoreParagraphStyle(
    s: Editable,
    position: Int,
    style: CustomStyle,
  ) {
    val paragraphStart = s.getParagraphBounds(position).first
    val styledEnd = includeNewline(s, position)
    if (styledEnd > paragraphStart) {
      setParagraphStyle(s, paragraphStart, styledEnd, style)
    } else {
      pendingStyle = style
      pendingPosition = position
    }
    view.spanState?.setCustomStyle(
      style.foregroundColor,
      style.backgroundColor,
      style.fontSize,
      style.fontFamily,
      style.letterSpacing,
      style.lineHeight,
    )
  }

  private fun getParagraphStyle(
    spannable: Spannable,
    start: Int,
    end: Int,
  ): CustomStyle? {
    if (start >= end) return null
    return spannable
      .getSpans(start, end, EnrichedInputCustomStyleSpan::class.java)
      .firstOrNull()
      ?.toCustomStyle()
      ?.takeIf { !it.isEmpty() }
  }

  private fun setParagraphStyle(
    spannable: Spannable,
    start: Int,
    end: Int,
    style: CustomStyle,
  ) {
    applyCustomStyleSpan(spannable, start, end) { style }
    collapseAdjacentCustomSpans(spannable, start, end)
  }

  private fun isZeroWidthSpaceOnly(
    text: CharSequence,
    start: Int,
    end: Int,
  ): Boolean = (start until end).all { text[it] == EnrichedConstants.ZWS }

  private fun includeNewline(
    text: CharSequence,
    position: Int,
  ): Int = if (position < text.length && text[position] == '\n') position + 1 else position

  private fun applyCustomStyleSpan(
    spannable: Spannable,
    start: Int,
    end: Int,
    styleUpdater: (CustomStyle) -> CustomStyle,
  ) {
    val existingSpans = spannable.getSpans(start, end, EnrichedInputCustomStyleSpan::class.java)
    val boundaries = mutableSetOf(start, end)

    // Snapshot boundaries and spans before any modifications
    val oldSpans =
      existingSpans.mapNotNull { span ->
        val spanStart = spannable.getSpanStart(span)
        val spanEnd = spannable.getSpanEnd(span)
        if (spanStart == -1 || spanEnd == -1) null else Triple(span, spanStart, spanEnd)
      }

    // Remove old spans, restore outer edges, and collect internal boundaries
    for ((span, spanStart, spanEnd) in oldSpans) {
      val style = span.toCustomStyle()

      spannable.removeSpan(span)

      if (spanStart < start) setCustomSpan(spannable, spanStart, start, style)
      if (spanEnd > end) setCustomSpan(spannable, end, spanEnd, style)

      if (spanStart in start..end) boundaries.add(spanStart)
      if (spanEnd in start..end) boundaries.add(spanEnd)
    }

    // Build the new merged spans chunk-by-chunk
    val sortedBoundaries = boundaries.sorted()

    for (i in 0 until sortedBoundaries.size - 1) {
      val chunkStart = sortedBoundaries[i]
      val chunkEnd = sortedBoundaries[i + 1]

      // Find the old span that fully covers this specific chunk
      val oldSpan = oldSpans.firstOrNull { it.second <= chunkStart && it.third >= chunkEnd }?.first
      val oldStyle = oldSpan?.toCustomStyle() ?: CustomStyle()
      val merged = styleUpdater(oldStyle)

      setCustomSpan(spannable, chunkStart, chunkEnd, merged)
    }
  }

  fun afterTextChanged(
    s: Editable,
    startCursorPosition: Int,
    endCursorPosition: Int,
  ) {
    val isInsertion = endCursorPosition > startCursorPosition
    val deletedStyle = deletedParagraphStyle
    deletedParagraphStyle = null

    if (isInsertion) {
      val activeStyle = view.spanState?.customStyle ?: CustomStyle()

      // Split existing spans if they don't match the current active values
      splitCustomSpanOnInsertion(s, startCursorPosition, endCursorPosition, activeStyle)

      setCustomSpan(s, startCursorPosition, endCursorPosition, activeStyle)
      pendingStyle = null
      pendingPosition = null
    } else if (deletedStyle != null) {
      restoreParagraphStyle(s, startCursorPosition, deletedStyle)
    }

    // Merge any adjacent spans that have the exact same style
    collapseAdjacentCustomSpans(s, startCursorPosition, endCursorPosition)
  }

  private fun splitCustomSpanOnInsertion(
    spannable: Spannable,
    insertStart: Int,
    insertEnd: Int,
    activeStyle: CustomStyle,
  ) {
    val spans = spannable.getSpans(insertStart, insertEnd, EnrichedInputCustomStyleSpan::class.java)

    for (span in spans) {
      val spanStart = spannable.getSpanStart(span)
      val spanEnd = spannable.getSpanEnd(span)
      if (spanStart < 0 || spanEnd < 0) continue

      val spanStyle = span.toCustomStyle()

      // If the existing span perfectly matches the active state, leave it
      if (spanStyle == activeStyle) continue

      // Spans differ. We must split the old span so it doesn't cover the new text
      spannable.removeSpan(span)

      if (spanStart < insertStart) {
        setCustomSpan(spannable, spanStart, insertStart, spanStyle)
      }
      if (spanEnd > insertEnd) {
        setCustomSpan(spannable, insertEnd, spanEnd, spanStyle)
      }
    }
  }

  private fun collapseAdjacentCustomSpans(
    spannable: Spannable,
    start: Int,
    end: Int,
  ) {
    // Look slightly outside the typed area to catch adjacent spans
    val searchStart = (start - 1).coerceAtLeast(0)
    val searchEnd = (end + 1).coerceAtMost(spannable.length)

    val spans = spannable.getSpans(searchStart, searchEnd, EnrichedInputCustomStyleSpan::class.java)
    if (spans.isEmpty()) return

    // Sort spans and extract their boundaries simultaneously
    val sortedSpans =
      spans
        .mapNotNull { span ->
          val spanStart = spannable.getSpanStart(span)
          val spanEnd = spannable.getSpanEnd(span)
          if (spanStart == -1 || spanEnd == -1) null else Triple(span, spanStart, spanEnd)
        }.sortedBy { it.second }

    // Wipe all spans in this region immediately (we safely hold their data in sortedSpans)
    sortedSpans.forEach { spannable.removeSpan(it.first) }

    var (_, currentStart, currentEnd) = sortedSpans[0]
    var currentStyle = sortedSpans[0].first.toCustomStyle()

    // Iterate and merge
    for (i in 1 until sortedSpans.size) {
      val (span, spanStart, spanEnd) = sortedSpans[i]
      val spanStyle = span.toCustomStyle()

      // If spans are touching/overlapping AND their values match perfectly extend the span
      if (spanStart <= currentEnd && spanStyle == currentStyle) {
        currentEnd = maxOf(currentEnd, spanEnd)
      } else {
        // Values changed or there is a gap. Commit the current merged block.
        setCustomSpan(spannable, currentStart, currentEnd, currentStyle)

        // Start a new tracking block
        currentStart = spanStart
        currentEnd = spanEnd
        currentStyle = spanStyle
      }
    }

    // Commit the final block
    setCustomSpan(spannable, currentStart, currentEnd, currentStyle)
  }

  private fun setCustomSpan(
    spannable: Spannable,
    start: Int,
    end: Int,
    style: CustomStyle,
  ) {
    if (start >= end || style.isEmpty()) return

    val span =
      EnrichedInputCustomStyleSpan.create(
        style.foregroundColor,
        style.backgroundColor,
        style.fontSize,
        style.fontFamily,
        style.letterSpacing,
        style.lineHeight,
        view.context.assets,
        view.allowFontScaling,
      )
    spannable.setSpan(
      span,
      start,
      end,
      EnrichedSpanFlags.forSpan(span),
    )
  }
}
