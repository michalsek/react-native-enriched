package com.swmansion.enriched.common.spans

import android.content.res.AssetManager
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import com.facebook.react.common.ReactConstants
import com.facebook.react.views.text.ReactTypefaceUtils.applyStyles
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan

open class EnrichedFontFamilySpan(
  val fontFamily: String,
  private val assets: AssetManager?,
) : MetricAffectingSpan(),
  EnrichedInlineSpan {
  override fun updateDrawState(paint: TextPaint) {
    apply(paint)
  }

  override fun updateMeasureState(paint: TextPaint) {
    apply(paint)
  }

  private fun apply(paint: TextPaint) {
    val assetManager = assets ?: return
    val newTypeface = applyStyles(paint.typeface, ReactConstants.UNSET, ReactConstants.UNSET, fontFamily, assetManager)
    paint.typeface = newTypeface
  }
}
