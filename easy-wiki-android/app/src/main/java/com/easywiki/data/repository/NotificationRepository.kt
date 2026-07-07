package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.NotificationItem

class NotificationRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun listNotifications(): Result<List<NotificationItem>> = runCatching {
        val response = api().listNotifications()
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载通知失败" })
        }
        response.data
    }

    suspend fun markAsRead(notificationId: Long): Result<NotificationItem> = runCatching {
        val response = api().markNotificationRead(notificationId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "标记已读失败" })
        }
        response.data
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
