package com.swmansion.enriched.textinput.utils

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.textinput.spans.EnrichedInputAlignmentSpan
import com.swmansion.enriched.textinput.spans.EnrichedInputParagraphMarginSpan
import com.swmansion.enriched.textinput.spans.EnrichedParagraphMarginSpacerSpan

object ParagraphMarginSpacers {
  private const val MARKER = '\uE000'
  private val MARKER_STRING = MARKER.toString()
  private const val SPACER_LENGTH = 2

  private data class SpacerRange(
    val start: Int,
    val end: Int,
    val kind: EnrichedParagraphMarginSpacerSpan.Kind,
    val span: EnrichedParagraphMarginSpacerSpan? = null,
  )

  private data class Spacer(
    val kind: EnrichedParagraphMarginSpacerSpan.Kind,
    val heightPx: Int,
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
    var hasDeletedSpacers = mergeLinesJoinedToBottomSpacers(text)
    fitMarginSpansToLines(text)

    val existing = spacerRanges(text)
    if (existing.zipWithNext().any { (previous, next) -> previous.end > next.start }) {
      removeSpacers(text)
      insertSpacers(text)
      removeEmptyAlignmentSpans(text)
      return
    }

    val existingByIndex = existing.groupBy { publicIndex(existing, it.start) }
    val targetByIndex = targetSpacers(text, existing)

    for (index in (existingByIndex.keys + targetByIndex.keys).sortedDescending()) {
      val current = existingByIndex[index].orEmpty()
      val target = targetByIndex[index].orEmpty()

      if (current.map { it.kind } == target.map { it.kind } && current.all { it.end - it.start == SPACER_LENGTH }) {
        current.zip(target).forEach { (range, spacer) -> updateHeight(text, range, spacer.heightPx) }
        continue
      }

      current.asReversed().forEach { text.delete(it.start, it.end) }
      hasDeletedSpacers = hasDeletedSpacers || current.isNotEmpty()
      insertSpacers(text, actualIndex(existing, index), target)
    }

    if (hasDeletedSpacers) removeEmptyAlignmentSpans(text)
  }

  private fun removeEmptyAlignmentSpans(text: Editable) {
    for (span in text.getSpans(0, text.length, EnrichedInputAlignmentSpan::class.java)) {
      if (text.getSpanStart(span) == text.getSpanEnd(span)) text.removeSpan(span)
    }
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
      when (range.kind) {
        EnrichedParagraphMarginSpacerSpan.Kind.TOP -> {
          val firstIndex = if (isLineStart(text, range.start)) range.start else range.start + 1
          if (safeIndex in firstIndex until range.end) return range.end
        }

        EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM -> {
          val lastIndex = if (isLineEnd(text, range.end)) range.end else range.end - 1
          if (safeIndex in range.start + 1..lastIndex) return range.start
        }
      }
    }

