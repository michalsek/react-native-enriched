package com.swmansion.enriched.textinput.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import android.text.style.UpdateLayout

class EnrichedParagraphMarginSpacerSpan(
  val kind: Kind,
  private val heightPx: Int,
) : ReplacementSpan(),
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
