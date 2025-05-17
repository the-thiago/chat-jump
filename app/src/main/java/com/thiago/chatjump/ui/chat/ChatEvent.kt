package com.thiago.chatjump.ui.chat

sealed interface ChatEvent {
    data class OnInputTextChange(val text: String) : ChatEvent
    data class OnSendMessage(val text: String) : ChatEvent
    data class OnPlayResponse(val text: String) : ChatEvent
    object OnScrollToBottom : ChatEvent
} 