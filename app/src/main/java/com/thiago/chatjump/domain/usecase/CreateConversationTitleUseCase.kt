package com.thiago.chatjump.domain.usecase

import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.AIChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateConversationTitleUseCase @Inject constructor(
    private val repository: AIChatRepository
) {
    suspend operator fun invoke(message: ChatMessage): Flow<String> {
        val message = message.copy(content = "Please resume this message to serve as title of the chat: ${message.content}")
        return repository.getResponse(message)
    }
} 