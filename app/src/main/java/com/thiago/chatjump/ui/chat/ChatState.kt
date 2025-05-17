package com.thiago.chatjump.ui.chat

import com.thiago.chatjump.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isStreaming: Boolean = false,
    val currentStreamingMessage: String = "",
    val scrollToBottom: Boolean = false
) 