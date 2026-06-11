package com.swmansion.enriched.textinput.spans

import android.text.Layout
import com.swmansion.enriched.common.spans.EnrichedAlignmentSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

class EnrichedInputAlignmentSpan(
  alignment: Layout.Alignment,
  htmlStyle: HtmlStyle,
) : EnrichedAlignmentSpan(alignment, htmlStyle),
  EnrichedInputSpan {
  override val dependsOnHtmlStyle: Boolean = false

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputAlignmentSpan = EnrichedInputAlignmentSpan(alignment, htmlStyle)
}
