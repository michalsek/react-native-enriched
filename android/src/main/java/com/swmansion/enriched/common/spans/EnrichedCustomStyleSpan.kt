package com.swmansion.enriched.common.spans

import android.content.res.AssetManager
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import android.util.Log
import com.facebook.react.common.ReactConstants
import com.facebook.react.common.assets.ReactFontManager
import com.facebook.react.views.text.ReactTypefaceUtils.applyStyles
import com.swmansion.enriched.common.CustomStyle
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan
import com.swmansion.enriched.common.spans.interfaces.isSpacerLine
import com.swmansion.enriched.common.withOpacity
import kotlin.math.ceil
import kotlin.math.floor

open class EnrichedCustomStyleSpan(
  private val foregroundColor: Int?,
  private val backgroundColor: Int?,
  private val fontSizeSp: Float?,
  private val fontFamily: String?,
  private val letterSpacing: Float?,
  private val lineHeight: Float?,
  private val assets: AssetManager,
  private val allowFontScaling: Boolean,
) : MetricAffectingSpan(),
  EnrichedInlineSpan {
  private val resolvedFontFamily: String? =
    fontFamily?.trim()?.takeIf { it.isNotEmpty() }?.let { family ->
      if (isFontFamilyAvailable(family)) {
        family
      } else {
        Log.w(TAG, "Font family '$family' not found, keeping the current font.")
        null
      }
    }

  fun getForegroundColor(): Int? = foregroundColor

  fun getBackgroundColor(): Int? = backgroundColor

  fun getFontSize(): Float? = fontSizeSp

  fun getFontFamily(): String? = fontFamily

  fun getLetterSpacing(): Float? = letterSpacing

  fun getLineHeight(): Float? = lineHeight

  fun toCustomStyle(): CustomStyle =
    CustomStyle(
      foregroundColor = foregroundColor,
      backgroundColor = backgroundColor,
      fontSize = fontSizeSp,
      fontFamily = fontFamily,
      letterSpacing = letterSpacing,
      lineHeight = lineHeight,
    )

  protected fun getAssets(): AssetManager = assets

  override fun updateMeasureState(textPaint: TextPaint) {
    applyFontState(textPaint)
  }

  override fun updateDrawState(textPaint: TextPaint) {
    foregroundColor?.let { textPaint.color = it }
    backgroundColor?.let { textPaint.bgColor = withOpacity(it) }
    applyFontState(textPaint)
  }

  private fun applyFontState(textPaint: TextPaint) {
    resolvedFontFamily?.let { family ->
      textPaint.typeface =
        applyStyles(
          textPaint.typeface,
          ReactConstants.UNSET,
          ReactConstants.UNSET,
          family,
          assets,
        )
    }
    fontSizeSp?.takeIf { it > 0f }?.let { size ->
      textPaint.textSize = pixelFromSpOrDp(size, allowFontScaling)
    }
    letterSpacing?.let { spacing ->
      if (textPaint.textSize > 0f) {
        textPaint.letterSpacing = pixelFromSpOrDp(spacing, allowFontScaling) / textPaint.textSize
      }
    }
  }

  internal fun lineHeightPx(): Float? = lineHeight?.takeIf { it > 0f }?.let { pixelFromSpOrDp(it, allowFontScaling) }

  // ReactFontManager resolves custom fonts in such way, we don't have a way
  // of knowing if Typeface.DEFAULT was returned as a fallback to an unavailable
  // font family, or because we requested a default font family ourselves. We
  // compare desired font to the sans-serif family, which is hardcoded as a default
  // one in the Typeface source code
  private fun isFontFamilyAvailable(family: String): Boolean =
    family == DEFAULT_FAMILY ||
      ReactFontManager.getInstance().getTypeface(family, Typeface.NORMAL, assets) != Typeface.DEFAULT

  companion object {
    private const val TAG = "EnrichedCustomStyleSpan"

    // Mirrors the framework's private Typeface.DEFAULT_FAMILY
    private const val DEFAULT_FAMILY = "sans-serif"
  }
}

interface EnrichedCustomStyleLineHeightSpan : LineHeightSpan.WithDensity {
  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
  ) {
    chooseHeight(text, start, end, spanstartv, v, fm, null)
  }

  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
    paint: TextPaint?,
  ) {
    val spanned = text as? Spanned ?: return
    if (spanned.isSpacerLine(start, end)) return
    if (!spanned.coversLine(this, start, end)) return
    if (paint != null) spanned.resetToNaturalMetrics(start, end, fm, paint)
    fm.centerInLineHeight(spanned.tallestCustomLineHeightPx(start, end))
  }
}

private fun Spanned.resetToNaturalMetrics(
  start: Int,
  end: Int,
  fm: Paint.FontMetricsInt,
  paint: TextPaint,
) {
  var top = 0
  var ascent = 0
  var descent = 0
  var bottom = 0

  val runFm = Paint.FontMetricsInt()
  var runStart = start
  while (runStart < end) {
    val runEnd = nextSpanTransition(runStart, end, MetricAffectingSpan::class.java)
    val runPaint = TextPaint(paint)
    for (span in getSpans(runStart, runEnd, MetricAffectingSpan::class.java)) {
      if (getSpanStart(span) < runEnd && getSpanEnd(span) > runStart) {
        span.updateMeasureState(runPaint)
      }
    }
    runPaint.getFontMetricsInt(runFm)
    top = minOf(top, runFm.top)
    ascent = minOf(ascent, runFm.ascent)
    descent = maxOf(descent, runFm.descent)
    bottom = maxOf(bottom, runFm.bottom)
    runStart = runEnd
  }

  fm.top = top
  fm.ascent = ascent
  fm.descent = descent
  fm.bottom = bottom
}

internal fun Paint.FontMetricsInt.centerInLineHeight(lineHeightPx: Float) {
  if (lineHeightPx <= 0f) return

  val extra = lineHeightPx - (descent - ascent)
  ascent -= ceil(extra / 2f).toInt()
  descent += floor(extra / 2f).toInt()

  if (extra >= 0f) {
    top = minOf(top, ascent)
    bottom = maxOf(bottom, descent)
  } else {
    top = ascent
    bottom = descent
  }
}

private fun Spanned.coversLine(
  span: Any,
  start: Int,
  end: Int,
): Boolean = getSpanStart(span) < end && getSpanEnd(span) > start

internal fun Spanned.tallestCustomLineHeightPx(
  start: Int,
  end: Int,
): Float =
  getSpans(start, end, EnrichedCustomStyleSpan::class.java).fold(0f) { tallest, span ->
    if (coversLine(span, start, end)) maxOf(tallest, span.lineHeightPx() ?: 0f) else tallest
  }
