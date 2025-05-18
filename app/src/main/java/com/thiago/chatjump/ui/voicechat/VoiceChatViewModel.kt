package com.thiago.chatjump.ui.voicechat

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.chatjump.domain.repository.VoiceChatRepository
import com.thiago.chatjump.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    private val eventChannel = Channel<VoiceChatUiEvent>()
    val events = eventChannel.receiveAsFlow()

    private var loopJob: Job? = null
    private val silenceThreshold = 2000 // amplitude
    private val silenceWindowMs = 1000L
    private val sampleIntervalMs = 200L
    private val minVoiceDurationMs = 500L // must speak at least this long above threshold

    fun onEvent(event: VoiceChatEvent) {
        when (event) {
            is VoiceChatEvent.StartConversation -> startConversation(event.context)
        }
    }

    private fun startConversation(context: Context) {
        if (loopJob != null) return // already running
        loopJob = viewModelScope.launch {
            while (true) {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    if (!_uiState.value.isOffline) {
                        _uiState.update { it.copy(isOffline = true) }
                    }
                    // Wait until connection is restored
                    while (!NetworkUtil.isNetworkAvailable(context)) {
                        delay(1000)
                    }
                    // Connection restored
                    _uiState.update { it.copy(isOffline = false) }
                    eventChannel.send(VoiceChatUiEvent.BackOnline)
                }

                // 1. Listen
                _uiState.value = _uiState.value.copy(isRecording = true, isThinking = false)
                repository.startRecording(context)

                var silentDuration = 0L
                var voiceDuration = 0L
                var hasSpoken = false
                var interruptedByOffline = false

                while (true) {
                    delay(sampleIntervalMs)

                    // If we go offline during recording, stop and discard
                    if (!NetworkUtil.isNetworkAvailable(context)) {
                        _uiState.update { it.copy(isOffline = true) }
                        interruptedByOffline = true
                        break
                    }

                    val amp = repository.currentAmplitude()
                    val normAmp = (amp / 32767f).coerceIn(0f, 1f)
                    _uiState.value = _uiState.value.copy(userAmplitude = normAmp)
                    if (amp >= silenceThreshold) {
                        hasSpoken = true
                        voiceDuration += sampleIntervalMs
                        silentDuration = 0L
                    } else {
                        silentDuration += sampleIntervalMs
                        if (hasSpoken && silentDuration >= silenceWindowMs) {
                            break // user finished speaking
                        }
                    }
                }

                // Stop recording (or discard if offline)
                _uiState.value = _uiState.value.copy(isRecording = false, userAmplitude = 0f)
                val audioFile = repository.stopRecording()

                // If we lost internet, discard recording and restart loop which will handle offline flow
                if (interruptedByOffline || !NetworkUtil.isNetworkAvailable(context)) {
                    audioFile?.delete()
                    continue // restart while(true)
                }

                _uiState.value = _uiState.value.copy(isThinking = true)

                val spokeEnough = voiceDuration >= minVoiceDurationMs

                if (audioFile != null && spokeEnough) {
                    try {
                        val (userText, aiAudio) = repository.processUserAudio(context, audioFile)

                        // Skip if transcription is blank (failsafe)
                        if (userText.isNotBlank()) {
                            aiAudio?.let { playAudio(it) }
                        }
                    } catch (e: Exception) {
                        Log.e("VoiceChatViewModel", "Error processing audio: ", e)
                        eventChannel.send(VoiceChatUiEvent.UnexpectedError)
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