package com.thiago.chatjump.ui.conversations

sealed interface ConversationHistoryEvent {
    data class OnSearchQueryChange(val query: String) : ConversationHistoryEvent
} 