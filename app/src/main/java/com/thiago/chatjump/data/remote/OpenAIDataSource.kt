package com.thiago.chatjump.data.remote

import android.util.Log
import com.thiago.chatjump.data.remote.dto.ChatCompletionMessage
import com.thiago.chatjump.data.remote.dto.ChatCompletionRequest
import com.thiago.chatjump.data.remote.dto.ChatCompletionResponse
import com.thiago.chatjump.data.remote.dto.ChatRequest
import com.thiago.chatjump.data.remote.dto.ChatResponse
import com.thiago.chatjump.data.remote.dto.SpeechRequest
import com.thiago.chatjump.data.remote.dto.TranscriptionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIDataSource @Inject constructor(
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
        Log.i("OpenAIClient", "Starting streaming chat completion...")
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
                            Log.e("OpenAIClient", "Error parsing streaming response: ${e.message}")
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("OpenAIClient", "Error in getStreamingChatCompletion: ${e.message}")
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