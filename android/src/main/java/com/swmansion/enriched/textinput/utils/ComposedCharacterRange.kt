package com.swmansion.enriched.textinput.utils

import android.icu.text.BreakIterator

private const val ZERO_WIDTH_JOINER = '‍'

private fun CharSequence.mayBeInsideComposedCharacter(index: Int): Boolean {
  if (index <= 0 || index >= length) return false

  val current = this[index]
  val previous = this[index - 1]
  if (Character.isLowSurrogate(current)) return true
  if (Character.isHighSurrogate(current) && Character.isLowSurrogate(previous)) return true
  if (current == ZERO_WIDTH_JOINER || previous == ZERO_WIDTH_JOINER) return true
  if (current in '︀'..'️') return true

  return when (Character.getType(current).toByte()) {
    Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK -> true
    else -> false
  }
}

fun CharSequence.composedCharacterAlignedRange(
  start: Int,
  end: Int,
): Pair<Int, Int> {
  val rangeStart = minOf(start, end).coerceIn(0, length)
  val rangeEnd = maxOf(start, end).coerceIn(0, length)
  val startMayBeInside = mayBeInsideComposedCharacter(rangeStart)
  val endMayBeInside = rangeEnd != rangeStart && mayBeInsideComposedCharacter(rangeEnd)
  if (!startMayBeInside && !endMayBeInside) return rangeStart to rangeEnd

  val breaks = BreakIterator.getCharacterInstance()
  breaks.setText(toString())

  val alignedStart =
    when {
      !startMayBeInside || breaks.isBoundary(rangeStart) -> rangeStart
      rangeStart == rangeEnd -> breaks.following(rangeStart)
      else -> breaks.preceding(rangeStart)
    }
  if (rangeStart == rangeEnd) return alignedStart to alignedStart

  val alignedEnd =
    if (!endMayBeInside || breaks.isBoundary(rangeEnd)) rangeEnd else breaks.following(rangeEnd)

  return alignedStart to alignedEnd
}
