package com.thiago.chatjump.data.repository

import com.thiago.chatjump.data.remote.ChatCompletionMessage
import com.thiago.chatjump.data.remote.OpenAIClient
import com.thiago.chatjump.domain.model.ChatMessage
import com.thiago.chatjump.domain.repository.AIChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIChatRepositoryImpl @Inject constructor(
    private val openAIClient: OpenAIClient
) : AIChatRepository {
    override suspend fun getResponse(message: ChatMessage): Flow<String> = flow {
        try {
            println("Starting getResponse")
            val response = openAIClient.getChatCompletion(
                messages = listOf(
                    ChatCompletionMessage(
                        role = if (message.isUser) "user" else "assistant",
                        content = message.content
                    )
                )
            )
            emit(response.choices.firstOrNull()?.message?.content ?: "")
        } catch (e: Exception) {
            println("Error in getAIResponse: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun getAIResponse(messages: List<ChatMessage>): Flow<String> = flow {
        try {
            println("Starting AI response collection...")
            openAIClient.getStreamingChatCompletion(
                messages = messages.map { message ->
                    ChatCompletionMessage(
                        role = if (message.isUser) "user" else "assistant",
                        content = message.content
                    )
                }
            ).collect { content ->
                println("Received content: $content")
                // Add a small delay between emissions for smoother streaming
                delay(25)
                emit(content)
            }
        } catch (e: Exception) {
            println("Error in getAIResponse: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}