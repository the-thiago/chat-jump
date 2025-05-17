package com.thiago.chatjump.domain.usecase

import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.AIChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateConversationTitleUseCase @Inject constructor(
    private val repository: AIChatRepository
) {
    suspend operator fun invoke(message: ChatMessage): Flow<String> {
        return repository.getResponse(message)
    }
} 