package com.swmansion.enriched.textinput.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spanned
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan
import android.text.style.UpdateLayout
import com.swmansion.enriched.common.spans.interfaces.EnrichedSpacerLineSpan

class EnrichedParagraphMarginSpacerSpan(
  val kind: Kind,
  val heightPx: Int,
) : ReplacementSpan(),
  LineHeightSpan,
  EnrichedSpacerLineSpan,
  UpdateLayout {
  enum class Kind {
    TOP,
    BOTTOM,
  }

  override fun getSize(
    paint: Paint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?,
  ): Int {
    if (fm != null) {
      fm.top = -heightPx
      fm.ascent = -heightPx
      fm.descent = 0
      fm.bottom = 0
    }

    return 0
  }

  // The line's newline run brings the paint's font metrics, so the spacer sets the line box itself.
  override fun chooseHeight(
    text: CharSequence,
    start: Int,
    end: Int,
    spanstartv: Int,
    v: Int,
    fm: Paint.FontMetricsInt,
  ) {
    val spanStart = (text as? Spanned)?.getSpanStart(this) ?: return
    if (spanStart < start || spanStart >= end) return

    fm.top = -heightPx
    fm.ascent = -heightPx
    fm.descent = 0
    fm.bottom = 0
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence?,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint,
  ) = Unit
}
