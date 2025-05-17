package com.thiago.chatjump.data.remote

import com.thiago.chatjump.BuildConfig
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import retrofit2.Response

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

    suspend fun getStreamingChatCompletion(
        messages: List<ChatCompletionMessage>
    ): Flow<String> = flow {
        println("Starting streaming chat completion...")
        try {
            val response = api.createStreamingChatCompletion(
                authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = messages,
                    stream = true
                )
            )
            
            response.body()?.string()?.let { rawResponse ->
                rawResponse.split("\n")
                    .filter { it.startsWith("data: ") }
                    .map { it.substring(6) }
                    .filter { it != "[DONE]" }
                    .forEach { json ->
                        try {
                            val content = json.split("\"content\":\"")[1].split("\"")[0]
                            emit(content)
                        } catch (e: Exception) {
                            println("Error parsing streaming response: ${e.message}")
                        }
                    }
            }
        } catch (e: Exception) {
            println("Error in getStreamingChatCompletion: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST("v1/chat/completions")
    suspend fun createStreamingChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
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