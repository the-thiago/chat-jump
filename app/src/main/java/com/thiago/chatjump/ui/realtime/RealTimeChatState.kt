package com.thiago.chatjump.ui.realtime

import androidx.compose.runtime.Immutable
import com.thiago.chatjump.domain.model.ChatMessage

@Immutable
data class RealTimeChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioWaveform: List<Float> = emptyList(),
    val scrollToBottom: Boolean = false
) 