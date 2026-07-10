package com.easywiki.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easywiki.model.NotificationItem
import com.easywiki.ui.common.EmptyState
import com.easywiki.ui.common.ShimmerListSkeleton
import com.easywiki.util.DeepLinkDestination
import com.easywiki.viewmodel.NotificationUiState

@Composable
fun NotificationScreen(
    uiState: NotificationUiState,
    onLoad: () -> Unit,
    onNotificationClick: (NotificationItem, (DeepLinkDestination) -> Unit) -> Unit,
    onNavigate: (DeepLinkDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (uiState.notifications.isEmpty() && !uiState.isLoading) {
            onLoad()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                ShimmerListSkeleton(itemCount = 6, lineCount = 3)
            }
            uiState.notifications.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.NotificationsOff,
                    title = "暂无通知",
                    subtitle = "当有新消息时，会在这里显示"
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { onNotificationClick(notification, onNavigate) }
                        )
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                notification.createdAt?.let { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        if (!notification.read) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
                containerColor = MaterialTheme.colorScheme.error
            )
        }
    }
}
