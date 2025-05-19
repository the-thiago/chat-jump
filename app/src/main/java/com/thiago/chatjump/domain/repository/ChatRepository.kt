package com.thiago.chatjump.domain.repository

import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.model.ConversationItem
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllConversations(): Flow<List<ConversationItem>>
    suspend fun createConversation(title: String): Int
    fun getMessagesForConversation(conversationId: Int): Flow<List<ChatMessage>>
    suspend fun saveMessage(conversationId: Int, message: ChatMessage)
    suspend fun deleteMessage(messageId: String)
}