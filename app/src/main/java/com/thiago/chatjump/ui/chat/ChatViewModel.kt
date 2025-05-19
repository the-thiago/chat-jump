package com.thiago.chatjump.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.data.repository.TextToSpeechRepositoryImpl
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.ChatRepository
import com.thiago.chatjump.domain.usecase.CreateConversationTitleUseCase
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getAIResponseUseCase: GetAIResponseUseCase,
    private val createConversationTitleUseCase: CreateConversationTitleUseCase,
    private val chatRepository: ChatRepository,
    private val textToSpeechRepositoryImpl: TextToSpeechRepositoryImpl,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val eventChannel = Channel<ChatUiEvent>()
    val events = eventChannel.receiveAsFlow()

    private var pendingUserMessage: ChatMessage? = null

    private var currentConversationId: Int? = null
    private var newChat = true

    init {
        // Observe speaking / loading state from TTS to update UI flags
        viewModelScope.launch {
            textToSpeechRepositoryImpl.isSpeaking.collect { isSpeaking ->
                _state.update { current ->
                    if (isSpeaking) {
                        // Playback started: move loadingMessageId to speakingMessageId
                        val msgId = current.speakingMessageId ?: current.loadingSpeechMessageId
                        current.copy(speakingMessageId = msgId, loadingSpeechMessageId = null)
                    } else {
                        // Playback stopped: clear flags
                        current.copy(speakingMessageId = null, loadingSpeechMessageId = null)
                    }
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
                removePendingMessage()
                val userMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = event.text,
                    isUser = true
                )
                handleSend(userMessage)
            }
            is ChatEvent.OnPlayResponse -> {
                onPlayResponse(event)
            }
            ChatEvent.OnRetry -> {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    _state.update { it.copy(error = "No internet connection") }
                    return
                }
                viewModelScope.launch {
                    if (pendingUserMessage != null) {
                        createConversationTitle()
                        getAIResponse()
                    }
                    _state.update { it.copy(error = null, canRetry = false) }
                }
            }
            ChatEvent.OnDismissError -> {
                _state.update { it.copy(error = null, canRetry = false) }
            }
        }
    }

    private fun removePendingMessage() {
        // If there's a pending user message (from a previous failed attempt), remove it from UI and database
        pendingUserMessage?.let { messageToRemove ->
            // Remove from UI list
            _state.update { currentState ->
                currentState.copy(
                    messages = currentState.messages.filterNot { it.id == messageToRemove.id },
                    error = null,
                    canRetry = false,
                )
            }

            // Remove from database if it had been saved already
            currentConversationId?.let {
                viewModelScope.launch {
                    chatRepository.deleteMessage(messageToRemove.id)
                }
            }

            // Clear pending reference
            pendingUserMessage = null
        }
    }

    private fun onPlayResponse(event: ChatEvent.OnPlayResponse) {
        if (event.messageId == null) {
            textToSpeechRepositoryImpl.stop()
            _state.update { it.copy(speakingMessageId = null) }
        } else if (event.messageId == state.value.speakingMessageId) {
            textToSpeechRepositoryImpl.stop()
            _state.update { it.copy(speakingMessageId = null) }
        } else {
            textToSpeechRepositoryImpl.stop()
            val isCached = event.text?.let { textToSpeechRepositoryImpl.isTextCached(it) } ?: true
            if (isCached) {
                _state.update { it.copy(speakingMessageId = event.messageId, loadingSpeechMessageId = null) }
            } else {
                _state.update { it.copy(loadingSpeechMessageId = event.messageId, speakingMessageId = null) }
            }
            event.text?.let { textToSpeechRepositoryImpl.speak(it) }
        }
    }

    private fun handleSend(userMessage: ChatMessage) {
        pendingUserMessage = userMessage
        // Add user message to the ui list
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + userMessage,
                    inputText = "",
                    isThinking = true,
                )
            }
            delay(50L) // Just to make sure the thinking bubble is visible
            eventChannel.send(ChatUiEvent.ScrollToBottom)
        }
        viewModelScope.launch {
            createConversationTitle()
            getAIResponse()
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechRepositoryImpl.shutdown()
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
        }
    }

    private suspend fun getAIResponse() {
        try {
            eventChannel.send(ChatUiEvent.ScrollToBottom)
            delay(200L) // To Show the thinking bubble

            var accumulatedResponse = ""

            currentConversationId?.let { conversationId ->
                chatRepository.saveMessage(conversationId, pendingUserMessage!!)
            }

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
                chatRepository.saveMessage(conversationId, aiMessage)
            }

            pendingUserMessage = null
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting AI response: ${e.message}")
            setTryAgainState()
        }
        eventChannel.send(ChatUiEvent.ScrollToBottom)
    }

    private suspend fun createConversationTitle() {
        _state.update { it.copy(isThinking = true, error = null, canRetry = false) }
        if (newChat) {
            try {
                createConversationTitleUseCase(pendingUserMessage!!).collect { title ->
                    // Create new conversation with the generated title
                    currentConversationId = chatRepository.createConversation(title)
                    newChat = false
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating title, it will generated again when this conversation is opened again or on retry: ${e.message}")
                setTryAgainState()
            }
        }
    }

    private fun setTryAgainState() {
        _state.update {
            it.copy(
                isThinking = false,
                currentStreamingMessage = "",
                error = "Failed to respond",
                canRetry = true,
            )
        }
    }
}