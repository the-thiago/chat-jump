package com.thiago.chatjump.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.thiago.chatjump.R
import com.thiago.chatjump.ui.chat.components.MessageBubble
import com.thiago.chatjump.ui.chat.components.StreamingMessageBubble
import com.thiago.chatjump.ui.chat.components.ThinkingBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onConversationHistoryClick: () -> Unit,
    onRealTimeClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    conversationId: Int
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Load conversation if ID is provided
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    val coroutineScope = rememberCoroutineScope()
    ObserveAsEvents(
        flow = viewModel.events,
    ) { event ->
        coroutineScope.launch {
            if (event is ChatUiEvent.ScrollToBottom) {
                val index = listState.layoutInfo.totalItemsCount - 1
                if (index > -1) {
                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onConversationHistoryClick) {
                        Icon(Icons.AutoMirrored.Filled.List, "Conversation History")
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
                            onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                            onPlay = { viewModel.onEvent(ChatEvent.OnPlayResponse(message.content, message.id)) },
                            isSpeaking = state.speakingMessageId == message.id && state.isSpeaking
                        )
                    }

                    // Show thinking bubble when waiting for response
                    if (state.isThinking) {
                        this.item {
                            ThinkingBubble()
                        }
                    }

                    // Show streaming message
                    if (state.currentStreamingMessage.isNotEmpty()) {
                        this.item {
                            StreamingMessageBubble(
                                text = state.currentStreamingMessage,
                                modifier = Modifier.animateContentSize()
                            )
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
                            onValueChange = { viewModel.onEvent(ChatEvent.OnInputTextChange(it)) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your message...") },
                            maxLines = 10
                        )
                        
                        Spacer(modifier = Modifier.padding(8.dp))
                        
                        // Send button
                        IconButton(
                            onClick = { viewModel.onEvent(ChatEvent.OnSendMessage(state.inputText)) },
                            enabled = state.inputText.isNotBlank() && !state.isThinking
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (state.inputText.isNotBlank() && !state.isThinking)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Wave button
                        IconButton(
                            onClick = onRealTimeClick
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sound_wave),
                                contentDescription = "Real Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}