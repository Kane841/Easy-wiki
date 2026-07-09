package com.easywiki.model

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class AssignmentStatus {
    UNASSIGNED,
    PENDING_ACCEPT,
    ACCEPTED
}

data class Task(
    val id: Long,
    val groupId: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority?,
    val assigneeId: Long?,
    val assignmentStatus: AssignmentStatus?,
    val creatorId: Long?,
    val dueDate: String?,
    val createdAt: String?
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority? = null,
    val assigneeId: Long? = null,
    val dueDate: String? = null
)

data class UpdateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority? = null,
    val status: TaskStatus? = null,
    val dueDate: String? = null
)

data class AssignTaskRequest(
    val assigneeId: Long
)
