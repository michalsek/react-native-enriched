package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedLetterSpacingSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputLetterSpacingSpan(
  letterSpacing: Float,
  private val htmlStyle: HtmlStyle,
) : EnrichedLetterSpacingSpan(letterSpacing, htmlStyle.allowFontScaling),
  EnrichedInputValueSpan {
  // Rebuilding keeps the font scaling configuration up to date
  override val dependsOnHtmlStyle: Boolean = true

  override val styleValue: Any = letterSpacing

  override fun copySpan(): EnrichedInputLetterSpacingSpan = EnrichedInputLetterSpacingSpan(letterSpacing, htmlStyle)

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputLetterSpacingSpan =
    EnrichedInputLetterSpacingSpan(letterSpacing, htmlStyle)
}
