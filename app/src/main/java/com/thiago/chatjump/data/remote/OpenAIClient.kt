package com.thiago.chatjump.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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
                            val rawContent = json.split("\"content\":\"")[1].split("\"")[0]
                            // Replace escaped newline sequences with real newline characters so MarkdownText can render correctly
                            val content = rawContent
                                .replace("\\n", "\n")
                                .replace("\\r", "\r")
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

    suspend fun getSpeech(request: SpeechRequest): ByteArray {
        val response = api.createSpeechJson(
            request = request
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Speech request failed: ${response.code()}")
        }
        return response.body()?.bytes()
            ?: throw IllegalStateException("Empty speech response body")
    }

    suspend fun transcribeAudio(
        part: MultipartBody.Part,
        modelBody: RequestBody,
        langBody: RequestBody
    ): Response<TranscriptionResponse> {
        return api.transcribeAudio(
            file = part,
            model = modelBody,
            language = langBody
        )
    }

    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse> {
        return api.chatCompletion(request)
    }

    suspend fun createSpeech(@Body request: SpeechRequest): Response<ResponseBody> {
        return api.createSpeech(request)
    }
}

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST("v1/chat/completions")
    suspend fun createStreamingChatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>

    // Speech synthesis
    @POST("v1/audio/speech")
    @Headers("Content-Type: application/json")
    suspend fun createSpeechJson(
        @Body request: SpeechRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody
    ): Response<TranscriptionResponse>

    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse>

    @POST("v1/audio/speech")
    suspend fun createSpeech(@Body request: SpeechRequest): Response<ResponseBody>
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

// ---------- OpenAI Speech ---------

data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String = "alloy",
    val response_format: String = "mp3"
)

data class TranscriptionResponse(
    val text: String
)

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessageDto
)