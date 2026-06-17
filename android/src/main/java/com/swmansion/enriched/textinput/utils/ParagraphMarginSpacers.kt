package com.swmansion.enriched.textinput.utils

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.textinput.spans.EnrichedInputParagraphMarginSpan
import com.swmansion.enriched.textinput.spans.EnrichedParagraphMarginSpacerSpan

object ParagraphMarginSpacers {
  private const val MARKER = '\uE000'
  private val MARKER_STRING = MARKER.toString()

  private data class SpacerRange(
    val start: Int,
    val end: Int,
    val kind: EnrichedParagraphMarginSpacerSpan.Kind,
  )

  private data class MarginRange(
    val start: Int,
    val end: Int,
    val topPx: Int,
    val bottomPx: Int,
  )

  fun normalized(text: CharSequence): SpannableStringBuilder {
    val builder = SpannableStringBuilder(text)
    normalize(builder)
    return builder
  }

  fun normalize(text: Editable) {
    removeSpacers(text)
    insertSpacers(text)
  }

  fun publicText(text: CharSequence): SpannableStringBuilder {
    val builder = SpannableStringBuilder(text)
    removeSpacers(builder)
    removeOrphanMarkers(builder)
    return builder
  }

  fun publicText(
    text: CharSequence,
    start: Int,
    end: Int,
  ): SpannableStringBuilder {
    val safeStart = start.coerceAtLeast(0).coerceAtMost(text.length)
    val safeEnd = end.coerceAtLeast(safeStart).coerceAtMost(text.length)
    return publicText(text.subSequence(safeStart, safeEnd))
  }

  fun coerceIndex(
    text: CharSequence,
    index: Int,
  ): Int {
    val safeIndex = index.coerceIn(0, text.length)
    val spanned = text as? Spannable ?: return safeIndex

    for (range in spacerRanges(spanned)) {
      if (safeIndex <= range.start || safeIndex >= range.end) continue

      return when (range.kind) {
        EnrichedParagraphMarginSpacerSpan.Kind.TOP -> range.end
        EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM -> range.start
      }
    }

    return safeIndex
  }

  fun publicIndexBefore(
    text: CharSequence,
    index: Int,
  ): Int {
    val safeIndex = index.coerceIn(0, text.length)
    var hiddenCount = 0

    for (i in 0 until safeIndex) {
      if (text[i] == EnrichedConstants.ZWS) hiddenCount++
    }

    val spanned = text as? Spannable
    if (spanned != null) {
      for (range in spacerRanges(spanned)) {
        hiddenCount +=
          when {
            safeIndex >= range.end -> range.end - range.start
            safeIndex > range.start -> safeIndex - range.start
            else -> 0
          }
      }
    }

    return safeIndex - hiddenCount
  }

  fun actualIndexForPublicIndex(
    text: CharSequence,
    publicIndex: Int,
  ): Int {
    val target = publicIndex.coerceAtLeast(0)
    val spanned = text as? Spannable
    val ranges = spanned?.let { spacerRanges(it) }.orEmpty()
    var rangeIndex = 0
    var publicCount = 0
    var actualIndex = 0

    while (actualIndex < text.length) {
      val range = ranges.getOrNull(rangeIndex)
      if (range != null && actualIndex >= range.start) {
        actualIndex = range.end
        rangeIndex++
        continue
      }

      if (publicCount == target) {
        return actualIndex
      }

      if (text[actualIndex] != EnrichedConstants.ZWS) {
        publicCount++
      }
      actualIndex++
    }

    return text.length
  }

  private fun removeSpacers(text: Editable) {
    for (range in spacerRanges(text).asReversed()) {
      text.delete(range.start, range.end)
    }
  }

  private fun removeOrphanMarkers(text: Editable) {
    for (i in text.length - 1 downTo 0) {
      if (text[i] == MARKER) {
        text.delete(i, i + 1)
      }
    }
  }

  private fun insertSpacers(text: Editable) {
    val ranges =
      text
        .getSpans(0, text.length, EnrichedInputParagraphMarginSpan::class.java)
        .mapNotNull { span ->
          val start = text.getSpanStart(span)
          val end = text.getSpanEnd(span)
          if (start < 0 || end < 0 || start >= end) return@mapNotNull null

          MarginRange(
            start = start,
            end = end,
            topPx = span.marginTopPx().toInt(),
            bottomPx = span.marginBottomPx().toInt(),
          )
        }.sortedWith(compareByDescending<MarginRange> { it.start }.thenByDescending { it.end })

    for (range in ranges) {
      if (range.bottomPx > 0) {
        val insertAt = range.end.coerceIn(0, text.length)
        text.insert(insertAt, "\n$MARKER")
        text.setSpan(
          EnrichedParagraphMarginSpacerSpan(
            EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM,
            range.bottomPx,
          ),
          insertAt + 1,
          insertAt + 2,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

      if (range.topPx > 0) {
        val insertAt = range.start.coerceIn(0, text.length)
        text.insert(insertAt, "$MARKER_STRING\n")
        text.setSpan(
          EnrichedParagraphMarginSpacerSpan(
            EnrichedParagraphMarginSpacerSpan.Kind.TOP,
            range.topPx,
          ),
          insertAt,
          insertAt + 1,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }
  }

  private fun spacerRanges(text: Spannable): List<SpacerRange> =
    text
      .getSpans(0, text.length, EnrichedParagraphMarginSpacerSpan::class.java)
      .mapNotNull { span ->
        val spanStart = text.getSpanStart(span)
        val spanEnd = text.getSpanEnd(span)
        if (spanStart < 0 || spanEnd < 0) return@mapNotNull null

        when (span.kind) {
          EnrichedParagraphMarginSpacerSpan.Kind.TOP -> {
            val end =
              if (spanEnd < text.length && text[spanEnd] == '\n') {
                spanEnd + 1
              } else {
                spanEnd
              }
            SpacerRange(spanStart, end, span.kind)
          }

          EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM -> {
            val start =
              if (spanStart > 0 && text[spanStart - 1] == '\n') {
                spanStart - 1
              } else {
                spanStart
              }
            SpacerRange(start, spanEnd, span.kind)
          }
        }
      }.filter { it.start < it.end }
      .sortedBy { it.start }
}
