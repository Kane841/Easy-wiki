package com.easywiki.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.easywiki.model.AssignmentStatus
import com.easywiki.model.Task
import com.easywiki.model.TaskPriority
import com.easywiki.model.TaskStatus
import com.easywiki.ui.theme.priorityHigh
import com.easywiki.ui.theme.priorityLow
import com.easywiki.ui.theme.priorityMedium
import com.easywiki.ui.theme.priorityNone
import com.easywiki.ui.theme.priorityUrgent
import com.easywiki.ui.common.EmptyState
import com.easywiki.ui.common.ShimmerListSkeleton
import com.easywiki.viewmodel.TaskBoardUiState
import com.easywiki.viewmodel.TaskViewModel

private val TAB_STATUSES = listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE)

private val TAB_LABELS = mapOf(
    TaskStatus.TODO to "待办",
    TaskStatus.IN_PROGRESS to "进行中",
    TaskStatus.DONE to "已完成"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    uiState: TaskBoardUiState,
    taskViewModel: TaskViewModel,
    onRefresh: () -> Unit,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (uiState.allTasks.isEmpty() && !uiState.isLoading) {
            onRefresh()
        }
    }

    val selectedIndex = TAB_STATUSES.indexOf(uiState.selectedTab).coerceAtLeast(0)

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = taskViewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "创建任务")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedIndex) {
                TAB_STATUSES.forEachIndexed { index, status ->
                    val count = taskViewModel.tasksForTab(status).size
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { taskViewModel.selectTab(status) },
                        text = {
                            Text("${TAB_LABELS[status] ?: status.name} ($count)")
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val tasks = taskViewModel.tasksForTab(uiState.selectedTab)
                when {
                    uiState.isLoading && uiState.allTasks.isEmpty() -> {
                        ShimmerListSkeleton(itemCount = 4, lineCount = 3)
                    }
                    tasks.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.TaskAlt,
                            title = "暂无任务",
                            subtitle = "点击右下角 + 创建新任务",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                TaskCard(task = task, onClick = { onTaskClick(task.id) })
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateTaskDialog(
            isLoading = uiState.isLoading,
            onDismiss = taskViewModel::hideCreateDialog,
            onConfirm = taskViewModel::createTask
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Chip
                PriorityChip(priority = task.priority)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                    task.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: assignment status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignment status badge
                if (task.status != TaskStatus.DONE) {
                    val (statusLabel, statusColor) = assignmentStatusInfo(task.assignmentStatus)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                task.assigneeId?.let { id ->
                    Text(
                        text = "#$id",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Colored priority chip used across task screens.
 */
@Composable
fun PriorityChip(
    priority: TaskPriority?,
    modifier: Modifier = Modifier
) {
    val color = priorityColor(priority)
    val label = when (priority) {
        TaskPriority.URGENT -> "紧急"
        TaskPriority.HIGH -> "高"
        TaskPriority.MEDIUM -> "中"
        TaskPriority.LOW -> "低"
        null -> "无"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun priorityColor(priority: TaskPriority?): Color = when (priority) {
    TaskPriority.URGENT -> priorityUrgent
    TaskPriority.HIGH -> priorityHigh
    TaskPriority.MEDIUM -> priorityMedium
    TaskPriority.LOW -> priorityLow
    null -> priorityNone
}

private fun assignmentStatusInfo(status: AssignmentStatus?): Pair<String, Color> = when (status) {
    AssignmentStatus.UNASSIGNED -> "未分配" to Color(0xFF757575)
    AssignmentStatus.PENDING_ACCEPT -> "待接受" to Color(0xFFFB8C00)
    AssignmentStatus.ACCEPTED -> "已接受" to Color(0xFF43A047)
    null -> "未分配" to Color(0xFF757575)
}

@Composable
private fun CreateTaskDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, TaskPriority?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建任务") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        val isSelected = priority == p
                        val color = priorityColor(p)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        1.5.dp, color, RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable { priority = p }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (p) {
                                    TaskPriority.URGENT -> "紧急"
                                    TaskPriority.HIGH -> "高"
                                    TaskPriority.MEDIUM -> "中"
                                    TaskPriority.LOW -> "低"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description.ifBlank { null }, priority) },
                enabled = !isLoading
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}
