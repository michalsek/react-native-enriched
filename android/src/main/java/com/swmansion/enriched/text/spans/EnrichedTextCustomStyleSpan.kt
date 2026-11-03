package com.swmansion.enriched.text.spans

import android.content.res.AssetManager
import com.swmansion.enriched.common.spans.EnrichedCustomStyleLineHeightSpan
import com.swmansion.enriched.common.spans.EnrichedCustomStyleSpan
import com.swmansion.enriched.text.EnrichedTextStyle
import com.swmansion.enriched.text.spans.interfaces.EnrichedTextSpan

open class EnrichedTextCustomStyleSpan(
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
  EnrichedTextSpan {
  override val dependsOnHtmlStyle: Boolean = false

  override fun rebuildWithStyle(style: EnrichedTextStyle): EnrichedTextCustomStyleSpan =
    create(
      getForegroundColor(),
      getBackgroundColor(),
      getFontSize(),
      getFontFamily(),
      getLetterSpacing(),
      getLineHeight(),
      getAssets(),
      style.allowFontScaling,
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
    ): EnrichedTextCustomStyleSpan =
      if ((lineHeight ?: 0f) > 0f) {
        EnrichedTextLineHeightCustomStyleSpan(
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
        EnrichedTextCustomStyleSpan(
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

private class EnrichedTextLineHeightCustomStyleSpan(
  foregroundColor: Int?,
  backgroundColor: Int?,
  fontSize: Float?,
  fontFamily: String?,
  letterSpacing: Float?,
  lineHeight: Float?,
  assets: AssetManager,
  allowFontScaling: Boolean,
) : EnrichedTextCustomStyleSpan(
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
