package com.thiago.chatjump.domain.repository

import java.io.File

interface VoiceChatRepository {
    fun startRecording(context: android.content.Context)
    fun stopRecording(): File?
    suspend fun processUserAudio(context: android.content.Context, audioFile: File): Pair<String, String?>
    fun currentAmplitude(): Int
}