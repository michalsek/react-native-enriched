package com.swmansion.enriched.common.spans.interfaces

import android.text.Spanned

interface EnrichedSpacerLineSpan

fun CharSequence.isSpacerLine(
  start: Int,
  end: Int,
): Boolean {
  val spanned = this as? Spanned ?: return false
  return spanned.getSpans(start, end, EnrichedSpacerLineSpan::class.java).any { span ->
    val spanStart = spanned.getSpanStart(span)
    spanStart >= start && spanStart < end
  }
}
