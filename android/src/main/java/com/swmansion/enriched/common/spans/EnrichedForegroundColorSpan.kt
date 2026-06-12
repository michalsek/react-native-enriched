package com.swmansion.enriched.common.spans

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan

open class EnrichedForegroundColorSpan(
  val color: Int,
) : CharacterStyle(),
  UpdateAppearance,
  EnrichedInlineSpan {
  override fun updateDrawState(paint: TextPaint) {
    paint.color = color
  }
}
