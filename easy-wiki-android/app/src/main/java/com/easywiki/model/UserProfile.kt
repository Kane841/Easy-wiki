package com.easywiki.model

data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val createdAt: String?
)
