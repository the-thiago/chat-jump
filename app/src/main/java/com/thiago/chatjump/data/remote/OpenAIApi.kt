package com.thiago.chatjump.data.remote

import com.thiago.chatjump.data.remote.dto.ChatCompletionRequest
import com.thiago.chatjump.data.remote.dto.ChatCompletionResponse
import com.thiago.chatjump.data.remote.dto.ChatRequest
import com.thiago.chatjump.data.remote.dto.ChatResponse
import com.thiago.chatjump.data.remote.dto.SpeechRequest
import com.thiago.chatjump.data.remote.dto.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OpenAIApi {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST("v1/chat/completions")
    suspend fun createStreamingChatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>

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