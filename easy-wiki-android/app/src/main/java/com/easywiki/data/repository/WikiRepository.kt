package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.api.WikiConflictException
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.UpdateWikiPageRequest
import com.easywiki.model.WikiPage
import com.easywiki.model.WikiTreeNode
import retrofit2.HttpException

class WikiRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun getTree(groupId: Long): Result<List<WikiTreeNode>> = runCatching {
        val response = api().getWikiTree(groupId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载 Wiki 树失败" })
        }
        response.data
    }

    suspend fun getPage(groupId: Long, pageId: Long): Result<WikiPage> = runCatching {
        val response = api().getWikiPage(groupId, pageId)
        if (response.code != 0 || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "加载页面失败" })
        }
        response.data
    }

    suspend fun updatePage(
        groupId: Long,
        pageId: Long,
        title: String,
        content: String?,
        version: Int
    ): Result<WikiPage> = runCatching {
        try {
            val response = api().updateWikiPage(
                groupId,
                pageId,
                UpdateWikiPageRequest(title = title, content = content, version = version)
            )
            if (response.code == 409) {
                throw WikiConflictException()
            }
            if (response.code != 0 || response.data == null) {
                throw IllegalStateException(response.message.ifBlank { "保存失败" })
            }
            response.data
        } catch (e: HttpException) {
            if (e.code() == 409) {
                throw WikiConflictException()
            }
            throw e
        }
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
