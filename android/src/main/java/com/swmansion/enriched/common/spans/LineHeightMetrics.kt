package com.swmansion.enriched.common.spans

import android.graphics.Paint
import android.text.Spanned
import com.swmansion.enriched.common.spans.interfaces.EnrichedSpacerLineSpan
import kotlin.math.ceil
import kotlin.math.floor

fun CharSequence.isSpacerLine(
  start: Int,
  end: Int,
): Boolean {
  val spanned = this as? Spanned ?: return false
  return spanned.getSpans(start, end, EnrichedSpacerLineSpan::class.java).any { span ->
    val spanStart = spanned.getSpanStart(span)
    spanStart >= start && spanStart < end
  }
}

fun Paint.FontMetricsInt.centerInLineHeight(lineHeightPx: Float) {
  if (lineHeightPx <= 0f) return

  val currentHeight = (descent - ascent).toFloat()
  val extra = lineHeightPx - currentHeight
  val topExtra = ceil(extra / 2.0f).toInt()
  val bottomExtra = floor(extra / 2.0f).toInt()

  ascent -= topExtra
  descent += bottomExtra

  if (extra >= 0f) {
    top = minOf(top, ascent)
    bottom = maxOf(bottom, descent)
  } else {
    // CSS line-height is the exact line box height, so a taller glyph box
    // overflows it; top/bottom would otherwise grow the line back.
    top = ascent
    bottom = descent
  }
}
