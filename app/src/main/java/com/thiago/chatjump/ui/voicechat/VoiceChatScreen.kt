package com.thiago.chatjump.ui.voicechat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.chatjump.ui.voicechat.components.WaveformVisualizer
import com.thiago.chatjump.ui.voicechat.components.YarnBallVisualizer
import com.thiago.chatjump.util.ObserveAsEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(viewModel: VoiceChatViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val coroutineScope = rememberCoroutineScope()
    ObserveAsEvents(
        flow = viewModel.events,
    ) { event ->
        coroutineScope.launch {
            when (event) {
                VoiceChatUiEvent.BackOnline -> snackbarHostState.showSnackbar("Back online, try again!")
                VoiceChatUiEvent.UnexpectedError -> snackbarHostState.showSnackbar("Unexpected error, try speaking again!")
            }
        }
    }

    LaunchedEffect(uiState.isOffline) {
        if (uiState.isOffline) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "No internet connection",
                    duration = SnackbarDuration.Indefinite
                )
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Permission state holders
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showRationale by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    // Launcher to request the permission
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.onEvent(VoiceChatEvent.StartConversation(context))
        } else {
            // Determine whether to show rationale or treat as permanently denied
            if (activity != null) {
                showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
                permanentlyDenied = !showRationale
            }
        }
    }

    // Start conversation automatically if permission already granted (e.g., after configuration change)
    LaunchedEffect(hasMicPermission) {
        if (hasMicPermission) {
            viewModel.onEvent(VoiceChatEvent.StartConversation(context))
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // UI
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                showRationale -> {
                    PermissionRationaleDialog(
                        onDismiss = { showRationale = false },
                        onConfirm = {
                            showRationale = false
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
                permanentlyDenied -> {
                    PermissionPermanentlyDeniedScreen(onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    })
                }
                else -> {
                    VisualizerScreen(uiState)
                }
            }
        }
    }
}

@Composable
private fun VisualizerScreen(uiState: VoiceChatState) {
    // Smooth transition factor: 0f (user/yarn) -> 1f (AI/wave)
    val progress by animateFloatAsState(
        targetValue = if (uiState.isAiSpeaking) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "visualTransition"
    )

    // Phase-split helpers so the yarn morph happens first, then we cross-fade
    val morphProgress = (progress * 2f).coerceIn(0f, 1f)             // 0->1 during first half
    val secondHalfFactor = ((progress - 0.5f) * 2f).coerceIn(0f, 1f) // 0 during first half, 0->1 during second half

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Yarn Ball first morphs to a flat line (morphProgress) and only then fades out
        YarnBallVisualizer(
            isRecording = uiState.isRecording,
            amplitude = uiState.userAmplitude,
            morphToLineProgress = morphProgress,
            modifier = Modifier.graphicsLayer {
                // Visible for the whole first half, then fades & scales down
                alpha = 1f - secondHalfFactor
                scaleX = 1f - 0.2f * secondHalfFactor
                scaleY = 1f - 0.2f * secondHalfFactor
            }
        )

        // Waveform stays hidden until the yarn is flat, then fades/zooms in
        WaveformVisualizer(
            amplitude = uiState.aiAmplitude,
            modifier = Modifier.graphicsLayer {
                alpha = secondHalfFactor
                scaleX = 0.8f + 0.2f * secondHalfFactor
                scaleY = 0.8f + 0.2f * secondHalfFactor
            }
        )

        // Display thin progress bar for thinking state
        if (uiState.isThinking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PermissionRationaleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Microphone Permission Required") },
        text = {
            Text("RealChat requires access to your microphone so you can speak to the AI assistant. Without this permission, the app won't be able to hear you.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionPermanentlyDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Microphone permission has been permanently denied. Please enable it in app settings to continue.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
} 