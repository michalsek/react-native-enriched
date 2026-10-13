package com.swmansion.enriched.textinput.styles

import android.text.Editable
import android.text.Spannable
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.textinput.EnrichedTextInputView
import com.swmansion.enriched.textinput.spans.EnrichedInputFontFamilySpan
import com.swmansion.enriched.textinput.spans.EnrichedInputFontSizeSpan
import com.swmansion.enriched.textinput.spans.EnrichedInputForegroundColorSpan
import com.swmansion.enriched.textinput.spans.EnrichedInputLetterSpacingSpan
import com.swmansion.enriched.textinput.spans.EnrichedInputLineHeightSpan
import com.swmansion.enriched.textinput.spans.EnrichedSpans
import com.swmansion.enriched.textinput.spans.interfaces.EnrichedInputValueSpan
import com.swmansion.enriched.textinput.utils.getParagraphBounds
import com.swmansion.enriched.textinput.utils.getSafeSpanBoundaries

/**
 * Handles inline text styles parametrized by a value: font family, font size,
 * letter spacing and line height.
 *
 * Unlike plain inline styles (bold, italic...), spans of the same type can
 * carry different values, so they can only be merged when the values are
 * equal and have to be split (preserving the value) when a different value is
 * applied to a sub-range.
 */
class TextStyles(
  private val view: EnrichedTextInputView,
) {
  // Values for styles started at a cursor position, applied to the text typed
  // next. Consumed on the first insertion or dropped when the selection moves.
  private val pendingValues = mutableMapOf<String, Any>()
  private var pendingPosition: Int? = null

  // Text styles of a paragraph's whole content that is being deleted.
  private var deletedParagraphValues: Map<String, Any>? = null

  fun setStyleValue(
    name: String,
    value: Any?,
    collapsedSelectionMode: String = "typing",
  ) {
    val selection = view.selection ?: return
    val spannable = view.text as? Spannable ?: return
    val (start, end) = selection.getInlineSelection()

    if (start == end) {
      val (paragraphStart, paragraphEnd) = spannable.getParagraphBounds(start)
      // Text typed into an empty paragraph is its whole content, so the style
      // goes to the paragraph itself in either mode.
      if (isEmptyParagraph(spannable, paragraphStart, paragraphEnd)) {
        if (setEmptyParagraphValue(spannable, name, value, paragraphStart, paragraphEnd)) {
          selection.validateStyles()
          return
        }
      } else if (collapsedSelectionMode == "paragraph") {
        applyValueSpan(spannable, name, paragraphStart, paragraphEnd, value)
        selection.validateStyles()
        return
      }
    }

    if (start == end) {
      // Cursor only - the style will be applied to the text typed next
      if (value == null) {
        view.spanState?.setStart(name, null)
        pendingValues.remove(name)
      } else {
        view.spanState?.setStart(name, start)
        pendingValues[name] = value
        pendingPosition = start
      }
      return
    }

    applyValueSpan(spannable, name, start, end, value)
    selection.validateStyles()
  }

  /**
   * Stores the value on an empty paragraph. The paragraph gets a zero-width
   * space to anchor its spans on, which also cover its newline. A paragraph
   * without text styles of its own first takes the ones it is typing with
   * (e.g. inherited after Enter), so the styles accumulate.
   * Returns false when there is nothing to store (removing from an unanchored
   * paragraph).
   */
  private fun setEmptyParagraphValue(
    spannable: Spannable,
    name: String,
    value: Any?,
    paragraphStart: Int,
    paragraphEnd: Int,
  ): Boolean {
    var contentEnd = paragraphEnd
    val ownValues = getParagraphValues(spannable, paragraphStart, includeNewline(spannable, paragraphEnd))
    val seedValues = if (ownValues.isEmpty()) getActiveValues() else ownValues

    if (paragraphStart == paragraphEnd) {
      if (value == null) {
        if (paragraphEnd < spannable.length) {
          applyValueSpan(spannable, name, paragraphStart, paragraphStart + 1, null)
        }
        return false
      }

      val editable = spannable as? Editable ?: return false
      editable.insert(paragraphStart, EnrichedConstants.ZWS_STRING)
      contentEnd = paragraphStart + 1
      if (view.selectionStart == paragraphStart && view.selectionEnd == paragraphStart) {
        view.setSelection(contentEnd)
      }
    }

    val styledEnd = includeNewline(spannable, contentEnd)
    for ((style, seedValue) in seedValues) {
      if (style != name) {
        applyValueSpan(spannable, style, paragraphStart, styledEnd, seedValue)
      }
    }
    applyValueSpan(spannable, name, paragraphStart, styledEnd, value)
    return true
  }

  private fun isEmptyParagraph(
    text: CharSequence,
    paragraphStart: Int,
    paragraphEnd: Int,
  ): Boolean = (paragraphStart until paragraphEnd).all { text[it] == EnrichedConstants.ZWS }

  private fun includeNewline(
    text: CharSequence,
    position: Int,
  ): Int = if (position < text.length && text[position] == '\n') position + 1 else position

  private fun getParagraphValues(
    spannable: Spannable,
    start: Int,
    end: Int,
  ): Map<String, Any> {
    val values = mutableMapOf<String, Any>()
    if (start >= end) return values

    for ((style, config) in EnrichedSpans.textStyleSpans) {
      for (span in spannable.getSpans(start, end, config.clazz)) {
        val valueSpan = span as? EnrichedInputValueSpan ?: continue
        values[style] = valueSpan.styleValue
        break
      }
    }
    return values
  }

  /**
   * A zero-width space was inserted at the position to anchor an empty
   * paragraph. The paragraph's text styles kept on its newline (or the given
   * ones, when it has none) cover the anchor too.
   */
  fun extendValuesOverAnchor(
    spannable: Spannable,
    anchor: Int,
    fallbackValues: Map<String, Any>,
  ) {
    val styledEnd = includeNewline(spannable, anchor + 1)
    val ownValues = getParagraphValues(spannable, anchor, styledEnd)
    for ((style, value) in ownValues.ifEmpty { fallbackValues }) {
      applyValueSpan(spannable, style, anchor, styledEnd, value)
    }
  }

  fun getActiveValues(): Map<String, Any> {
    val values = mutableMapOf<String, Any>()
    for (style in EnrichedSpans.textStyleSpans.keys) {
      getActiveValue(style)?.let { values[style] = it }
    }
    return values
  }

  /**
   * Makes the value active at the caret position, so the text typed next gets
   * it. Used for an empty paragraph whose styles are kept on its newline.
   */
  fun setPendingValue(
    name: String,
    value: Any,
    position: Int,
  ) {
    if (pendingPosition != position) {
      pendingValues.clear()
    }
    pendingValues[name] = value
    pendingPosition = position
  }

  // The style start of a value started at the caret, kept until the caret moves.
  fun getPendingStart(name: String): Int? {
    val position = pendingPosition ?: return null
    val selection = view.selection ?: return null
    if (selection.start != position || selection.end != position || !pendingValues.containsKey(name)) return null
    return position
  }

  /**
   * Applies (or removes, when value is null) a value span over [start, end).
   * Overlapping spans of the same type are trimmed preserving their values,
   * adjacent spans with an equal value are merged into the new span.
   */
  private fun applyValueSpan(
    spannable: Spannable,
    name: String,
    start: Int,
    end: Int,
    value: Any?,
  ) {
    val clazz = EnrichedSpans.textStyleSpans[name]?.clazz ?: return

    var finalStart = start
    var finalEnd = end

    // Take surrounding characters into account so adjacent spans with the
    // same value get merged instead of producing sibling spans.
    val lookupStart = (start - 1).coerceAtLeast(0)
    val lookupEnd = (end + 1).coerceAtMost(spannable.length)

    for (span in spannable.getSpans(lookupStart, lookupEnd, clazz)) {
      val spanStart = spannable.getSpanStart(span)
      val spanEnd = spannable.getSpanEnd(span)
      if (spanStart < 0 || spanEnd < 0) continue

      val valueSpan = span as? EnrichedInputValueSpan ?: continue
      val overlaps = spanStart < end && spanEnd > start
      val touches = spanEnd == start || spanStart == end

      if (value != null && valueSpan.styleValue == value && (overlaps || touches)) {
        // Same value - absorb the span into the final range
        finalStart = minOf(finalStart, spanStart)
        finalEnd = maxOf(finalEnd, spanEnd)
        spannable.removeSpan(span)
        continue
      }

      if (!overlaps) continue

      // Different value (or removal) - trim the span preserving its value
      spannable.removeSpan(span)
      if (spanStart < start) {
        setValueSpan(spannable, valueSpan.copySpan(), spanStart, start)
      }
      if (spanEnd > end) {
        setValueSpan(spannable, valueSpan.copySpan(), end, spanEnd)
      }
    }

    if (value != null) {
      val span = createSpan(name, value) ?: return
      setValueSpan(spannable, span, finalStart, finalEnd)
    }
  }

  fun beforeTextChanged(
    s: CharSequence,
    start: Int,
    count: Int,
    after: Int,
  ) {
    deletedParagraphValues = null
    val spanned = s as? Spannable ?: return
    val end = start + count
    if (count == 0 || after != 0 || end > s.length) return

    val isWholeParagraph = (start == 0 || s[start - 1] == '\n') && (end == s.length || s[end] == '\n')
    val isVisibleContent = (start until end).none { s[it] == '\n' } && !isEmptyParagraph(s, start, end)
    if (isWholeParagraph && isVisibleContent) {
      deletedParagraphValues = getParagraphValues(spanned, start, start + 1)
    }
  }

  // The emptied paragraph keeps the text styles of its removed content: on its
  // newline (if it has one) and for the text typed next.
  private fun restoreParagraphValues(
    s: Editable,
    position: Int,
    values: Map<String, Any>,
  ) {
    for ((style, value) in values) {
      if (position < s.length && s[position] == '\n') {
        applyValueSpan(s, style, position, position + 1, value)
      }
      view.spanState?.setStart(style, position)
      setPendingValue(style, value, position)
    }
  }

  fun afterTextChanged(
    s: Editable,
    startCursorPosition: Int,
    endCursorPosition: Int,
  ) {
    val deletedValues = deletedParagraphValues
    deletedParagraphValues = null

    if (endCursorPosition > startCursorPosition) {
      // Split spans that are not actively extended when text is inserted
      // inside of them (e.g. after the style was removed at the cursor)
      for ((style, config) in EnrichedSpans.textStyleSpans) {
        if (view.spanState?.getStart(style) != null) continue
        splitSpanOnInsertion(s, config.clazz, startCursorPosition, endCursorPosition)
      }
    }

    // Extend the started styles over the inserted text
    for ((style, config) in EnrichedSpans.textStyleSpans) {
      val start = view.spanState?.getStart(style) ?: continue
      val value = resolveActiveValue(s, style, config.clazz, start, endCursorPosition) ?: continue
      applyValueSpan(s, style, start, endCursorPosition.coerceAtLeast(start), value)
    }

    if (endCursorPosition > startCursorPosition && pendingValues.isNotEmpty()) {
      // Once applied, the values can be resolved from the spans themselves
      pendingValues.clear()
      pendingPosition = null
    }

    val isBackspace = endCursorPosition == startCursorPosition
    if (!isBackspace) return

    if (!deletedValues.isNullOrEmpty()) {
      restoreParagraphValues(s, startCursorPosition, deletedValues)
    }

    // Merge same-value spans that ended up adjacent to each other after
    // deletion, so the HTML output doesn't contain sibling spans.
    for ((_, config) in EnrichedSpans.textStyleSpans) {
      mergeAdjacentSpans(s, config.clazz, startCursorPosition)
    }
  }

  // Called when EnrichedSelection re-validates styles (i.e. selection has
  // changed). Pending values are only valid at the position they were set at.
  fun onValidateStyles() {
    val pendingPos = pendingPosition ?: return
    val selection = view.selection ?: return
    if (selection.start != pendingPos || selection.end != pendingPos) {
      pendingValues.clear()
      pendingPosition = null
    }
  }

  fun removeStyle(
    name: String,
    start: Int,
    end: Int,
  ): Boolean {
    val clazz = EnrichedSpans.textStyleSpans[name]?.clazz ?: return false
    val spannable = view.text as? Spannable ?: return false
    val spans = spannable.getSpans(start, end, clazz)
    if (spans.isEmpty()) return false

    applyValueSpan(spannable, name, start, end, null)
    return true
  }

  fun getStyleRange(): Pair<Int, Int> = view.selection?.getInlineSelection() ?: Pair(0, 0)

  /**
   * Returns the value of the style active at the current selection
   * (a pending value or the value of the span the selection is in).
   */
  fun getActiveValue(name: String): Any? {
    pendingValues[name]?.let { return it }

    val start = view.spanState?.getStart(name) ?: return null
    val clazz = EnrichedSpans.textStyleSpans[name]?.clazz ?: return null
    val spannable = view.text as? Spannable ?: return null

    return spanValueAt(spannable, clazz, start)
  }

  private fun resolveActiveValue(
    s: Editable,
    name: String,
    clazz: Class<*>,
    styleStart: Int,
    endCursorPosition: Int,
  ): Any? {
    pendingValues[name]?.let { return it }

    // Prefer the span the style start is in
    spanValueAt(s, clazz, styleStart)?.let { return it }

    // Fall back to any span within the affected range
    for (span in s.getSpans(styleStart, endCursorPosition, clazz)) {
      val valueSpan = span as? EnrichedInputValueSpan ?: continue
      return valueSpan.styleValue
    }

    return null
  }

  private fun spanValueAt(
    spannable: Spannable,
    clazz: Class<*>,
    position: Int,
  ): Any? {
    var fallback: Any? = null
    for (span in spannable.getSpans(position, position, clazz)) {
      val spanStart = spannable.getSpanStart(span)
      val spanEnd = spannable.getSpanEnd(span)
      if (spanStart < 0 || spanEnd < 0) continue
      val valueSpan = span as? EnrichedInputValueSpan ?: continue

      if (spanStart <= position && spanEnd > position) {
        return valueSpan.styleValue
      }
      fallback = valueSpan.styleValue
    }
    return fallback
  }

  private fun splitSpanOnInsertion(
    spannable: Spannable,
    clazz: Class<*>,
    insertStart: Int,
    insertEnd: Int,
  ) {
    for (span in spannable.getSpans(insertStart, insertEnd, clazz)) {
      val spanStart = spannable.getSpanStart(span)
      val spanEnd = spannable.getSpanEnd(span)
      if (spanStart < 0 || spanEnd < 0) continue
      // Spans that only touch the inserted range don't cover the new text
      if (spanStart >= insertEnd || spanEnd <= insertStart) continue

      val valueSpan = span as? EnrichedInputValueSpan ?: continue
      spannable.removeSpan(span)

      if (spanStart < insertStart) {
        setValueSpan(spannable, valueSpan.copySpan(), spanStart, insertStart)
      }
      if (spanEnd > insertEnd) {
        setValueSpan(spannable, valueSpan.copySpan(), insertEnd, spanEnd)
      }
    }
  }

  private fun mergeAdjacentSpans(
    spannable: Spannable,
    clazz: Class<*>,
    position: Int,
  ) {
    val spans = spannable.getSpans(position, position, clazz)
    if (spans.size < 2) return

    for (left in spans) {
      val leftStart = spannable.getSpanStart(left)
      val leftEnd = spannable.getSpanEnd(left)
      if (leftStart < 0 || leftEnd < 0) continue
      val leftValueSpan = left as? EnrichedInputValueSpan ?: continue

      for (right in spans) {
        if (left === right) continue
        val rightStart = spannable.getSpanStart(right)
        val rightEnd = spannable.getSpanEnd(right)
        if (rightStart < 0 || rightEnd < 0) continue
        val rightValueSpan = right as? EnrichedInputValueSpan ?: continue

        if (leftEnd == rightStart && leftValueSpan.styleValue == rightValueSpan.styleValue) {
          spannable.removeSpan(left)
          spannable.removeSpan(right)
          setValueSpan(spannable, leftValueSpan.copySpan(), leftStart, rightEnd)
          return
        }
      }
    }
  }

  private fun setValueSpan(
    spannable: Spannable,
    span: Any,
    start: Int,
    end: Int,
  ) {
    if (start >= end) return
    val (safeStart, safeEnd) = spannable.getSafeSpanBoundaries(start, end)
    if (safeStart >= safeEnd) return
    spannable.setSpan(span, safeStart, safeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
  }

  private fun createSpan(
    name: String,
    value: Any,
  ): Any? =
    when (name) {
      EnrichedSpans.FONT_FAMILY -> (value as? String)?.let { EnrichedInputFontFamilySpan(it, view.htmlStyle) }
      EnrichedSpans.FONT_SIZE -> (value as? Float)?.let { EnrichedInputFontSizeSpan(it, view.htmlStyle) }
      EnrichedSpans.LETTER_SPACING -> (value as? Float)?.let { EnrichedInputLetterSpacingSpan(it, view.htmlStyle) }
      EnrichedSpans.LINE_HEIGHT -> (value as? Float)?.let { EnrichedInputLineHeightSpan(it, view.htmlStyle) }
      EnrichedSpans.FOREGROUND_COLOR -> (value as? Int)?.let { EnrichedInputForegroundColorSpan(it) }
      else -> null
    }
}
