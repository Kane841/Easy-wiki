package com.easywiki.model

enum class NotificationEventType {
    JOIN_REQUEST,
    JOIN_APPROVED,
    JOIN_REJECTED,
    TASK_ASSIGNED,
    TASK_ACCEPTED,
    TASK_DUE_REMINDER,
    TASK_ASSIGNMENT_TIMEOUT,
    WIKI_UPDATED,
    CHAT_MENTION
}

data class NotificationItem(
    val id: Long,
    val groupId: Long?,
    val type: NotificationEventType,
    val title: String,
    val body: String,
    val data: String?,
    val read: Boolean,
    val createdAt: String?
)

data class NotificationWsPayload(
    val id: Long?,
    val eventType: NotificationEventType?,
    val content: String?,
    val targetUrl: String?,
    val groupId: Long?
)

data class RegisterDeviceRequest(
    val fcmToken: String,
    val platform: String
)
