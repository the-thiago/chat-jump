package com.thiago.chatjump.ui.realtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class RealTimeChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(RealTimeChatState())
    val state: StateFlow<RealTimeChatState> = _state.asStateFlow()

    fun onEvent(event: RealTimeChatEvent) {
        when (event) {
            is RealTimeChatEvent.OnInputTextChange -> {
                _state.update { it.copy(inputText = event.text) }
            }
            is RealTimeChatEvent.OnSendMessage -> {
                if (event.text.isBlank()) return
                
                // Add user message
                val userMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = event.text,
                    isUser = true
                )
                
                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + userMessage,
                        inputText = "",
                        isProcessing = true,
                        scrollToBottom = true
                    )
                }

                // TODO: Call usecase to get AI response
                // For now, we'll simulate a response
                viewModelScope.launch {
                    // Simulate processing time
                    kotlinx.coroutines.delay(1000)
                    
                    _state.update { it.copy(isProcessing = false, isSpeaking = true) }
                    
                    // Simulate audio waveform
                    val waveform = List(100) { index ->
                        kotlin.math.sin(index * 0.1f) * 0.5f + 0.5f
                    }
                    
                    _state.update { it.copy(audioWaveform = waveform) }
                    
                    // Simulate speaking time
                    kotlinx.coroutines.delay(2000)
                    
                    // Add the complete response to messages
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "This is a simulated response.",
                        isUser = false
                    )
                    
                    _state.update { 
                        it.copy(
                            messages = it.messages + aiMessage,
                            isSpeaking = false,
                            audioWaveform = emptyList(),
                            scrollToBottom = true
                        )
                    }
                }
            }
            RealTimeChatEvent.OnStartListening -> {
                // TODO: Call usecase to start voice recognition
                _state.update { it.copy(isListening = true) }
            }
            RealTimeChatEvent.OnStopListening -> {
                // TODO: Call usecase to stop voice recognition
                _state.update { it.copy(isListening = false) }
            }
            RealTimeChatEvent.OnScrollToBottom -> {
                _state.update { it.copy(scrollToBottom = false) }
            }
        }
    }
} 