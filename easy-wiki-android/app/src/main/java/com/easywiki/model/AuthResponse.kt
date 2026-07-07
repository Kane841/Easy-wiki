package com.easywiki.model

data class AuthResponse(
    val token: String,
    val userId: Long,
    val username: String
)
