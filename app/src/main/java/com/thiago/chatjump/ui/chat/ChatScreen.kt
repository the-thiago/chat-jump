package com.thiago.chatjump.ui.chat

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.chatjump.ui.chat.components.MessageBubble
import com.thiago.chatjump.ui.chat.components.ThinkingBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onConversationHistoryClick: () -> Unit,
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
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.scrollToBottom) {
        if (state.scrollToBottom) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            viewModel.onEvent(ChatEvent.OnScrollToBottom)
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
                            onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                            onPlay = { viewModel.onEvent(ChatEvent.OnPlayResponse(message.content)) }
                        )
                    }

                    // Show thinking bubble when waiting for response
                    if (state.isThinking) {
                        item {
                            ThinkingBubble()
                        }
                    }

                    // Show streaming message
                    if (state.currentStreamingMessage.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = 16.dp,
                                                bottomEnd = 16.dp
                                            )
                                        )
                                        .background(
                                             MaterialTheme.colorScheme.primary
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = state.currentStreamingMessage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
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
                    }
                }
            }
        }
    }
} 