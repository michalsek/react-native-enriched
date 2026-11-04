package com.swmansion.enriched.common

internal fun dropFinalParagraphTerminator(text: CharSequence): CharSequence =
  if (text.endsWith('\n')) text.subSequence(0, text.length - 1) else text
