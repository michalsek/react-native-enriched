package com.swmansion.enriched.common

import android.text.Layout

object EnrichedAlignmentMapping {
  // 'justify' is not supported by Android paragraph spans and 'auto' means the
  // natural alignment - both map to null (no alignment span).
  @JvmStatic
  fun cssToAlignment(value: String?): Layout.Alignment? =
    when (value?.lowercase()) {
      "left" -> Layout.Alignment.ALIGN_NORMAL
      "center" -> Layout.Alignment.ALIGN_CENTER
      "right" -> Layout.Alignment.ALIGN_OPPOSITE
      else -> null
    }

  @JvmStatic
  fun alignmentToCss(alignment: Layout.Alignment): String? =
    when (alignment) {
      Layout.Alignment.ALIGN_NORMAL -> "left"
      Layout.Alignment.ALIGN_CENTER -> "center"
      Layout.Alignment.ALIGN_OPPOSITE -> "right"
      else -> null
    }
}
