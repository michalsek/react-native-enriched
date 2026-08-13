package com.swmansion.enriched.common.spans

import android.graphics.Paint
import android.text.Spanned
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
  LineHeightSpan.WithDensity,
  EnrichedInlineSpan {
  fun lineHeightPx(): Float = pixelFromSpOrDp(lineHeight, allowFontScaling)

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
    // Fallback for layouts that don't provide the paint. Without it the
    // line's natural metrics cannot be recomputed (see the WithDensity
    // overload), so only grow the incoming metrics.
    val spanned = text as? Spanned
    if (spanned != null) {
      val spanStart = spanned.getSpanStart(this)
      val spanEnd = spanned.getSpanEnd(this)
      if (spanEnd <= start || spanStart >= end) return
    }
    expandTo(fm, lineHeightPx())
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
    val spanned = text as? Spanned
    if (spanned == null || paint == null) {
      chooseHeight(text, start, end, spanstartv, v, fm)
      return
    }

    // StaticLayout collects LineHeightSpans per paragraph and calls
    // chooseHeight for every line of that paragraph - skip the lines this
    // span doesn't cover so a large inline line height doesn't inflate the
    // paragraph's other lines (e.g. the wrapped remainder after one
    // enlarged word).
    val spanStart = spanned.getSpanStart(this)
    val spanEnd = spanned.getSpanEnd(this)
    if (spanEnd <= start || spanStart >= end) return

    // When a line wraps inside a measurement run, StaticLayout seeds the new
    // line's metrics from the previous line's already-adjusted metrics, so
    // the incoming fm can smuggle the previous line's chosen height into
    // this one. Rebuild this line's natural metrics from its actual text
    // runs and apply the tallest covering line height on top. Every
    // covering span computes the same result, so the call order of sibling
    // spans doesn't matter.
    resetToNaturalMetrics(spanned, start, end, fm, paint)

    var target = 0f
    for (span in spanned.getSpans(start, end, EnrichedInlineLineHeightSpan::class.java)) {
      val siblingStart = spanned.getSpanStart(span)
      val siblingEnd = spanned.getSpanEnd(span)
      if (siblingEnd <= start || siblingStart >= end) continue
      val px = span.lineHeightPx()
      if (px > target) target = px
    }
    expandTo(fm, target)
  }

  private fun expandTo(
    fm: Paint.FontMetricsInt,
    lineHeightPx: Float,
  ) {
    fm.centerInLineHeight(lineHeightPx)
  }

  private fun resetToNaturalMetrics(
    spanned: Spanned,
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
      val runEnd = spanned.nextSpanTransition(runStart, end, MetricAffectingSpan::class.java)
      val runPaint = TextPaint(paint)
      for (span in spanned.getSpans(runStart, runEnd, MetricAffectingSpan::class.java)) {
        if (spanned.getSpanStart(span) < runEnd && spanned.getSpanEnd(span) > runStart) {
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
}
