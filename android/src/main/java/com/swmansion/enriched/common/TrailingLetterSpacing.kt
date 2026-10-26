package com.swmansion.enriched.common

import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import com.swmansion.enriched.common.spans.EnrichedLetterSpacingSpan
import kotlin.math.abs
import kotlin.math.roundToInt

object TrailingLetterSpacing {
  private const val PROBE_TEXT = "AA"
  private const val PROBE_TEXT_SIZE = 100f
  private const val PROBE_LETTER_SPACING_EM = 0.1f
  private const val PROBE_WIDTH = 10_000

  private val trimsLineEdges: Boolean by lazy { detectLineEdgeTrimming() }

  fun insetPx(text: CharSequence?): Int {
    if (text !is Spanned || text.isEmpty()) return 0

    val spans = text.getSpans(0, text.length, EnrichedLetterSpacingSpan::class.java)
    if (spans.isEmpty() || !trimsLineEdges) return 0

    val coverage = HashMap<Float, Int>()
    var covered = 0
    for (span in spans) {
      val length = text.getSpanEnd(span) - text.getSpanStart(span)
      if (length <= 0) continue

      coverage[span.letterSpacingPx] = (coverage[span.letterSpacingPx] ?: 0) + length
      covered += length
    }
    coverage[0f] = (coverage[0f] ?: 0) + (text.length - covered).coerceAtLeast(0)

    val dominant =
      coverage.entries
        .maxWithOrNull(compareBy<Map.Entry<Float, Int>> { it.value }.thenByDescending { abs(it.key) })
        ?.key ?: 0f
    return dominant.roundToInt()
  }

  private fun detectLineEdgeTrimming(): Boolean {
    val paint = TextPaint().apply { textSize = PROBE_TEXT_SIZE }
    val glyphsWidth = paint.measureText(PROBE_TEXT)
    paint.letterSpacing = PROBE_LETTER_SPACING_EM
    val spacingPx = PROBE_LETTER_SPACING_EM * PROBE_TEXT_SIZE

    val layout = StaticLayout.Builder.obtain(PROBE_TEXT, 0, PROBE_TEXT.length, paint, PROBE_WIDTH).build()
    return layout.getLineWidth(0) < glyphsWidth + spacingPx * (PROBE_TEXT.length - 0.5f)
  }
}
