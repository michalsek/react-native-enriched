package com.swmansion.enriched.common.spans

import android.graphics.Paint
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.UpdateLayout
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedSpan

open class EnrichedParagraphMarginSpan(
  val marginTop: Float?,
  val marginBottom: Float?,
  private val allowFontScaling: Boolean,
) : LineHeightSpan.WithDensity,
  EnrichedSpan,
  UpdateLayout {
  fun marginTopPx(): Float = marginTop?.let { pixelFromSpOrDp(it, allowFontScaling) } ?: 0f

  fun marginBottomPx(): Float = marginBottom?.let { pixelFromSpOrDp(it, allowFontScaling) } ?: 0f

  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
  ) {
    applyMargins(text, start, end, fm)
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
    applyMargins(text, start, end, fm)
  }

  private fun applyMargins(
    text: CharSequence,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt,
  ) {
    val spanned = text as? Spanned ?: return
    val spanStart = spanned.getSpanStart(this)
    val spanEnd = spanned.getSpanEnd(this)
    if (spanStart < 0 || spanEnd < 0 || spanEnd <= start || spanStart >= end) return

    if (spanStart >= start && spanStart < end) {
      val top = marginTopPx().toInt()
      fm.ascent -= top
      fm.top -= top
    }

    if (spanEnd > start && spanEnd <= end) {
      val bottom = marginBottomPx().toInt()
      fm.descent += bottom
      fm.bottom += bottom
    }
  }
}
