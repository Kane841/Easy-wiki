package com.easywiki.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.easywiki.viewmodel.TaskDetailUiState
import com.easywiki.viewmodel.canAccept
import com.easywiki.viewmodel.canAssign
import com.easywiki.viewmodel.canClaim
import com.easywiki.viewmodel.canReject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    uiState: TaskDetailUiState,
    onLoad: (Long) -> Unit,
    onAssigneeIdChange: (String) -> Unit,
    onAssign: (Long) -> Unit,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onClaim: (Long) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskId) {
        onLoad(taskId)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onSnackbarDismiss()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.task?.title ?: "任务详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            uiState.task != null -> {
                val task = uiState.task
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(priorityColor(task.priority))
                        )
                        Text(
                            text = "优先级: ${task.priority?.name ?: "未设置"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "状态: ${task.status.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "分配状态: ${task.assignmentStatus?.name ?: "未分配"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    task.assigneeId?.let { assigneeId ->
                        Text(text = "负责人 ID: $assigneeId", style = MaterialTheme.typography.bodyMedium)
                    }

                    task.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "描述", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (task.canAssign()) {
                        OutlinedTextField(
                            value = uiState.assigneeIdInput,
                            onValueChange = onAssigneeIdChange,
                            label = { Text("负责人用户 ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onAssign(taskId) },
                            enabled = !uiState.isActionLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("分配任务")
                        }
                    }

                    if (task.canClaim()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onClaim(taskId) },
                            enabled = !uiState.isActionLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("认领任务")
                        }
                    }

                    if (task.canAccept()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onAccept(taskId) },
                            enabled = !uiState.isActionLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("接受任务")
                        }
                    }

                    if (task.canReject()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onReject(taskId) },
                            enabled = !uiState.isActionLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("拒绝任务")
                        }
                    }

                    uiState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
