package com.thiago.chatjump.data.remote.dto

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val stream: Boolean
)