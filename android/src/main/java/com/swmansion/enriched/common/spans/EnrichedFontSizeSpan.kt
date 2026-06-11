package com.swmansion.enriched.common.spans

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan
import kotlin.math.ceil

open class EnrichedFontSizeSpan(
  val fontSize: Float,
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
    paint.textSize = ceil(pixelFromSpOrDp(fontSize, allowFontScaling))
  }
}
