package com.thiago.chatjump.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface TextToSpeechRepository {
    fun speak(text: String)
    fun stop()
    fun shutdown()
    /** Returns true if an audio file for the given text already exists in cache */
    fun isTextCached(text: String): Boolean
    /** Emits true while speech audio is currently playing */
    val isSpeaking: StateFlow<Boolean>
}