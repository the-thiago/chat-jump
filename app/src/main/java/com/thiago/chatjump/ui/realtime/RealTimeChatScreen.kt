package com.thiago.chatjump.ui.realtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.chatjump.ui.chat.components.MessageBubble
import com.thiago.chatjump.ui.realtime.components.AudioWaveform
import com.thiago.chatjump.ui.realtime.components.OrbitingLines

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeChatScreen(
    modifier: Modifier = Modifier,
    onConversationHistoryClick: () -> Unit,
    viewModel: RealTimeChatViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.scrollToBottom) {
        if (state.scrollToBottom) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            viewModel.onEvent(RealTimeChatEvent.OnScrollToBottom)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onConversationHistoryClick) {
                        Icon(Icons.Default.List, "Conversation History")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(state.messages) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = { /* Handled by clipboard manager */ },
                            onPlay = { /* Handled by TTS */ }
                        )
                    }
                }

                // Waveform visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    when {
                        state.isSpeaking -> {
                            AudioWaveform(
                                waveform = state.audioWaveform,
                                isSpeaking = true
                            )
                        }
                        state.isProcessing -> {
                            OrbitingLines(isProcessing = true)
                        }
                        else -> {
                            OrbitingLines()
                        }
                    }
                }

                // Input area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = { viewModel.onEvent(RealTimeChatEvent.OnInputTextChange(it)) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your message...") },
                            maxLines = 10
                        )
                        
                        Spacer(modifier = Modifier.padding(8.dp))
                        
                        // Mic button for voice input
                        IconButton(
                            onClick = {
                                if (state.isListening) {
                                    viewModel.onEvent(RealTimeChatEvent.OnStopListening)
                                } else {
                                    viewModel.onEvent(RealTimeChatEvent.OnStartListening)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (state.isListening) "Stop listening" else "Start listening",
                                tint = if (state.isListening) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Send button
                        IconButton(
                            onClick = { viewModel.onEvent(RealTimeChatEvent.OnSendMessage(state.inputText)) },
                            enabled = state.inputText.isNotBlank() && !state.isProcessing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (state.inputText.isNotBlank() && !state.isProcessing)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
} 