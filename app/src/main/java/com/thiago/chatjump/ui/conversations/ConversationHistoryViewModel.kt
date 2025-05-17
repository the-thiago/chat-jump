package com.thiago.chatjump.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ConversationHistoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(ConversationHistoryState())
    val state: StateFlow<ConversationHistoryState> = _state.asStateFlow()

    init {
        loadConversations()
    }

    fun onEvent(event: ConversationHistoryEvent) {
        when (event) {
            is ConversationHistoryEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                // TODO: Call usecase to search conversations
            }
            is ConversationHistoryEvent.OnConversationClick -> {
                // TODO: Call usecase to load conversation
            }
            ConversationHistoryEvent.OnNewConversationClick -> {
                // TODO: Call usecase to create new conversation
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
                // TODO: Call usecase to load conversations
                // For now, we'll simulate some data
                val conversations = listOf(
                    ConversationItem(
                        id = UUID.randomUUID(),
                        title = "General Questions",
                        lastMessage = "What is the capital of France?",
                        timestamp = System.currentTimeMillis() - 3600000,
                        messageCount = 5
                    ),
                    ConversationItem(
                        id = UUID.randomUUID(),
                        title = "Math Help",
                        lastMessage = "Can you explain quadratic equations?",
                        timestamp = System.currentTimeMillis() - 7200000,
                        messageCount = 3
                    )
                )
                _state.update { 
                    it.copy(
                        conversations = conversations,
                        isLoading = false
                    )
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