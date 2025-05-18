package com.thiago.chatjump.domain.repository

import com.thiago.chatjump.data.local.entity.ConversationEntity
import com.thiago.chatjump.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllConversations(): Flow<List<ConversationEntity>>
    suspend fun getConversationById(id: Int): ConversationEntity?
    suspend fun createConversation(title: String): Int
    suspend fun updateConversationTitle(id: Int, title: String)
    fun getMessagesForConversation(conversationId: Int): Flow<List<ChatMessage>>
    suspend fun saveMessage(conversationId: Int, message: ChatMessage)
    suspend fun deleteConversation(id: Int)
}