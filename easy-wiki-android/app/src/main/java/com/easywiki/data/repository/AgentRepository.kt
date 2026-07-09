package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.AgentChatRequest
import com.easywiki.model.AgentChatResponse
import com.easywiki.model.AgentTaskCreateRequest
import com.easywiki.model.AgentTaskSuggestion
import com.easywiki.model.Task

class AgentRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun chat(groupId: Long, request: AgentChatRequest): Result<AgentChatResponse> = runCatching {
        val response = api().agentChat(groupId, request)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "Agent 回复失败" })
        }
        response.data
    }

    suspend fun createTasksFromSuggestions(
        groupId: Long,
        tasks: List<AgentTaskSuggestion>
    ): Result<List<Task>> = runCatching {
        val response = api().agentCreateTasks(groupId, AgentTaskCreateRequest(tasks = tasks))
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "创建任务失败" })
        }
        response.data
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
