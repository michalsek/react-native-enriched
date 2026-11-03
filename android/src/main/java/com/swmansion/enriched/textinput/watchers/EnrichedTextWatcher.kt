package com.swmansion.enriched.textinput.watchers

import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.swmansion.enriched.common.EnrichedConstants
import com.swmansion.enriched.textinput.EnrichedTextInputView
import com.swmansion.enriched.textinput.events.OnChangeTextEvent
import com.swmansion.enriched.textinput.spans.EnrichedInputAlignmentSpan
import com.swmansion.enriched.textinput.spans.EnrichedSpans
import com.swmansion.enriched.textinput.utils.ParagraphMarginSpacers

class EnrichedTextWatcher(
  private val view: EnrichedTextInputView,
) : TextWatcher {
  private var endCursorPosition: Int = 0
  private var startCursorPosition: Int = 0
  private var previousTextLength: Int = 0

  private var deletedText: String = ""
  private var anchorAlignmentToRestore: String? = null
  private var removesAlignmentAnchor: Boolean = false
  private var isApplyingStyles: Boolean = false

  override fun beforeTextChanged(
    s: CharSequence?,
    start: Int,
    count: Int,
    after: Int,
  ) {
    previousTextLength = s?.length ?: 0
    deletedText = if (count > 0 && s != null) s.substring(start, start + count) else ""

    anchorAlignmentToRestore = null
    removesAlignmentAnchor = false

    // When a ZWS is being deleted, check whether it was anchoring a list or paragraph
    // style. If so, capture the co-located alignment value so AlignmentStyles can
    // re-anchor it after the deletion completes.
    if (deletedText == EnrichedConstants.ZWS_STRING && s is Spannable) {
      val isBlockAnchor =
        EnrichedSpans.listSpans.values
          .any { config -> s.getSpans(start, start + 1, config.clazz).isNotEmpty() } ||
          EnrichedSpans.paragraphSpans.values
            .any { config -> s.getSpans(start, start + 1, config.clazz).isNotEmpty() }

      if (isBlockAnchor) {
        anchorAlignmentToRestore =
          s
            .getSpans(start, start + 1, EnrichedInputAlignmentSpan::class.java)
            .firstOrNull()
            ?.cssValue
      } else {
        removesAlignmentAnchor = s.getSpans(start, start + 1, EnrichedInputAlignmentSpan::class.java).isNotEmpty()
      }
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

    val isNestedChange = isApplyingStyles
    if (!view.isDuringTransaction && !isNestedChange && s.length < previousTextLength) {
      mergeParagraphsJoinedAtMarginSpacers()
    }
    emitEvents(s)

    if (view.isDuringTransaction) {
      updateSpaceKerning()
      return
    }
    isApplyingStyles = true
    try {
      applyStyles(s)
    } finally {
      isApplyingStyles = isNestedChange
    }
    if (!isNestedChange) {
      view.normalizeParagraphMarginSpacers()
      updateSpaceKerning()
    }
    view.layoutManager.invalidateLayout()
  }

  private fun mergeParagraphsJoinedAtMarginSpacers() {
    val textLength = previousTextLength
    val removedText = deletedText
    val alignmentToRestore = anchorAlignmentToRestore
    val isRemovingMany = view.isRemovingMany
    val removesParagraphBreak = removesAlignmentAnchor
    val join = view.mergeParagraphsJoinedAtMarginSpacers(endCursorPosition, removesParagraphBreak)
    if (join == endCursorPosition) return

    previousTextLength = textLength
    deletedText = if (removesParagraphBreak) "\n" else removedText
    anchorAlignmentToRestore = alignmentToRestore
    removesAlignmentAnchor = false
    view.isRemovingMany = isRemovingMany
    startCursorPosition = join
    endCursorPosition = join
  }

  private fun applyStyles(s: Editable) {
    view.inlineStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
    view.customStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
    view.paragraphStyles?.afterTextChanged(s, endCursorPosition, previousTextLength)
    view.listStyles?.afterTextChanged(s, endCursorPosition, previousTextLength)
    view.alignmentStyles?.afterTextChanged(s, endCursorPosition, deletedText, anchorAlignmentToRestore)
    view.shortcutsHandler?.afterTextChanged(s, endCursorPosition, previousTextLength)
    view.parametrizedStyles?.afterTextChanged(s, startCursorPosition, endCursorPosition)
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
