package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedFontSizeSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputFontSizeSpan(
  fontSize: Float,
  private val htmlStyle: HtmlStyle,
) : EnrichedFontSizeSpan(fontSize, htmlStyle.allowFontScaling),
  EnrichedInputValueSpan {
  // Rebuilding keeps the font scaling configuration up to date
  override val dependsOnHtmlStyle: Boolean = true

  override val styleValue: Any = fontSize

  override fun copySpan(): EnrichedInputFontSizeSpan = EnrichedInputFontSizeSpan(fontSize, htmlStyle)

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputFontSizeSpan = EnrichedInputFontSizeSpan(fontSize, htmlStyle)
}
