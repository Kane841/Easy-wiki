package com.easywiki.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.easywiki.data.ws.WebSocketManager
import com.easywiki.ui.agent.AgentScreen
import com.easywiki.ui.chat.ChatScreen
import com.easywiki.ui.notification.NotificationScreen
import com.easywiki.ui.task.TaskBoardScreen
import com.easywiki.ui.wiki.WikiTreeScreen
import com.easywiki.util.DeepLinkDestination
import com.easywiki.util.WorkspaceTab as NavWorkspaceTab
import com.easywiki.viewmodel.AgentUiState
import com.easywiki.viewmodel.AgentViewModel
import com.easywiki.viewmodel.ChatUiState
import com.easywiki.viewmodel.ChatViewModel
import com.easywiki.viewmodel.NotificationUiState
import com.easywiki.viewmodel.NotificationViewModel
import com.easywiki.viewmodel.TaskBoardUiState
import com.easywiki.viewmodel.TaskViewModel
import com.easywiki.viewmodel.WikiTreeUiState
import com.easywiki.viewmodel.WikiViewModel

enum class WorkspaceTab(val label: String) {
    WIKI("Wiki"),
    TASKS("任务"),
    CHAT("聊天"),
    NOTIFICATIONS("通知"),
    AGENT("Agent")
}

fun WorkspaceTab.toNavTab(): NavWorkspaceTab = when (this) {
    WorkspaceTab.WIKI -> NavWorkspaceTab.WIKI
    WorkspaceTab.TASKS -> NavWorkspaceTab.TASKS
    WorkspaceTab.CHAT -> NavWorkspaceTab.CHAT
    WorkspaceTab.NOTIFICATIONS -> NavWorkspaceTab.NOTIFICATIONS
    WorkspaceTab.AGENT -> NavWorkspaceTab.AGENT
}

fun NavWorkspaceTab.toWorkspaceTab(): WorkspaceTab = when (this) {
    NavWorkspaceTab.WIKI -> WorkspaceTab.WIKI
    NavWorkspaceTab.TASKS -> WorkspaceTab.TASKS
    NavWorkspaceTab.CHAT -> WorkspaceTab.CHAT
    NavWorkspaceTab.NOTIFICATIONS -> WorkspaceTab.NOTIFICATIONS
    NavWorkspaceTab.AGENT -> WorkspaceTab.AGENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    groupId: Long,
    groupName: String?,
    wikiViewModel: WikiViewModel,
    wikiTreeState: WikiTreeUiState,
    taskViewModel: TaskViewModel,
    taskBoardState: TaskBoardUiState,
    chatViewModel: ChatViewModel,
    chatState: ChatUiState,
    currentUserId: Long?,
    notificationViewModel: NotificationViewModel,
    notificationState: NotificationUiState,
    agentViewModel: AgentViewModel,
    agentState: AgentUiState,
    webSocketManager: WebSocketManager,
    initialTab: WorkspaceTab? = null,
    onWikiPageClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onNavigateFromNotification: (DeepLinkDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(initialTab?.ordinal ?: 0)
    }
    val tabs = WorkspaceTab.entries

    DisposableEffect(Unit) {
        webSocketManager.connect()
        onDispose { webSocketManager.disconnect() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(groupName ?: "工作区") })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    WorkspaceTab.WIKI -> Icons.Default.Book
                                    WorkspaceTab.TASKS -> Icons.Default.Task
                                    WorkspaceTab.CHAT -> Icons.AutoMirrored.Filled.Chat
                                    WorkspaceTab.NOTIFICATIONS -> Icons.Default.Notifications
                                    WorkspaceTab.AGENT -> Icons.Default.AutoAwesome
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tabs[selectedTabIndex]) {
                WorkspaceTab.WIKI -> WikiTreeScreen(
                    uiState = wikiTreeState,
                    onRefresh = wikiViewModel::loadTree,
                    onPageClick = onWikiPageClick
                )
                WorkspaceTab.TASKS -> TaskBoardScreen(
                    uiState = taskBoardState,
                    taskViewModel = taskViewModel,
                    onRefresh = taskViewModel::loadTasks,
                    onTaskClick = onTaskClick
                )
                WorkspaceTab.CHAT -> ChatScreen(
                    uiState = chatState,
                    currentUserId = currentUserId,
                    onLoad = chatViewModel::loadMessages,
                    onInputChange = chatViewModel::updateInput,
                    onSend = chatViewModel::sendMessage
                )
                WorkspaceTab.NOTIFICATIONS -> NotificationScreen(
                    uiState = notificationState,
                    onLoad = notificationViewModel::loadNotifications,
                    onNotificationClick = notificationViewModel::markAsReadAndNavigate,
                    onNavigate = onNavigateFromNotification
                )
                WorkspaceTab.AGENT -> AgentScreen(
                    uiState = agentState,
                    onInputChange = agentViewModel::updateInput,
                    onSend = agentViewModel::sendMessage,
                    onAdoptTasks = { message ->
                        agentViewModel.adoptTaskSuggestions(message.taskSuggestions)
                    },
                    onSnackbarDismiss = agentViewModel::clearSnackbar
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title 即将推出")
    }
}
