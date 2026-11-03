package com.swmansion.enriched.common.spans

import android.os.Build
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class EnrichedSpaceKerningSpan(
  val kerningEm: Float,
  val kerningPx: Float,
) : MetricAffectingSpan() {
  override fun updateDrawState(paint: TextPaint) {
    apply(paint)
  }

  override fun updateMeasureState(paint: TextPaint) {
    apply(paint)
  }

  private fun apply(paint: TextPaint) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      paint.wordSpacing += kerningPx
    } else {
      paint.letterSpacing += kerningEm
    }
  }
}
