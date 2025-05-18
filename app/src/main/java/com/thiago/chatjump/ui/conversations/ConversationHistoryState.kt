package com.thiago.chatjump.ui.conversations

import com.thiago.chatjump.domain.model.ConversationItem

data class ConversationHistoryState(
    val conversations: List<ConversationItem> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
)