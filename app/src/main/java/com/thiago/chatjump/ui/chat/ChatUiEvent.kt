package com.thiago.chatjump.ui.chat

sealed interface ChatUiEvent {
    object ScrollToBottom : ChatUiEvent
}