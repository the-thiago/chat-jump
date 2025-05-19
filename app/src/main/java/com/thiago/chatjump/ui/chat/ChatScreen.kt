package com.thiago.chatjump.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.chatjump.R
import com.thiago.chatjump.ui.chat.components.MessageBubble
import com.thiago.chatjump.ui.chat.components.StreamingMessageBubble
import com.thiago.chatjump.ui.chat.components.ThinkingBubble
import com.thiago.chatjump.util.ObserveAsEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                if (state.messages.isNotEmpty()) {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            listState.scrollToItem(0)
        }
    }

    DisposableEffect(Unit) { // Stop any playing TTS when this screen leaves composition
        onDispose {
            viewModel.onEvent(ChatEvent.OnPlayResponse(null, null))
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onConversationHistoryClick) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.chat_screen_conversation_history_icon_description))
                    }
                }
            )
        },
        bottomBar = {
            // Input area relocated to bottomBar so Snackbar appears above it
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
                        placeholder = { Text(stringResource(R.string.chat_screen_input_placeholder)) },
                        maxLines = 10,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    val enabled = state.inputText.isNotBlank() && !state.isThinking && state.currentStreamingMessage.isEmpty()
                    IconButton(
                        onClick = { viewModel.onEvent(ChatEvent.OnSendMessage(state.inputText)) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_screen_send_icon_description),
                            tint = if (enabled)
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
                            contentDescription = stringResource(R.string.chat_screen_real_time_icon_description),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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
                if (state.messages.isEmpty() && !state.isThinking && state.currentStreamingMessage.isEmpty() && state.error == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.chat_screen_empty_state_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                // Messages list
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Error message & retry button (will appear at the very bottom because of reverseLayout)
                    if (state.error != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = state.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (state.canRetry) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        viewModel.onEvent(ChatEvent.OnRetry)
                                    }) {
                                        Text(stringResource(R.string.chat_screen_retry_button))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.onEvent(ChatEvent.OnDismissError) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.chat_screen_dismiss_error_icon_description),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Extra placeholders (thinking/streaming) should be at the bottom when present
                    if (state.isThinking) {
                        item {
                            ThinkingBubble()
                        }
                    }

                    if (state.currentStreamingMessage.isNotEmpty()) {
                        item {
                            StreamingMessageBubble(
                                text = state.currentStreamingMessage
                            )
                        }
                    }

                    items(state.messages.asReversed()) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                            onPlay = { viewModel.onEvent(ChatEvent.OnPlayResponse(message.content, message.id)) },
                            isSpeaking = state.speakingMessageId == message.id,
                            isLoading = state.loadingSpeechMessageId == message.id,
                        )
                    }
                }
            }
        }
    }

    // Automatically dismiss error when user starts typing again
    LaunchedEffect(state.inputText) {
        if (state.inputText.isNotBlank() && state.error != null && !state.canRetry) {
            viewModel.onEvent(ChatEvent.OnDismissError)
        }
    }
}