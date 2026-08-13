package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedForegroundColorSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputForegroundColorSpan(
  color: Int,
) : EnrichedForegroundColorSpan(color),
  EnrichedInputValueSpan {
  override val dependsOnHtmlStyle: Boolean = false

  override val styleValue: Any = color

  override fun copySpan(): EnrichedInputForegroundColorSpan = EnrichedInputForegroundColorSpan(color)

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputForegroundColorSpan = EnrichedInputForegroundColorSpan(color)
}
