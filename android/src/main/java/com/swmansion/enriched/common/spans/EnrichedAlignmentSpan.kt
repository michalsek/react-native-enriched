package com.swmansion.enriched.common.spans

import android.text.Layout
import android.text.style.AlignmentSpan
import android.text.style.UpdateLayout
import com.swmansion.enriched.common.EnrichedStyle
import com.swmansion.enriched.common.spans.interfaces.EnrichedSpan

// UpdateLayout makes the live EditText layout reflow when the span is
// added or removed - AlignmentSpan.Standard alone doesn't trigger it.
@Suppress("UNUSED_PARAMETER")
open class EnrichedAlignmentSpan(
  alignment: Layout.Alignment,
  enrichedStyle: EnrichedStyle,
) : AlignmentSpan.Standard(alignment),
  EnrichedSpan,
  UpdateLayout
