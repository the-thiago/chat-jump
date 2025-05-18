package com.thiago.chatjump.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.ChatRepository
import com.thiago.chatjump.domain.usecase.CreateConversationTitleUseCase
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
import com.thiago.chatjump.util.TextToSpeechManager
import com.thiago.chatjump.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import android.content.Context

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getAIResponseUseCase: GetAIResponseUseCase,
    private val createConversationTitleUseCase: CreateConversationTitleUseCase,
    private val chatRepository: ChatRepository,
    private val textToSpeechManager: TextToSpeechManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val eventChannel = Channel<ChatUiEvent>()
    val events = eventChannel.receiveAsFlow()

    private var messageToSend: ChatMessage? = null

    private var currentConversationId: Int? = null
    private var newChat = true

    init {
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking) {
                    _state.update { it.copy(speakingMessageId = null) }
                }
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
                
                // Check network connectivity before proceeding
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    _state.update {
                        it.copy(error = "No internet connection", canRetry = false)
                    }
                    return
                }
                val userMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = event.text,
                    isUser = true
                )
                onSendMessage(userMessage)
            }
            is ChatEvent.OnPlayResponse -> {
                if (event.messageId == state.value.speakingMessageId) {
                    textToSpeechManager.stop()
                    _state.update { it.copy(speakingMessageId = null) }
                } else {
                    textToSpeechManager.stop()
                    _state.update { it.copy(speakingMessageId = event.messageId) }
                    textToSpeechManager.speak(event.text)
                }
            }
            ChatEvent.OnRetry -> {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    _state.update { it.copy(error = "No internet connection", canRetry = false) }
                    return
                }
                messageToSend?.let { onSendMessage(it) }
            }
            ChatEvent.OnDismissError -> {
                _state.update { it.copy(error = null, canRetry = false) }
            }
        }
    }

    private fun onSendMessage(userMessage: ChatMessage) {
        messageToSend = userMessage
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + userMessage,
                    inputText = "",
                    isThinking = true,
                )
            }
            delay(50L) // To make sure the thinking bubble is visible
            eventChannel.send(ChatUiEvent.ScrollToBottom)
        }

        viewModelScope.launch {
            currentConversationId?.let { conversationId ->
                chatRepository.saveMessage(conversationId, userMessage)
            }
        }

        createConversationTitle(userMessage)
        getAIResponse()
        messageToSend == null
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
                    // Add a small delay to ensure the UI is ready
                    delay(100)
                    eventChannel.send(ChatUiEvent.ScrollToBottom)
                }
            } catch (e: Exception) {
                // TODO: Handle error
                _state.update { it.copy(isThinking = false) }
            }
        }
    }

    private fun getAIResponse() {
        viewModelScope.launch {
            _state.update { it.copy(isThinking = true, error = null, canRetry = false) }
            try {
                eventChannel.send(ChatUiEvent.ScrollToBottom)
                delay(300L)

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

                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = accumulatedResponse,
                    isUser = false
                )
                eventChannel.send(ChatUiEvent.ScrollToBottom)

                _state.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        currentStreamingMessage = "",
                        isThinking = false,
                    )
                }
                eventChannel.send(ChatUiEvent.ScrollToBottom)

                currentConversationId?.let { conversationId ->
                    // Persist AI message only once (user message was already saved)
                    chatRepository.saveMessage(conversationId, aiMessage)
                }
            } catch (e: Exception) {
                println("thiago e $e")
                _state.update {
                    it.copy(
                        isThinking = false,
                        currentStreamingMessage = "",
                        error = "Failed to fetch response. Please try again.",
                        canRetry = true
                    )
                }
            }
            eventChannel.send(ChatUiEvent.ScrollToBottom)
        }
    }

    private fun createConversationTitle(userMessage: ChatMessage) {
        if (newChat) {
            viewModelScope.launch {
                try {
                    createConversationTitleUseCase(userMessage).collect { title ->
                        // Create new conversation with the generated title
                        currentConversationId = chatRepository.createConversation(title)
                        currentConversationId?.let { conversationId ->
                            chatRepository.saveMessage(conversationId, userMessage)
                        }
                    }
                    newChat = false
                } catch (e: Exception) {
                    println("Error generating title: ${e.message}")
                }
            }
        }
    }
} 