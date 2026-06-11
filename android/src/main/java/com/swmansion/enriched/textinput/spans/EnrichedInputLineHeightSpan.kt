package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedInlineLineHeightSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputLineHeightSpan(
  lineHeight: Float,
  private val htmlStyle: HtmlStyle,
) : EnrichedInlineLineHeightSpan(lineHeight, htmlStyle.allowFontScaling),
  EnrichedInputValueSpan {
  // Rebuilding keeps the font scaling configuration up to date
  override val dependsOnHtmlStyle: Boolean = true

  override val styleValue: Any = lineHeight

  override fun copySpan(): EnrichedInputLineHeightSpan = EnrichedInputLineHeightSpan(lineHeight, htmlStyle)

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputLineHeightSpan = EnrichedInputLineHeightSpan(lineHeight, htmlStyle)
}
