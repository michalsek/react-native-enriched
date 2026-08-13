package com.swmansion.enriched.common.spans

import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedSpan

open class EnrichedParagraphMarginSpan(
  val marginTop: Float?,
  val marginBottom: Float?,
  protected val allowFontScaling: Boolean,
) : EnrichedSpan {
  fun marginTopPx(): Float = marginTop?.let { pixelFromSpOrDp(it, allowFontScaling) } ?: 0f

  fun marginBottomPx(): Float = marginBottom?.let { pixelFromSpOrDp(it, allowFontScaling) } ?: 0f
}
