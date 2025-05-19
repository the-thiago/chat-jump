package com.thiago.chatjump.data.remote

import android.util.Log
import com.thiago.chatjump.data.remote.dto.ChatCompletionMessage
import com.thiago.chatjump.data.remote.dto.ChatCompletionRequest
import com.thiago.chatjump.data.remote.dto.ChatCompletionResponse
import com.thiago.chatjump.data.remote.dto.ChatMessageDto
import com.thiago.chatjump.data.remote.dto.ChatRequest
import com.thiago.chatjump.data.remote.dto.SpeechRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIDataSourceImpl @Inject constructor(
    private val api: OpenAIApi
) : OpenAIDataSource {

    override suspend fun getChatCompletion(
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

    override suspend fun getStreamingChatCompletion(
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

    override suspend fun getSpeech(request: SpeechRequest): ByteArray {
        val response = api.createSpeechJson(
            request = request
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Speech request failed: ${response.code()}")
        }
        return response.body()?.bytes()
            ?: throw IllegalStateException("Empty speech response body")
    }

    override suspend fun transcribeAudio(audioFile: File): String {
        val mediaType = "audio/mp4".toMediaType()
        val body = audioFile.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", audioFile.name, body)
        val modelBody = "whisper-1".toRequestBody("text/plain".toMediaType())
        val langBody = "en".toRequestBody("text/plain".toMediaType())
        return api.transcribeAudio(
            file = part,
            model = modelBody,
            language = langBody
        ).body()?.text ?: ""
    }

    override suspend fun chatCompletion(conversationHistory: MutableList<ChatMessageDto>): String {
        return api.chatCompletion(
            request = ChatRequest(
                messages = conversationHistory,
            )
        ).body()?.choices?.firstOrNull()?.message?.content ?: ""
    }

    override suspend fun createSpeech(text: String): ResponseBody? {
        val request = SpeechRequest(input = text, model = "tts-1", voice = "alloy", response_format = "mp3")
        return api.createSpeech(request).body()
    }
}