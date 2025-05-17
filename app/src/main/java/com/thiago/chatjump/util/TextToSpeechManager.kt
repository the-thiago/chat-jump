package com.thiago.chatjump.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initializeTextToSpeech()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Language not supported")
                }
            } else {
                Log.e("TextToSpeech", "Initialization failed")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                Log.e("TextToSpeech", "Error speaking text")
            }
        })
    }

    fun speak(text: String) {
        textToSpeech?.let { tts ->
            if (tts.isSpeaking) {
                tts.stop()
            }
            
            // Remove markdown formatting for better speech
            val cleanText = text.replace(Regex("`.*?`"), "")
                .replace(Regex("\\*.*?\\*"), "")
                .replace(Regex("\\*\\*.*?\\*\\*"), "")
                .replace(Regex("#+\\s"), "")
                .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
                .replace(Regex("-\\s"), "")
                .replace(Regex("```.*?```"), "")
                .trim()

            tts.speak(
                cleanText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "message_${System.currentTimeMillis()}"
            )
        }
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
    }
} 