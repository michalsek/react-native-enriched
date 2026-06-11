package com.swmansion.enriched.common.spans

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan

open class EnrichedLetterSpacingSpan(
  val letterSpacing: Float,
  private val allowFontScaling: Boolean,
) : MetricAffectingSpan(),
  EnrichedInlineSpan {
  override fun updateDrawState(paint: TextPaint) {
    apply(paint)
  }

  override fun updateMeasureState(paint: TextPaint) {
    apply(paint)
  }

  private fun apply(paint: TextPaint) {
    // TextPaint.letterSpacing is in EMs, the JS value is in logical units.
    // This mirrors how React Native applies the letterSpacing style prop.
    if (paint.textSize == 0f) return
    paint.letterSpacing = pixelFromSpOrDp(letterSpacing, allowFontScaling) / paint.textSize
  }
}
