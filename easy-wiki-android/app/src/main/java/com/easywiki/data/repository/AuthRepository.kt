package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.AuthResponse
import com.easywiki.model.LoginRequest
import com.easywiki.model.RegisterRequest

class AuthRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return runCatching {
            val response = api().login(LoginRequest(username, password))
            if (response.code != 0 || response.data == null) {
                throw IllegalStateException(response.message.ifBlank { "登录失败" })
            }
            settingsDataStore.setJwtToken(response.data.token)
            response.data
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> {
        return runCatching {
            val response = api().register(RegisterRequest(username, email, password))
            if (response.code != 0) {
                throw IllegalStateException(response.message.ifBlank { "注册失败" })
            }
        }
    }

    suspend fun checkHealth(serverUrl: String): Result<Unit> {
        return runCatching {
            val response = apiFactory(serverUrl, tokenProvider).health()
            if (response.code != 0) {
                throw IllegalStateException(response.message.ifBlank { "服务器不可用" })
            }
        }
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
