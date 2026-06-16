package com.swmansion.enriched.common.spans

import android.graphics.Paint
import kotlin.math.ceil
import kotlin.math.floor

fun Paint.FontMetricsInt.expandToCenteredLineHeight(lineHeightPx: Float) {
  val currentHeight = (descent - ascent).toFloat()
  if (lineHeightPx <= currentHeight) return

  val extra = lineHeightPx - currentHeight
  val topExtra = ceil(extra / 2.0f).toInt()
  val bottomExtra = floor(extra / 2.0f).toInt()

  ascent -= topExtra
  descent += bottomExtra
  top = minOf(top, ascent)
  bottom = maxOf(bottom, descent)
}
