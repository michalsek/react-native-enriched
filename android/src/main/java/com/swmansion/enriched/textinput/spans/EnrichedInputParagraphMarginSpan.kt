package com.swmansion.enriched.textinput.spans

import com.swmansion.enriched.common.spans.EnrichedParagraphMarginSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputParagraphMarginSpan(
  marginTop: Float?,
  marginBottom: Float?,
  private val htmlStyle: HtmlStyle,
) : EnrichedParagraphMarginSpan(marginTop, marginBottom, htmlStyle.allowFontScaling),
  EnrichedInputSpan {
  override val dependsOnHtmlStyle: Boolean = true

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputParagraphMarginSpan =
    EnrichedInputParagraphMarginSpan(marginTop, marginBottom, htmlStyle)
}
