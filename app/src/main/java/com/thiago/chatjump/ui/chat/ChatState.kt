package com.thiago.chatjump.ui.chat

import com.thiago.chatjump.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val speakingMessageId: String? = null,
    val loadingSpeechMessageId: String? = null,
    val currentStreamingMessage: String = "",
    val error: String? = null,
    val canRetry: Boolean = false,
) 