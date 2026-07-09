package com.easywiki.model

enum class AgentIntent {
    TASK_ORGANIZE,
    WIKI_SUMMARY,
    TASK_SUGGEST,
    GENERAL
}

data class AgentChatTurn(
    val role: String,
    val content: String
)

data class AgentChatRequest(
    val message: String,
    val history: List<AgentChatTurn> = emptyList()
)

data class AgentChatResponse(
    val reply: String,
    val intent: AgentIntent
)

data class AgentTaskSuggestion(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority? = null
)

data class AgentTaskCreateRequest(
    val tasks: List<AgentTaskSuggestion>
)

data class AgentTasksJson(
    val tasks: List<AgentTaskSuggestion> = emptyList()
)
