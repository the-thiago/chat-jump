package com.thiago.chatjump.ui.chat

import com.thiago.chatjump.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val speakingMessageId: String? = null,
    val currentStreamingMessage: String = "",
) 