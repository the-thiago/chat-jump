package com.thiago.chatjump.data.remote

import com.thiago.chatjump.BuildConfig
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIClient @Inject constructor(
    private val api: OpenAIApi
) {
    suspend fun getChatCompletion(
        messages: List<ChatCompletionMessage>
    ): ChatCompletionResponse {
        return api.createChatCompletion(
            authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
            request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = messages,
                stream = false
            )
        )
    }
}

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val stream: Boolean
)

data class ChatCompletionMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatCompletionMessage
) 