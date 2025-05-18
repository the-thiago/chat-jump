package com.thiago.chatjump.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.ChatRepository
import com.thiago.chatjump.domain.usecase.CreateConversationTitleUseCase
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
import com.thiago.chatjump.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getAIResponseUseCase: GetAIResponseUseCase,
    private val createConversationTitleUseCase: CreateConversationTitleUseCase,
    private val chatRepository: ChatRepository,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val eventChannel = Channel<ChatUiEvent>()
    val events = eventChannel.receiveAsFlow()

    private var currentConversationId: Int? = null
    private var newChat = true

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
                    )
                }
                viewModelScope.launch {
                    eventChannel.send(ChatUiEvent.ScrollToBottom)
                }

                // Save user message
                viewModelScope.launch {
                    currentConversationId?.let { conversationId ->
                        chatRepository.saveMessage(conversationId, userMessage)
                    }
                }

                if (newChat) {
                    newChat = false
                    viewModelScope.launch {
                        try {
                            createConversationTitleUseCase(userMessage).collect { title ->
                                // Create new conversation with the generated title
                                currentConversationId = chatRepository.createConversation(title)
                                // Save the user message to the new conversation
                                currentConversationId?.let { conversationId ->
                                    chatRepository.saveMessage(conversationId, userMessage)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error generating title: ${e.message}")
                        }
                    }
                }

                // Get AI response
                viewModelScope.launch {
                    try {
                        delay(300L) // Just to see the thinking bubble
                        var accumulatedResponse = ""
                        
                        getAIResponseUseCase(_state.value.messages).collect { response ->
                            accumulatedResponse += response
                            _state.update { 
                                it.copy(
                                    currentStreamingMessage = accumulatedResponse,
                                    isThinking = false,
                                )
                            }
                            eventChannel.send(ChatUiEvent.ScrollToBottom)
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
                                isThinking = false,
                            )
                        }

                        // Save AI message
                        currentConversationId?.let { conversationId ->
                            chatRepository.saveMessage(conversationId, aiMessage)
                        }
                    } catch (e: Exception) {
                        _state.update { 
                            it.copy(
                                isThinking = false,
                                currentStreamingMessage = "",
                            )
                        }
                        // TODO: Handle error
                    }
                    eventChannel.send(ChatUiEvent.ScrollToBottom)
                }
            }
            is ChatEvent.OnPlayResponse -> {
                textToSpeechManager.stop()
                textToSpeechManager.speak(event.text)
                _state.update { it.copy(speakingMessageId = event.messageId) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }

    fun loadConversation(conversationId: Int) {
        if (conversationId == -1) {
            currentConversationId = null
            newChat = true
            _state.update { it.copy(messages = emptyList()) }
            return
        }

        currentConversationId = conversationId
        newChat = false
        
        viewModelScope.launch {
            _state.update { it.copy(isThinking = true) }
            try {
                // Load messages from database
                chatRepository.getMessagesForConversation(conversationId).collect { messages ->
                    _state.update { 
                        it.copy(
                            messages = messages,
                            isThinking = false
                        )
                    }
                    eventChannel.send(ChatUiEvent.ScrollToBottom)
                }
            } catch (e: Exception) {
                // TODO: Handle error
                _state.update { it.copy(isThinking = false) }
            }
        }
    }
} 