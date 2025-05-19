package com.thiago.chatjump.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.chatjump.R
import com.thiago.chatjump.domain.model.ConversationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationHistoryScreen(
    onConversationClick: (Int) -> Unit,
    onNewConversationClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            ConversationTopBar(onBackClick, onNewConversationClick)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConversationSearchBar(state, viewModel::onEvent)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.conversations.isEmpty() && state.searchQuery.isNotBlank() -> {
                        EmptySearchText()
                    }
                    state.conversations.isEmpty() -> {
                        ConversationsEmptyText()
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onClick = { onConversationClick(conversation.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConversationTopBar(onBackClick: () -> Unit, onNewConversationClick: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.conversation_history_screen_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.conversation_history_screen_back_icon_description)
                )
            }
        },
        actions = {
            IconButton(onClick = onNewConversationClick) {
                Icon(
                    Icons.Default.ModeEdit,
                    stringResource(R.string.conversation_history_screen_new_conversation_icon_description)
                )
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConversationSearchBar(
    state: ConversationHistoryState,
    onEvent: (ConversationHistoryEvent) -> Unit,
) {
    SearchBar(
        inputField = {
            TextField(
                value = state.searchQuery,
                onValueChange = { onEvent(ConversationHistoryEvent.OnSearchQueryChange(it)) },
                placeholder = { Text(stringResource(R.string.conversation_history_screen_search_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        stringResource(R.string.conversation_history_screen_search_icon_description)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onEvent(ConversationHistoryEvent.OnSearchQueryChange(""))
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                stringResource(R.string.conversation_history_screen_clear_search_icon_description)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        expanded = state.isSearchActive,
        onExpandedChange = { },
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search suggestions could go here
    }
}

@Composable
private fun BoxScope.EmptySearchText() {
    Text(
        text = stringResource(R.string.conversation_history_screen_no_results_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun BoxScope.ConversationsEmptyText() {
    Text(
        text = stringResource(R.string.conversation_history_screen_empty_state_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.Center),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ConversationItem(
    conversation: ConversationItem,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { Text(conversation.title) },
        supportingContent = { Text(dateFormat.format(Date(conversation.timestamp))) },
        modifier = Modifier.clickable(onClick = onClick)
    )
} 