package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.ChatMessage

class ChatRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun listMessages(groupId: Long, page: Int = 0, size: Int = 50): Result<List<ChatMessage>> =
        runCatching {
            val response = api().listChatMessages(groupId, page, size)
            if (response.code != 0 || response.data == null) {
                throw IllegalStateException(response.message.ifBlank { "加载聊天记录失败" })
            }
            response.data.content.reversed()
        }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
