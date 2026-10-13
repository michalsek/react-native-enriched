package com.swmansion.enriched.common

// The parser terminates every paragraph with a newline; only the last one is not part of the text.
internal fun dropFinalParagraphTerminator(text: CharSequence): CharSequence =
  if (text.endsWith('\n')) text.subSequence(0, text.length - 1) else text
