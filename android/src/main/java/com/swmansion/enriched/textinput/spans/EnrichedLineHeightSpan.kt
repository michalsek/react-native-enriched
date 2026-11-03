package com.swmansion.enriched.textinput.spans

import android.graphics.Paint
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.common.pixelFromSpOrDp
import com.swmansion.enriched.common.spans.centerInLineHeight
import com.swmansion.enriched.common.spans.interfaces.EnrichedHeadingSpan
import com.swmansion.enriched.common.spans.tallestCustomLineHeightPx

class EnrichedLineHeightSpan(
  val lineHeight: Float,
  val allowFontScaling: Boolean,
) : MetricAffectingSpan(),
  LineHeightSpan {
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
    if (((text as? Spanned)?.tallestCustomLineHeightPx(start, end) ?: 0f) > 0f) return

    fm.centerInLineHeight(pixelFromSpOrDp(lineHeight, allowFontScaling))
  }
}
