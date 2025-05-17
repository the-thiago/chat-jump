package com.thiago.chatjump.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnInputTextChange -> {
                _state.update { it.copy(inputText = event.text) }
            }
            is ChatEvent.OnSendMessage -> {
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
                        isThinking = true,
                        scrollToBottom = true
                    )
                }

                // TODO: Call usecase to get AI response
                // For now, we'll simulate a response
                viewModelScope.launch {
                    _state.update { it.copy(isThinking = false, isStreaming = true) }
                    
                    // Simulate streaming response
                    val response = "This is a simulated response that will be streamed token by token."
                    var currentText = ""
                    
                    response.split(" ").forEach { token ->
                        currentText += "$token "
                        _state.update { 
                            it.copy(
                                currentStreamingMessage = currentText,
                                scrollToBottom = true
                            )
                        }
                        kotlinx.coroutines.delay(100) // Simulate network delay
                    }
                    
                    // Add the complete response to messages
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = response,
                        isUser = false
                    )
                    
                    _state.update { 
                        it.copy(
                            messages = it.messages + aiMessage,
                            isStreaming = false,
                            currentStreamingMessage = "",
                            scrollToBottom = true
                        )
                    }
                }
            }
            is ChatEvent.OnPlayResponse -> {
                // TODO: Call usecase to play text-to-speech
            }
            ChatEvent.OnScrollToBottom -> {
                _state.update { it.copy(scrollToBottom = false) }
            }
        }
    }
} 