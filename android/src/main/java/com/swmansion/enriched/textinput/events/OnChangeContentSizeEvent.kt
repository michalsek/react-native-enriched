package com.swmansion.enriched.textinput.events

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

class OnChangeContentSizeEvent(
  surfaceId: Int,
  viewId: Int,
  private val width: Double,
  private val height: Double,
  private val experimentalSynchronousEvents: Boolean,
) : Event<OnChangeContentSizeEvent>(surfaceId, viewId) {
  override fun getEventName(): String = EVENT_NAME

  override fun getEventData(): WritableMap {
    val eventData: WritableMap = Arguments.createMap()
    eventData.putDouble("width", width)
    eventData.putDouble("height", height)
    return eventData
  }

  override fun experimental_isSynchronous(): Boolean = experimentalSynchronousEvents

  companion object {
    const val EVENT_NAME: String = "onChangeContentSize"
  }
}
