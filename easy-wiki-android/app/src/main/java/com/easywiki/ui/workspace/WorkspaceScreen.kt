package com.easywiki.ui.workspace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.easywiki.viewmodel.GroupUiState
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
    groupUiState: GroupUiState,
    onShowInviteDialog: () -> Unit,
    onHideInviteDialog: () -> Unit,
    onGenerateInviteToken: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    initialTab: WorkspaceTab? = null,
    onWikiPageClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onNavigateFromNotification: (DeepLinkDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(initialTab?.ordinal ?: 0)
    }

    LaunchedEffect(initialTab) {
        initialTab?.let { selectedTabIndex = it.ordinal }
    }

    val tabs = WorkspaceTab.entries
    val unreadCount = notificationState.notifications.count { !it.read }

    DisposableEffect(Unit) {
        webSocketManager.connect()
        onDispose { webSocketManager.disconnect() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(groupName ?: "工作区") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShowInviteDialog) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = "邀请成员"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == WorkspaceTab.NOTIFICATIONS && unreadCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                                color = MaterialTheme.colorScheme.onError,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
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
                            }
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
                    onPageClick = onWikiPageClick,
                    onCreatePage = wikiViewModel::createPage,
                    onShowCreateDialog = wikiViewModel::showCreateDialog,
                    onHideCreateDialog = wikiViewModel::hideCreateDialog
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

    if (groupUiState.showInviteDialog) {
        InviteDialog(
            token = groupUiState.inviteToken,
            expiresAt = groupUiState.inviteExpiresAt,
            isLoading = groupUiState.isGeneratingInvite,
            errorMessage = groupUiState.inviteError,
            onGenerateToken = { onGenerateInviteToken(groupId) },
            onDismiss = onHideInviteDialog
        )
    }
}

@Composable
private fun InviteDialog(
    token: String?,
    expiresAt: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onGenerateToken: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请成员") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                    Text("正在生成邀请令牌…")
                } else if (token != null) {
                    Text("将此令牌分享给他人，即可加入群组：")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = token,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    expiresAt?.let {
                        Text(
                            text = "有效期至：$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("邀请令牌", token))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("复制令牌")
                    }
                } else {
                    Text("点击下方按钮生成邀请令牌（有效期 7 天）")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onGenerateToken,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("生成令牌")
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
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
