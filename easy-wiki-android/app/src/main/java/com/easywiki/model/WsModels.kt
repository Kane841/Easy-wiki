package com.easywiki.model

data class WsOutgoingMessage(
    val type: String,
    val payload: Any?
)
