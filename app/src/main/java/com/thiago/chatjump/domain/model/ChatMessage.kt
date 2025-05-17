package com.thiago.chatjump.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) 