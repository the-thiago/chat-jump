package com.thiago.chatjump.ui.conversations

import java.util.UUID

sealed interface ConversationHistoryEvent {
    data class OnSearchQueryChange(val query: String) : ConversationHistoryEvent
    data class OnConversationClick(val id: UUID) : ConversationHistoryEvent
    object OnNewConversationClick : ConversationHistoryEvent
    object OnSearchActiveChange : ConversationHistoryEvent
    object OnDismissError : ConversationHistoryEvent
    object OnRefresh : ConversationHistoryEvent
} 