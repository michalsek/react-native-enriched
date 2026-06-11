package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedFontFamilySpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputFontFamilySpan(
  fontFamily: String,
  private val htmlStyle: HtmlStyle,
) : EnrichedFontFamilySpan(fontFamily, htmlStyle.assets),
  EnrichedInputValueSpan {
  override val dependsOnHtmlStyle: Boolean = false

  override val styleValue: Any = fontFamily

  override fun copySpan(): EnrichedInputFontFamilySpan = EnrichedInputFontFamilySpan(fontFamily, htmlStyle)

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputFontFamilySpan = EnrichedInputFontFamilySpan(fontFamily, htmlStyle)
}
