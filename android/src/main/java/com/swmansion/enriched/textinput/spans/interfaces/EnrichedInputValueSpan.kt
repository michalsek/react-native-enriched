package com.swmansion.enriched.textinput.spans.interfaces

/**
 * An inline span that carries a parametrized value (font family, font size,
 * letter spacing, line height). Unlike plain inline spans (bold, italic),
 * adjacent spans of the same type can only be merged when their values are
 * equal, and partially overlapping spans must be split preserving the value.
 */
interface EnrichedInputValueSpan : EnrichedInputSpan {
  val styleValue: Any

  /** Creates a new instance of the span carrying the same value. */
  fun copySpan(): EnrichedInputValueSpan
}
