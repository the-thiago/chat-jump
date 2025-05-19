package com.thiago.chatjump.data.remote

import com.thiago.chatjump.data.remote.dto.ChatCompletionMessage
import com.thiago.chatjump.data.remote.dto.ChatCompletionResponse
import com.thiago.chatjump.data.remote.dto.ChatMessageDto
import com.thiago.chatjump.data.remote.dto.SpeechRequest
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import java.io.File

interface OpenAIDataSource {

    suspend fun getChatCompletion(
        messages: List<ChatCompletionMessage>
    ): ChatCompletionResponse

    suspend fun getStreamingChatCompletion(
        messages: List<ChatCompletionMessage>
    ): Flow<String>

    suspend fun getSpeech(request: SpeechRequest): ByteArray

    suspend fun transcribeAudio(audioFile: File): String

    suspend fun chatCompletion(conversationHistory: MutableList<ChatMessageDto>): String

    suspend fun createSpeech(text: String): ResponseBody?
}
