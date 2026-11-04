package com.swmansion.enriched.textinput.spans

import android.content.res.AssetManager
import com.swmansion.enriched.common.spans.EnrichedCustomStyleLineHeightSpan
import com.swmansion.enriched.common.spans.EnrichedCustomStyleSpan
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputSpan
import com.swmansion.enriched.textinput.styles.HtmlStyle

open class EnrichedInputCustomStyleSpan(
  foregroundColor: Int?,
  backgroundColor: Int?,
  fontSize: Float?,
  fontFamily: String?,
  letterSpacing: Float?,
  lineHeight: Float?,
  assets: AssetManager,
  allowFontScaling: Boolean,
) : EnrichedCustomStyleSpan(
    foregroundColor,
    backgroundColor,
    fontSize,
    fontFamily,
    letterSpacing,
    lineHeight,
    assets,
    allowFontScaling,
  ),
  EnrichedInputSpan {
  // font size depends on the htmlStyle's allowFontScaling
  override val dependsOnHtmlStyle: Boolean
    get() = getFontSize() != null || getLetterSpacing() != null || getLineHeight() != null

  override fun rebuildWithStyle(htmlStyle: HtmlStyle): EnrichedInputCustomStyleSpan =
    create(
      getForegroundColor(),
      getBackgroundColor(),
      getFontSize(),
      getFontFamily(),
      getLetterSpacing(),
      getLineHeight(),
      getAssets(),
      htmlStyle.allowFontScaling,
    )

  companion object {
    fun create(
      foregroundColor: Int?,
      backgroundColor: Int?,
      fontSize: Float?,
      fontFamily: String?,
      letterSpacing: Float?,
      lineHeight: Float?,
      assets: AssetManager,
      allowFontScaling: Boolean,
    ): EnrichedInputCustomStyleSpan =
      if ((lineHeight ?: 0f) > 0f) {
        EnrichedInputLineHeightCustomStyleSpan(
          foregroundColor,
          backgroundColor,
          fontSize,
          fontFamily,
          letterSpacing,
          lineHeight,
          assets,
          allowFontScaling,
        )
      } else {
        EnrichedInputCustomStyleSpan(
          foregroundColor,
          backgroundColor,
          fontSize,
          fontFamily,
          letterSpacing,
          lineHeight,
          assets,
          allowFontScaling,
        )
      }
  }
}

private class EnrichedInputLineHeightCustomStyleSpan(
  foregroundColor: Int?,
  backgroundColor: Int?,
  fontSize: Float?,
  fontFamily: String?,
  letterSpacing: Float?,
  lineHeight: Float?,
  assets: AssetManager,
  allowFontScaling: Boolean,
) : EnrichedInputCustomStyleSpan(
    foregroundColor,
    backgroundColor,
    fontSize,
    fontFamily,
    letterSpacing,
    lineHeight,
    assets,
    allowFontScaling,
  ),
  EnrichedCustomStyleLineHeightSpan
