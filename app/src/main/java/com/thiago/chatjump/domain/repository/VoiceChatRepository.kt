package com.thiago.chatjump.domain.repository

interface VoiceChatRepository {
    fun startRecording(context: android.content.Context)
    fun stopRecording(): java.io.File?
    suspend fun processUserAudio(context: android.content.Context, audioFile: java.io.File): Pair<String, String?>
    fun currentAmplitude(): Int
}