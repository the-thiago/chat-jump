package com.thiago.chatjump.data.remote.dto

data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String = "alloy",
    val response_format: String = "mp3"
)