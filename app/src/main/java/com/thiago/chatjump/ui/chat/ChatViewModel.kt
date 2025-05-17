package com.thiago.chatjump.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getAIResponseUseCase: GetAIResponseUseCase
) : ViewModel() {

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

                // Get AI response
                viewModelScope.launch {
                    try {
                        getAIResponseUseCase(_state.value.messages).collect { response ->
                            _state.update { 
                                it.copy(
                                    currentStreamingMessage = response,
                                    isStreaming = true
                                )
                            }
                        }
                        
                        // Add the complete response to messages
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            content = _state.value.currentStreamingMessage,
                            isUser = false
                        )
                        
                        _state.update { 
                            it.copy(
                                messages = it.messages + aiMessage,
                                isThinking = false,
                                isStreaming = false,
                                currentStreamingMessage = "",
                                scrollToBottom = true
                            )
                        }
                    } catch (e: Exception) {
                        _state.update { 
                            it.copy(
                                isThinking = false,
                                isStreaming = false,
                                currentStreamingMessage = ""
                            )
                        }
                        // TODO: Handle error
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

    fun loadConversation(conversationId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isThinking = true) }
            try {
                // TODO: Call usecase to load conversation
                // For now, we'll simulate loading
                kotlinx.coroutines.delay(500)
                
                val messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "Hello! How can I help you today?",
                        isUser = false
                    )
                )
                
                _state.update { 
                    it.copy(
                        messages = messages,
                        isThinking = false
                    )
                }
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }
} 