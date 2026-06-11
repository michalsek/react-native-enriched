package com.swmansion.enriched.common.spans

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.interfaces.EnrichedInlineSpan

/**
 * Line height applied to a portion of text. It affects the lines that the
 * spanned text spans. Unlike the input-wide line height span, it is not
 * removed when the input-wide line height changes and it takes precedence
 * over it on the lines it covers.
 */
open class EnrichedInlineLineHeightSpan(
  val lineHeight: Float,
  private val allowFontScaling: Boolean,
) : MetricAffectingSpan(),
  LineHeightSpan,
  EnrichedInlineSpan {
  override fun updateDrawState(p0: TextPaint?) {
    // Do nothing but inform TextView that line height should be recalculated
  }

  override fun updateMeasureState(p0: TextPaint) {
    // Do nothing but inform TextView that line height should be recalculated
  }

  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
  ) {
    val lineHeightPx = pixelFromSpOrDp(lineHeight, allowFontScaling)
    val currentHeight = (fm.descent - fm.ascent).toFloat()
    if (lineHeightPx <= currentHeight) return

    val extra = (lineHeightPx - currentHeight).toInt()
    fm.ascent -= extra
    fm.top = minOf(fm.top, fm.ascent)
  }
}
