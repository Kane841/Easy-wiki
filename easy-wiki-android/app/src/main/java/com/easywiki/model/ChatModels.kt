package com.easywiki.model

data class ChatMessage(
    val id: Long,
    val groupId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val content: String,
    val mentions: List<Long>?,
    val sentAt: String?
)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int
)

data class ChatSendPayload(
    val groupId: Long,
    val content: String
)
