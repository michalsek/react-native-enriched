package com.swmansion.enriched.common

data class CustomStyle(
  val foregroundColor: Int? = null,
  val backgroundColor: Int? = null,
  val fontSize: Float? = null,
  val fontFamily: String? = null,
  val letterSpacing: Float? = null,
  val lineHeight: Float? = null,
) {
  fun isEmpty(): Boolean =
    foregroundColor == null &&
      backgroundColor == null &&
      (fontSize == null || fontSize <= 0f) &&
      fontFamily.isNullOrBlank() &&
      (letterSpacing == null || letterSpacing == 0f) &&
      (lineHeight == null || lineHeight <= 0f)
}
