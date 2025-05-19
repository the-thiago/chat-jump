package com.thiago.chatjump.domain.model

data class ConversationItem(
    val id: Int,
    val title: String,
    val lastMessage: String,
    val timestamp: Long,
)