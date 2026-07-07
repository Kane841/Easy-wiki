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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.easywiki.ui.agent.AgentPlaceholderScreen
import com.easywiki.ui.chat.ChatPlaceholderScreen
import com.easywiki.ui.notification.NotificationPlaceholderScreen
import com.easywiki.ui.task.TaskBoardScreen
import com.easywiki.ui.wiki.WikiTreeScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    groupId: Long,
    groupName: String?,
    wikiViewModel: WikiViewModel,
    wikiTreeState: WikiTreeUiState,
    taskViewModel: TaskViewModel,
    taskBoardState: TaskBoardUiState,
    onWikiPageClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = WorkspaceTab.entries

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
                WorkspaceTab.CHAT -> ChatPlaceholderScreen()
                WorkspaceTab.NOTIFICATIONS -> NotificationPlaceholderScreen()
                WorkspaceTab.AGENT -> AgentPlaceholderScreen()
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
