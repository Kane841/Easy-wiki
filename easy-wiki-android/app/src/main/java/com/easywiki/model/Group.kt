package com.easywiki.model

data class Group(
    val id: Long,
    val name: String,
    val description: String?,
    val discoverable: Boolean,
    val createdBy: Long?,
    val createdAt: String?
)

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val discoverable: Boolean = false
)

data class GroupInviteResponse(
    val token: String,
    val expiresAt: String?
)
