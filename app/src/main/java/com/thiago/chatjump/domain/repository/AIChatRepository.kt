package com.thiago.chatjump.domain.repository

import com.thiago.chatjump.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AIChatRepository {
    suspend fun getResponse(message: ChatMessage): Flow<String>

    suspend fun getAIResponse(messages: List<ChatMessage>): Flow<String>
}