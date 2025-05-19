package com.thiago.chatjump.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.thiago.chatjump.data.remote.OpenAIDataSource
import com.thiago.chatjump.data.remote.dto.SpeechRequest
import com.thiago.chatjump.domain.repository.TextToSpeechRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager that leverages OpenAI Text-to-Speech (tts-1 / tts-1-hd) API to generate realistic voice
 * from text, downloads the resulting audio (mp3) and plays it with [android.media.MediaPlayer].
 */
@Singleton
class TextToSpeechRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openAIDataSource: OpenAIDataSource
) : TextToSpeechRepository {

    private var scope: CoroutineScope? = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override fun speak(text: String) {
        stop() // stop any previous playback

        val cleanText = clearMarkdown(text)

        if (scope == null) {
            scope = CoroutineScope(Dispatchers.IO + Job())
        }
        scope?.launch {
            try {
                // Reset states
                _isSpeaking.value = false

                val cacheFile = getCacheFileForText(cleanText)

                // Download if not cached
                if (!cacheFile.exists()) {
                    val audioBytes = openAIDataSource.getSpeech(
                        SpeechRequest(
                            model = "tts-1", // or "tts-1-hd" depending on availability
                            input = cleanText,
                            voice = "alloy",
                            response_format = "mp3"
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

    override fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
            Log.e("OpenAITTS", "Failed to stop playback")
        }
        mediaPlayer = null
        _isSpeaking.value = false
    }

    override fun shutdown() {
        stop()
        scope?.cancel()
        scope = null
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

    override fun isTextCached(text: String): Boolean {
        val cleanText = clearMarkdown(text)
        return getCacheFileForText(cleanText).exists()
    }

    private fun clearMarkdown(text: String): String {
        return text.replace(Regex("`.*?`"), "")
            .replace(Regex("\\*\\*|__"), "")
            .replace(Regex("\\*|_"), "")
            .replace(Regex("#+\\s"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .replace(Regex("-\\s"), "")
            .replace(Regex("```.*?```"), "")
            .trim()
    }
}