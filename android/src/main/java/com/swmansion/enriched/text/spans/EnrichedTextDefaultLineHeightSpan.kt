package com.swmansion.enriched.text.spans

import android.graphics.Paint
import android.text.Spannable
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.EnrichedInlineLineHeightSpan
import com.swmansion.enriched.common.spans.centerInLineHeight

class EnrichedTextDefaultLineHeightSpan(
  private val lineHeight: Float,
  private val allowFontScaling: Boolean,
) : MetricAffectingSpan(),
  LineHeightSpan {
  override fun updateDrawState(tp: TextPaint?) {
    // Do nothing but inform TextView that line height should be recalculated.
  }

  override fun updateMeasureState(textPaint: TextPaint) {
    // Do nothing but inform TextView that line height should be recalculated.
  }

  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
  ) {
    val spannable = text as? Spannable ?: return
    if (spannable.getSpans(start, end, EnrichedInlineLineHeightSpan::class.java).isNotEmpty()) return

    fm.centerInLineHeight(pixelFromSpOrDp(lineHeight, allowFontScaling))
  }
}
