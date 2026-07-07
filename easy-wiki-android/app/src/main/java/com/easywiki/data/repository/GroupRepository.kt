package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.CreateGroupRequest
import com.easywiki.model.Group

class GroupRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun listMyGroups(): Result<List<Group>> = runCatching {
        val response = api().listMyGroups()
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载群组失败" })
        }
        response.data
    }

    suspend fun createGroup(name: String, description: String?, discoverable: Boolean): Result<Group> =
        runCatching {
            val response = api().createGroup(
                CreateGroupRequest(name = name, description = description, discoverable = discoverable)
            )
            if (response.code != 0 || response.data == null) {
                throw IllegalStateException(response.message.ifBlank { "创建群组失败" })
            }
            response.data
        }

    suspend fun joinByInviteToken(token: String): Result<Group> = runCatching {
        val response = api().joinByInvite(token.trim())
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加入群组失败" })
        }
        response.data
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
