package com.thiago.chatjump.ui.voicechat

data class VoiceChatState(
    val isRecording: Boolean = false,
    val isThinking: Boolean = false,
    val isAiSpeaking: Boolean = false,
    val aiAmplitude: Float = 0f,
    val userAmplitude: Float = 0f,
    val isOffline: Boolean = false,
)