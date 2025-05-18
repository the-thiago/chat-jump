package com.thiago.chatjump.domain.repository

interface TextToSpeechRepository {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}