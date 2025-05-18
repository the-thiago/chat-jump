package com.thiago.chatjump.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import java.math.BigInteger

import com.thiago.chatjump.data.remote.OpenAIClient
import com.thiago.chatjump.data.remote.SpeechRequest

/**
 * Manager that leverages OpenAI Text-to-Speech (tts-1 / tts-1-hd) API to generate realistic voice
 * from text, downloads the resulting audio (mp3) and plays it with [MediaPlayer].
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openAIClient: OpenAIClient
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun speak(text: String) {
        stop() // stop any previous playback

        // Clean markdown for TTS
        val cleanText = text.replace(Regex("`.*?`"), "")
            .replace(Regex("\\*\\*|__"), "")
            .replace(Regex("\\*|_"), "")
            .replace(Regex("#+\\s"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .replace(Regex("-\\s"), "")
            .replace(Regex("```.*?```"), "")
            .trim()

        scope.launch {
            try {
                _isSpeaking.value = true
                val cacheFile = getCacheFileForText(cleanText)

                if (!cacheFile.exists()) {
                    val audioBytes = openAIClient.getSpeech(
                        SpeechRequest(
                            model = "tts-1", // or "tts-1-hd" depending on availability
                            input = cleanText,
                            voice = "alloy",
                            format = "mp3"
                        )
                    )
                    cacheFile.writeBytes(audioBytes)
                }

                // Play using MediaPlayer on main thread
                launch(Dispatchers.Main) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(cacheFile.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                _isSpeaking.value = false
                                it.release()
                                mediaPlayer = null
                            }
                            setOnErrorListener { mp, what, extra ->
                                _isSpeaking.value = false
                                mp.release()
                                mediaPlayer = null
                                true
                            }
                            start()
                        }
                        _isSpeaking.value = true
                    } catch (e: Exception) {
                        Log.e("OpenAITTS", "Playback error: ${e.message}")
                        _isSpeaking.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenAITTS", "Failed to fetch speech: ${e.message}")
                _isSpeaking.value = false
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private fun getCacheFileForText(text: String): File {
        val hash = sha256(text)
        return File(context.cacheDir, "openai_tts_$hash.mp3")
    }

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(text.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(64, '0')
    }
} 