package com.thiago.chatjump.data.repository

import com.thiago.chatjump.data.local.dao.ConversationDao
import com.thiago.chatjump.data.local.dao.MessageDao
import com.thiago.chatjump.data.local.entity.ConversationEntity
import com.thiago.chatjump.data.local.entity.MessageEntity
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    override suspend fun getConversationById(id: Int): ConversationEntity? {
        return conversationDao.getConversationById(id)
    }

    override suspend fun createConversation(title: String): Int {
        val conversation = ConversationEntity(title = title)
        return conversationDao.insertConversation(conversation).toInt()
    }

    override suspend fun updateConversationTitle(id: Int, title: String) {
        conversationDao.getConversationById(id)?.let { conversation ->
            conversationDao.updateConversation(conversation.copy(title = title))
        }
    }

    override fun getMessagesForConversation(conversationId: Int): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { messages ->
            messages.map { message ->
                ChatMessage(
                    id = message.id,
                    content = message.content,
                    isUser = message.isUser,
                    timestamp = message.timestamp
                )
            }
        }
    }

    override suspend fun saveMessage(conversationId: Int, message: ChatMessage) {
        messageDao.insertMessage(
            MessageEntity(
                id = message.id,
                conversationId = conversationId,
                content = message.content,
                isUser = message.isUser,
                timestamp = message.timestamp
            )
        )
    }

    override suspend fun deleteConversation(id: Int) {
        conversationDao.deleteConversation(id)
    }
} 