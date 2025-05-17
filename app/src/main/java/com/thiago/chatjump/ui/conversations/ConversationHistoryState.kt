package com.thiago.chatjump.ui.conversations

data class ConversationHistoryState(
    val conversations: List<ConversationItem> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ConversationItem(
    val id: Int,
    val title: String,
    val lastMessage: String,
    val timestamp: Long,
    val messageCount: Int
) 