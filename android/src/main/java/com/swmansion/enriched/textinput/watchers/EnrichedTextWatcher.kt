package com.swmansion.enriched.textinput.watchers

import android.text.Editable
import android.text.TextWatcher
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.swmansion.enriched.textinput.EnrichedTextInputView
import com.swmansion.enriched.textinput.events.OnChangeTextEvent
import com.swmansion.enriched.textinput.utils.ParagraphMarginSpacers

class EnrichedTextWatcher(
  private val view: EnrichedTextInputView,
) : TextWatcher {
  private var endCursorPosition: Int = 0
  private var startCursorPosition: Int = 0
  private var previousTextLength: Int = 0

  override fun beforeTextChanged(
    s: CharSequence?,
    start: Int,
    count: Int,
    after: Int,
  ) {
    previousTextLength = s?.length ?: 0
    if (s != null && !view.isDuringTransaction) {
      view.textStyles?.beforeTextChanged(s, start, count, after)
    }
  }

  override fun onTextChanged(
    s: CharSequence?,
    start: Int,
    before: Int,
    count: Int,
  ) {
    startCursorPosition = start
    endCursorPosition = start + count
    view.layoutManager.invalidateLayout()
    view.isRemovingMany = !view.isDuringTransaction && before > count + 1
  }

  override fun afterTextChanged(s: Editable?) {
    if (s == null) return
    if (view.isNormalizingParagraphMarginSpacers) return

    if (!view.isDuringTransaction) {
      if (s.length < previousTextLength) mergeParagraphsJoinedAtMarginSpacers()
      applyStyles(s)
      view.normalizeParagraphMarginSpacers()
      updateSpaceKerning()
      view.layoutManager.invalidateLayout()
    } else {
      updateSpaceKerning()
    }
    view.updateTrailingLetterSpacingInset()

    emitEvents(view.text ?: s)
  }

  private fun mergeParagraphsJoinedAtMarginSpacers() {
    val textLength = previousTextLength
    val isRemovingMany = view.isRemovingMany
    val join = view.mergeParagraphsJoinedAtMarginSpacers(endCursorPosition)
    if (join == endCursorPosition) return

    previousTextLength = textLength
    view.isRemovingMany = isRemovingMany
    startCursorPosition = join
    endCursorPosition = join
  }

  private fun applyStyles(s: Editable) {
    view.inlineStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
    view.paragraphStyles?.afterTextChanged(s, endCursorPosition, previousTextLength)
    view.listStyles?.afterTextChanged(s, endCursorPosition, previousTextLength)
    view.parametrizedStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
    view.textStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
    view.alignmentStyles?.afterTextChanged(s, endCursorPosition, previousTextLength)
  }

  private fun updateSpaceKerning() {
    if (view.isSettingValue) return

    view.updateSpaceKerning(
      startCursorPosition - SPACER_INDEX_SLACK,
      endCursorPosition + SPACER_INDEX_SLACK,
    )
  }

  private fun emitChangeText(editable: Editable) {
    if (!view.shouldEmitOnChangeText) {
      return
    }
    val context = view.context as ReactContext
    val surfaceId = UIManagerHelper.getSurfaceId(context)
    val dispatcher = UIManagerHelper.getEventDispatcherForReactTag(context, view.id)
    dispatcher?.dispatchEvent(
      OnChangeTextEvent(
        surfaceId,
        view.id,
        ParagraphMarginSpacers.publicText(editable),
        view.experimentalSynchronousEvents,
      ),
    )
  }

  private fun emitEvents(s: Editable) {
    emitChangeText(s)
    view.spanWatcher?.emitEvent(s, null)
  }

  companion object {
    private const val SPACER_INDEX_SLACK = 4
  }
}
