package com.thiago.chatjump.ui.conversations

sealed interface ConversationHistoryEvent {
    data class OnSearchQueryChange(val query: String) : ConversationHistoryEvent
    object OnNewConversationClick : ConversationHistoryEvent
    object OnSearchActiveChange : ConversationHistoryEvent
    object OnDismissError : ConversationHistoryEvent
    object OnRefresh : ConversationHistoryEvent
} 