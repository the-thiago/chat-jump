package com.thiago.chatjump.data.repository

import com.thiago.chatjump.data.remote.ChatCompletionMessage
import com.thiago.chatjump.data.remote.OpenAIClient
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.AIChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIChatRepositoryImpl @Inject constructor(
    private val openAIClient: OpenAIClient
) : AIChatRepository {

    override suspend fun getAIResponse(messages: List<ChatMessage>): Flow<String> = flow {
        val response = openAIClient.getChatCompletion(
            messages = messages.map { message ->
                ChatCompletionMessage(
                    role = if (message.isUser) "user" else "assistant",
                    content = message.content
                )
            }
        )
        
        response.choices.firstOrNull()?.message?.content?.let { content ->
            emit(content)
        }
    }
}