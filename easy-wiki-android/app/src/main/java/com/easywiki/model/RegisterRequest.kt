package com.easywiki.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)
