package com.swmansion.enriched.common

import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.Log
import com.swmansion.enriched.common.spans.EnrichedFontFamilySpan
import com.swmansion.enriched.common.spans.EnrichedSpaceKerningSpan
import org.json.JSONException
import org.json.JSONObject

class SpaceKerningTable private constructor(
  private val families: Map<String, Pairs>,
) {
  class Pairs(
    val before: Map<Int, Float>,
    val after: Map<Int, Float>,
  )

  fun pairsFor(fontFamily: String?): Pairs? = fontFamily?.let { families[it] }

  companion object {
    fun fromJson(json: String?): SpaceKerningTable? {
      if (json.isNullOrEmpty()) return null

      return try {
        val root = JSONObject(json)
        val families = HashMap<String, Pairs>()
        for (family in root.keys()) {
          val entry = root.optJSONObject(family) ?: continue
          val pairs = Pairs(parseSide(entry.optJSONObject("before")), parseSide(entry.optJSONObject("after")))
          if (pairs.before.isNotEmpty() || pairs.after.isNotEmpty()) families[family] = pairs
        }
        if (families.isEmpty()) null else SpaceKerningTable(families)
      } catch (e: JSONException) {
        Log.w("SpaceKerning", "Invalid spaceKerning: ${e.message}")
        null
      }
    }

    private fun parseSide(side: JSONObject?): Map<Int, Float> {
      if (side == null) return emptyMap()

      val result = HashMap<Int, Float>()
      for (key in side.keys()) {
        if (key.isEmpty() || key.codePointCount(0, key.length) != 1) continue
        val value = side.optDouble(key, 0.0).toFloat()
        if (value != 0f) result[key.codePointAt(0)] = value
      }
      return result
    }
  }
}

object SpaceKerning {
  private const val SPACE = ' '

  fun update(
    text: Spannable,
    table: SpaceKerningTable?,
    defaultFontFamily: String?,
    basePaint: TextPaint,
    rangeStart: Int = 0,
    rangeEnd: Int = text.length,
  ) {
    val start = paragraphStart(text, rangeStart.coerceIn(0, text.length))
    val end = paragraphEnd(text, rangeEnd.coerceIn(start, text.length))

    val existing = HashMap<Int, EnrichedSpaceKerningSpan>()
    for (span in text.getSpans(start, end, EnrichedSpaceKerningSpan::class.java)) {
      val spanStart = text.getSpanStart(span)
      if (spanStart < start || spanStart >= end) continue
      existing.put(spanStart, span)?.let { text.removeSpan(it) }
    }

    if (table != null) {
      val resolver = Resolver(text, defaultFontFamily, basePaint)
      for (index in start until end) {
        if (text[index] != SPACE) continue

        val kerningEm = resolver.kerningEm(table, index, start, end)
        val current = existing.remove(index)
        if (kerningEm == 0f) {
          current?.let { text.removeSpan(it) }
          continue
        }

        val kerningPx = kerningEm * resolver.textSizeAt(index)
        if (current != null && current.kerningEm == kerningEm && current.kerningPx == kerningPx) continue

        current?.let { text.removeSpan(it) }
        text.setSpan(EnrichedSpaceKerningSpan(kerningEm, kerningPx), index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
    }

    for (span in existing.values) text.removeSpan(span)
  }

  private fun paragraphStart(
    text: CharSequence,
    index: Int,
  ): Int {
    var i = index
    while (i > 0 && text[i - 1] != '\n') i--
    return i
  }

  private fun paragraphEnd(
    text: CharSequence,
    index: Int,
  ): Int {
    var i = index
    while (i < text.length && text[i] != '\n') i++
    return i
  }

  private class Resolver(
    private val text: Spanned,
    private val defaultFontFamily: String?,
    private val basePaint: TextPaint,
  ) {
    private val workPaint = TextPaint()

    fun kerningEm(
      table: SpaceKerningTable,
      index: Int,
      paragraphStart: Int,
      paragraphEnd: Int,
    ): Float {
      val fontFamily = fontFamilyAt(index)
      val pairs = table.pairsFor(fontFamily) ?: return 0f

      val previousIndex = if (index > paragraphStart) index - Character.charCount(Character.codePointBefore(text, index)) else -1
      val nextIndex = if (index + 1 < paragraphEnd) index + 1 else -1
      val before = if (previousIndex >= 0) pairs.before[Character.codePointAt(text, previousIndex)] else null
      val after = if (nextIndex >= 0) pairs.after[Character.codePointAt(text, nextIndex)] else null
      if (before == null && after == null) return 0f

      val textSize = textSizeAt(index)
      var kerningEm = 0f
      if (before != null && matches(previousIndex, fontFamily, textSize)) kerningEm += before
      if (after != null && matches(nextIndex, fontFamily, textSize)) kerningEm += after
      return kerningEm
    }

    fun textSizeAt(index: Int): Float {
      workPaint.set(basePaint)
      for (span in text.getSpans(index, index + 1, MetricAffectingSpan::class.java)) {
        if (span is EnrichedSpaceKerningSpan) continue
        span.updateMeasureState(workPaint)
      }
      return workPaint.textSize
    }

    private fun matches(
      index: Int,
      fontFamily: String?,
      textSize: Float,
    ): Boolean = fontFamilyAt(index) == fontFamily && textSizeAt(index) == textSize

    private fun fontFamilyAt(index: Int): String? =
      text.getSpans(index, index + 1, EnrichedFontFamilySpan::class.java).lastOrNull()?.fontFamily ?: defaultFontFamily
  }
}