    return safeIndex
  }

  private fun isLineStart(
    text: CharSequence,
    index: Int,
  ): Boolean = index == 0 || text[index - 1] == '\n'

  private fun isLineEnd(
    text: CharSequence,
    index: Int,
  ): Boolean = index == text.length || text[index] == '\n'

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

  fun mergeParagraphsJoinedAt(
    text: Editable,
    index: Int,
  ): Int {
    var join = index.coerceIn(0, text.length)
    while (join >= SPACER_LENGTH && text[join - 1] == MARKER && (join == text.length || text[join] != '\n')) {
      val kind = spacerKindAt(text, join - 1) ?: break
      if (text[join - SPACER_LENGTH] != '\n') break

      for (span in text.getSpans(join, join, EnrichedInputParagraphMarginSpan::class.java)) {
        if (text.getSpanStart(span) == join) text.removeSpan(span)
      }
      text.delete(join - SPACER_LENGTH, join)
      join -= SPACER_LENGTH
      if (kind == EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM) break
    }
    return join
  }

  private fun spacerKindAt(
    text: Spannable,
    index: Int,
  ): EnrichedParagraphMarginSpacerSpan.Kind? =
    text
      .getSpans(index, index + 1, EnrichedParagraphMarginSpacerSpan::class.java)
      .firstOrNull { text.getSpanStart(it) == index }
      ?.kind

  private fun mergeLinesJoinedToBottomSpacers(text: Editable): Boolean {
    var hasMerged = false
    for (range in spacerRanges(text).asReversed()) {
      if (range.kind != EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM || range.end >= text.length) continue
      if (text[range.end] == '\n' || text[range.end] == MARKER) continue
      if (text.getSpans(range.end, range.end + 1, EnrichedInputParagraphMarginSpan::class.java).isNotEmpty()) continue

      text.delete(range.start, range.end)
      hasMerged = true
    }
    return hasMerged
  }

  private fun fitMarginSpansToLines(text: Editable) {
    val spans = text.getSpans(0, text.length, EnrichedInputParagraphMarginSpan::class.java)
    for (span in spans) {
      val start = text.getSpanStart(span)
      val end = text.getSpanEnd(span)
      if (start < 0 || end <= start) continue

      val isNested =
        spans.any { other ->
          other !== span && text.getSpanStart(other) < end && text.getSpanEnd(other) > start
        }
      if (isNested) continue

      val lineStart = text.getParagraphBounds(start).first
      val lineEnd = text.getParagraphBounds(end - 1).second
      if (lineStart != start || lineEnd != end) {
        text.setSpan(span, lineStart, lineEnd, text.getSpanFlags(span))
      }
    }
  }

  private fun publicIndex(
    existing: List<SpacerRange>,
    index: Int,
  ): Int {
    var removed = 0
    for (range in existing) {
      if (index >= range.end) {
        removed += range.end - range.start
      } else {
        if (index > range.start) removed += index - range.start
        break
      }
    }
    return index - removed
  }

  private fun actualIndex(
    existing: List<SpacerRange>,
    publicIndex: Int,
  ): Int {
    var removed = 0
    for (range in existing) {
      if (range.start - removed >= publicIndex) break
      removed += range.end - range.start
    }
    return publicIndex + removed
  }

  private fun targetSpacers(
    text: Editable,
    existing: List<SpacerRange>,
  ): Map<Int, List<Spacer>> {
    val ranges =
      text
        .getSpans(0, text.length, EnrichedInputParagraphMarginSpan::class.java)
        .mapNotNull { span ->
          val start = text.getSpanStart(span)
          val end = text.getSpanEnd(span)
          if (start < 0 || end < 0 || start >= end) return@mapNotNull null

          val publicStart = publicIndex(existing, start)
          val publicEnd = publicIndex(existing, end)
          if (publicStart >= publicEnd) {
            text.removeSpan(span)
            return@mapNotNull null
          }

          MarginRange(
            start = publicStart,
            end = publicEnd,
            topPx = span.marginTopPx().toInt(),
            bottomPx = span.marginBottomPx().toInt(),
          )
        }.sortedWith(compareByDescending<MarginRange> { it.start }.thenByDescending { it.end })

    val target = HashMap<Int, MutableList<Spacer>>()
    for (range in ranges) {
      if (range.bottomPx > 0) {
        target.getOrPut(range.end) { mutableListOf() }.add(0, Spacer(EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM, range.bottomPx))
      }
      if (range.topPx > 0) {
        target.getOrPut(range.start) { mutableListOf() }.add(0, Spacer(EnrichedParagraphMarginSpacerSpan.Kind.TOP, range.topPx))
      }
    }
    return target
  }

  private fun updateHeight(
    text: Editable,
    range: SpacerRange,
    heightPx: Int,
  ) {
    val span = range.span ?: return
    if (span.heightPx == heightPx) return

    val spanStart = text.getSpanStart(span)
    val spanEnd = text.getSpanEnd(span)
    val flags = text.getSpanFlags(span)
    text.removeSpan(span)
    text.setSpan(EnrichedParagraphMarginSpacerSpan(span.kind, heightPx), spanStart, spanEnd, flags)
  }

  private fun insertSpacers(
    text: Editable,
    index: Int,
    spacers: List<Spacer>,
  ) {
    var insertAt = index
    for (spacer in spacers) {
      when (spacer.kind) {
        EnrichedParagraphMarginSpacerSpan.Kind.TOP -> {
          text.insert(insertAt, "$MARKER_STRING\n")
          text.setSpan(
            EnrichedParagraphMarginSpacerSpan(spacer.kind, spacer.heightPx),
            insertAt,
            insertAt + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
        }

        EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM -> {
          text.insert(insertAt, "\n$MARKER")
          text.setSpan(
            EnrichedParagraphMarginSpacerSpan(spacer.kind, spacer.heightPx),
            insertAt + 1,
            insertAt + 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
        }
      }
      insertAt += SPACER_LENGTH
    }
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
            SpacerRange(spanStart, end, span.kind, span)
          }

          EnrichedParagraphMarginSpacerSpan.Kind.BOTTOM -> {
            val start =
              if (spanStart > 0 && text[spanStart - 1] == '\n') {
                spanStart - 1
              } else {
                spanStart
              }
            SpacerRange(start, spanEnd, span.kind, span)
          }
        }
      }.filter { it.start < it.end }
      .sortedBy { it.start }
}
