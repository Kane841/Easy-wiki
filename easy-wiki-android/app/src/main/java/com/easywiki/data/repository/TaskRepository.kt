package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.AssignTaskRequest
import com.easywiki.model.CreateTaskRequest
import com.easywiki.model.Task
import com.easywiki.model.TaskPriority
import com.easywiki.model.TaskStatus
import com.easywiki.model.UpdateTaskRequest

class TaskRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun listTasks(groupId: Long, status: TaskStatus? = null): Result<List<Task>> = runCatching {
        val response = api().listTasks(groupId, status)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载任务失败" })
        }
        response.data
    }

    suspend fun getTask(groupId: Long, taskId: Long): Result<Task> = runCatching {
        val response = api().getTask(groupId, taskId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载任务详情失败" })
        }
        response.data
    }

    suspend fun createTask(
        groupId: Long,
        title: String,
        description: String?,
        priority: TaskPriority?
    ): Result<Task> = runCatching {
        val response = api().createTask(
            groupId,
            CreateTaskRequest(title = title, description = description, priority = priority)
        )
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "创建任务失败" })
        }
        response.data
    }

    suspend fun assignTask(groupId: Long, taskId: Long, assigneeId: Long): Result<Task> = runCatching {
        val response = api().assignTask(groupId, taskId, AssignTaskRequest(assigneeId))
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "分配任务失败" })
        }
        response.data
    }

    suspend fun acceptTask(groupId: Long, taskId: Long): Result<Task> = runCatching {
        val response = api().acceptTask(groupId, taskId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "接受任务失败" })
        }
        response.data
    }

    suspend fun rejectTask(groupId: Long, taskId: Long): Result<Task> = runCatching {
        val response = api().rejectTask(groupId, taskId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "拒绝任务失败" })
        }
        response.data
    }

    suspend fun claimTask(groupId: Long, taskId: Long): Result<Task> = runCatching {
        val response = api().claimTask(groupId, taskId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "认领任务失败" })
        }
        response.data
    }

    suspend fun giveUpTask(groupId: Long, taskId: Long): Result<Task> = runCatching {
        val response = api().giveUpTask(groupId, taskId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "放弃任务失败" })
        }
        response.data
    }

    suspend fun updateTaskStatus(groupId: Long, taskId: Long, task: Task, status: TaskStatus): Result<Task> =
        runCatching {
            val response = api().updateTask(
                groupId,
                taskId,
                UpdateTaskRequest(
                    title = task.title,
                    description = task.description,
                    priority = task.priority,
                    status = status,
                    dueDate = task.dueDate
                )
            )
            if (response.code != 0 || response.data == null) {
                throw IllegalStateException(response.message.ifBlank { "更新任务失败" })
            }
            response.data
        }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
