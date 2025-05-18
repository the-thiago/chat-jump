package com.thiago.chatjump.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.model.ConversationItem
import com.thiago.chatjump.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        viewModelScope.launch {
            combine(
                chatRepository.getAllConversations(),
                _state.map { it.searchQuery }
            ) { conversations, query ->
                if (query.isBlank()) {
                    conversations
                } else {
                    conversations.filter { conversation ->
                        conversation.title.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredConversations ->
                _state.update { 
                    it.copy(
                        conversations = filteredConversations.map { conversation ->
                            ConversationItem(
                                id = conversation.id,
                                title = conversation.title,
                                lastMessage = "",
                                timestamp = conversation.updatedAt,
                                messageCount = 0
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: ConversationHistoryEvent) {
        when (event) {
            is ConversationHistoryEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
        }
    }
} 