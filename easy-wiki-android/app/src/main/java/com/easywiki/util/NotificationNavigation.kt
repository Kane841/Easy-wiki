package com.easywiki.util

import com.easywiki.model.NotificationEventType
import com.easywiki.model.NotificationItem

object NotificationNavigation {

    fun resolveDestination(notification: NotificationItem, targetUrl: String? = null): DeepLinkDestination? {
        DeepLinkParser.parse(targetUrl)?.let { return it }
        notification.data?.let { data ->
            DeepLinkParser.parse(data)?.let { return it }
            try {
                val json = org.json.JSONObject(data)
                DeepLinkParser.parse(json.optString("targetUrl"))?.let { return it }
            } catch (_: Exception) {
                // fall through
            }
        }
        val groupId = notification.groupId ?: return null
        return when (notification.type) {
            NotificationEventType.CHAT_MENTION -> DeepLinkDestination.Chat(groupId)
            NotificationEventType.WIKI_UPDATED -> DeepLinkDestination.Workspace(groupId, WorkspaceTab.WIKI)
            NotificationEventType.TASK_ASSIGNED,
            NotificationEventType.TASK_ACCEPTED,
            NotificationEventType.TASK_DUE_REMINDER,
            NotificationEventType.TASK_ASSIGNMENT_TIMEOUT -> DeepLinkDestination.Workspace(groupId, WorkspaceTab.TASKS)
            else -> DeepLinkDestination.Workspace(groupId)
        }
    }
}

enum class WorkspaceTab {
    WIKI,
    TASKS,
    CHAT,
    NOTIFICATIONS,
    AGENT
}
