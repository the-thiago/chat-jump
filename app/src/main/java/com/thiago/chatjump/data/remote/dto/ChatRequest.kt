package com.thiago.chatjump.data.remote.dto

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessageDto>
)