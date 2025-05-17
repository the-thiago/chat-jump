package com.thiago.chatjump.ui.realtime

sealed interface RealTimeChatEvent {
    data class OnInputTextChange(val text: String) : RealTimeChatEvent
    data class OnSendMessage(val text: String) : RealTimeChatEvent
    object OnStartListening : RealTimeChatEvent
    object OnStopListening : RealTimeChatEvent
    object OnScrollToBottom : RealTimeChatEvent
} 