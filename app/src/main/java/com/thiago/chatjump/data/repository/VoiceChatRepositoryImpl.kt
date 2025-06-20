package com.thiago.chatjump.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import com.thiago.chatjump.data.remote.OpenAIDataSource
import com.thiago.chatjump.data.remote.dto.ChatMessageDto
import com.thiago.chatjump.domain.repository.VoiceChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceChatRepositoryImpl @Inject constructor(
    private val service: OpenAIDataSource,
) : VoiceChatRepository {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    // Maintain conversation context during the application lifecycle
    private val conversationHistory = mutableListOf<ChatMessageDto>()

    override fun startRecording(context: Context) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        outputFile = File(dir, "record_${System.currentTimeMillis()}.m4a")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }
    }

    override fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Pipeline: audio file -> transcription -> chat -> speech audio file path
     * Returns Triple<userText, aiText, aiAudioFilePath>
     */
    override suspend fun processUserAudio(context: Context, audioFile: File): Pair<String, String?> {
        return withContext(Dispatchers.IO) {
            // 1. Transcribe user audio (Whisper)
            val transcription = transcribe(audioFile)
            // 2. Chat completion
            val aiText = chat(transcription)
            // 3. Convert AI text to speech
            val audioPath = textToSpeech(context, aiText)
            Pair(transcription, audioPath)
        }
    }

    private suspend fun transcribe(audioFile: File): String {
        val response = service.transcribeAudio(audioFile)
        return response
    }

    private suspend fun chat(userText: String): String {
        // Append the new user message to the running history
        conversationHistory.add(ChatMessageDto("user", userText))

        val aiText = service.chatCompletion(conversationHistory)

        // Append assistant reply to history for future context
        if (aiText.isNotBlank()) {
            conversationHistory.add(ChatMessageDto("assistant", aiText))
        }

        return aiText
    }

    private suspend fun textToSpeech(context: Context, text: String): String? {
        if (text.isBlank()) return null
        val responseBody = service.createSpeech(text) ?: return null
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        val audioFile = File(dir, "ai_${UUID.randomUUID()}.mp3")
        audioFile.outputStream().use { output ->
            responseBody.byteStream().copyTo(output)
        }
        return audioFile.absolutePath
    }

    override fun currentAmplitude(): Int = recorder?.maxAmplitude ?: 0
} 