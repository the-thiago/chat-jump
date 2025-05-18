package com.thiago.chatjump.ui.voicechat

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.repository.VoiceChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Random
import javax.inject.Inject

@HiltViewModel
class VoiceChatViewModel @Inject constructor(
    private val repository: VoiceChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceChatState())
    val uiState: StateFlow<VoiceChatState> = _uiState

    private var loopJob: Job? = null
    private val SILENCE_THRESHOLD = 2000 // amplitude
    private val SILENCE_WINDOW_MS = 1000L
    private val SAMPLE_INTERVAL_MS = 200L
    private val MIN_VOICE_DURATION_MS = 500L // must speak at least this long above threshold

    fun onEvent(event: VoiceChatEvent) {
        when (event) {
            is VoiceChatEvent.StartConversation -> startConversation(event.context)
        }
    }

    private fun startConversation(context: Context) {
        if (loopJob != null) return // already running
        loopJob = viewModelScope.launch {
            while (true) {
                // 1. Listen
                _uiState.value = _uiState.value.copy(isRecording = true, isThinking = false)
                repository.startRecording(context)

                // Wait until user stops speaking using simple VAD
                var silentDuration = 0L
                var voiceDuration = 0L
                var hasSpoken = false
                while (true) {
                    delay(SAMPLE_INTERVAL_MS)
                    val amp = repository.currentAmplitude()
                    val normAmp = (amp / 32767f).coerceIn(0f, 1f)
                    _uiState.value = _uiState.value.copy(userAmplitude = normAmp)
                    if (amp >= SILENCE_THRESHOLD) {
                        hasSpoken = true
                        voiceDuration += SAMPLE_INTERVAL_MS
                        silentDuration = 0L
                    } else {
                        silentDuration += SAMPLE_INTERVAL_MS
                        if (hasSpoken && silentDuration >= SILENCE_WINDOW_MS) {
                            break // user finished speaking
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(isRecording = false, isThinking = true, userAmplitude = 0f)
                val audioFile = repository.stopRecording()

                val spokeEnough = voiceDuration >= MIN_VOICE_DURATION_MS

                if (audioFile != null && spokeEnough) {
                    // 2. Send to OpenAI (transcribe, chat, tts)
                    val (userText, aiAudio) = repository.processUserAudio(context, audioFile)

                    // Skip if transcription is blank (failsafe)
                    if (userText.isNotBlank()) {
                        // 3. Speak AI response
                        aiAudio?.let { playAudio(it) }
                    }
                } else {
                    // Not enough speech; ignore this recording
                    audioFile?.delete()
                }

                _uiState.value = _uiState.value.copy(isThinking = false)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun playAudio(filePath: String) {
        return suspendCancellableCoroutine { cont ->
            try {
                val player = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    start()
                }

                // Mark AI speaking in UI
                _uiState.value = _uiState.value.copy(isAiSpeaking = true)

                // Launch a coroutine to generate fake amplitude values while playing
                val ampJob = viewModelScope.launch {
                    val rnd = Random()
                    while (player.isPlaying) {
                        val amp = rnd.nextFloat() // 0..1
                        _uiState.value = _uiState.value.copy(aiAmplitude = amp)
                        delay(50)
                    }
                }

                player.setOnCompletionListener {
                    it.release()
                    ampJob.cancel()
                    _uiState.value = _uiState.value.copy(isAiSpeaking = false, aiAmplitude = 0f)
                    if (cont.isActive) cont.resume(Unit) {}
                }
                cont.invokeOnCancellation {
                    ampJob.cancel()
                    try { player.stop() } catch (_: Exception) {}
                    player.release()
                    _uiState.value = _uiState.value.copy(isAiSpeaking = false, aiAmplitude = 0f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isAiSpeaking = false, aiAmplitude = 0f)
                if (cont.isActive) cont.resume(Unit) {}
            }
        }
    }
}