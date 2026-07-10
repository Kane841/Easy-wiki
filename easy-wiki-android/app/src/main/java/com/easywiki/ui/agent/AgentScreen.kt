package com.easywiki.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.easywiki.model.AgentIntent
import com.easywiki.ui.common.EmptyState
import com.easywiki.ui.wiki.MarkdownContent
import com.easywiki.viewmodel.AgentMessage
import com.easywiki.viewmodel.AgentUiState

@Composable
fun AgentScreen(
    uiState: AgentUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAdoptTasks: (AgentMessage) -> Unit,
    onSnackbarDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onSnackbarDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI 助手",
                    subtitle = "向 Agent 提问，可整理任务、摘要 Wiki 或生成任务建议",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    itemsIndexed(uiState.messages) { _, message ->
                        AgentBubble(
                            message = message,
                            onAdoptTasks = { onAdoptTasks(message) },
                            isAdopting = uiState.isAdopting
                        )
                    }
                    if (uiState.isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("向 Agent 提问…") },
                    maxLines = 4,
                    enabled = !uiState.isLoading
                )
                IconButton(
                    onClick = onSend,
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AgentBubble(
    message: AgentMessage,
    onAdoptTasks: () -> Unit,
    isAdopting: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isUser) {
                Text(text = message.content)
            } else {
                MarkdownContent(
                    markdown = message.content,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!isUser && message.intent == AgentIntent.TASK_SUGGEST && message.taskSuggestions.isNotEmpty()) {
            Button(
                onClick = onAdoptTasks,
                enabled = !isAdopting,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(if (isAdopting) "创建中…" else "采纳 (${message.taskSuggestions.size})")
            }
        }
    }
}
