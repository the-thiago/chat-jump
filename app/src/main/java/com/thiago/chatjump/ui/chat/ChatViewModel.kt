package com.thiago.chatjump.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
import com.thiago.chatjump.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getAIResponseUseCase: GetAIResponseUseCase,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var newChat = false

    init {
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collect { isSpeaking ->
                _state.update { it.copy(isSpeaking = isSpeaking) }
            }
        }
    }

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
                        var accumulatedResponse = ""
                        
                        getAIResponseUseCase(_state.value.messages).collect { response ->
                            accumulatedResponse += response
                            _state.update { 
                                it.copy(
                                    currentStreamingMessage = accumulatedResponse,
                                    scrollToBottom = true,
                                    isThinking = false,
                                )
                            }
                        }

                        // Add the complete response to messages
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            content = accumulatedResponse,
                            isUser = false
                        )

                        _state.update {
                            it.copy(
                                messages = it.messages + aiMessage,
                                currentStreamingMessage = "",
                                scrollToBottom = true,
                                isThinking = false,
                            )
                        }
                    } catch (e: Exception) {
                        _state.update { 
                            it.copy(
                                isThinking = false,
                                currentStreamingMessage = "",
                                scrollToBottom = true,
                            )
                        }
                        // TODO: Handle error
                    }
                }
            }
            is ChatEvent.OnPlayResponse -> {
                if (_state.value.isSpeaking) {
                    textToSpeechManager.stop()
                } else {
                    textToSpeechManager.speak(event.text)
                }
            }
            ChatEvent.OnScrollToBottom -> {
                _state.update { it.copy(scrollToBottom = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }

    fun loadConversation(conversationId: Int) {
        if (conversationId == -1) {
            newChat = true
            return
        }
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