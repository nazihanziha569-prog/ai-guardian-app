package com.example.ai_guardian.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEvents {
    sealed class Event {
        object OpenChatbot          : Event()
        data class OpenChatbotEmergency(val detectedText: String = "") : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(event: Event) { _events.tryEmit(event) }
}