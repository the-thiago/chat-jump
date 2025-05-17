package com.thiago.chatjump.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.data.local.entity.ConversationEntity
import com.thiago.chatjump.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationHistoryState())
    val state: StateFlow<ConversationHistoryState> = _state.asStateFlow()

    init {
        loadConversations()
    }

    fun onEvent(event: ConversationHistoryEvent) {
        when (event) {
            is ConversationHistoryEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                // TODO: Implement search functionality
            }
            ConversationHistoryEvent.OnNewConversationClick -> {
                // Handled by the UI
            }
            ConversationHistoryEvent.OnSearchActiveChange -> {
                _state.update { it.copy(isSearchActive = !it.isSearchActive) }
            }
            ConversationHistoryEvent.OnDismissError -> {
                _state.update { it.copy(error = null) }
            }
            ConversationHistoryEvent.OnRefresh -> {
                loadConversations()
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                chatRepository.getAllConversations().collect { conversations ->
                    _state.update { 
                        it.copy(
                            conversations = conversations.map { conversation ->
                                ConversationItem(
                                    id = conversation.id,
                                    title = conversation.title,
                                    lastMessage = "", // We don't need the last message for now
                                    timestamp = conversation.updatedAt,
                                    messageCount = 0 // We don't need the message count for now
                                )
                            },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Failed to load conversations: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
} 