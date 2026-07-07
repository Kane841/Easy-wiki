package com.easywiki.data.repository

import com.easywiki.data.api.ApiClient
import com.easywiki.data.api.EasyWikiApi
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.RegisterDeviceRequest

class DeviceRepository(
    private val settingsDataStore: SettingsDataStore,
    private val apiFactory: (String, () -> String?) -> EasyWikiApi = { baseUrl, tokenProvider ->
        ApiClient.create(baseUrl, tokenProvider)
    },
    private val tokenProvider: () -> String? = { null }
) {

    suspend fun registerDevice(fcmToken: String, platform: String = "android"): Result<Unit> = runCatching {
        val response = api().registerDevice(RegisterDeviceRequest(fcmToken = fcmToken, platform = platform))
        if (response.code != 0) {
            throw IllegalStateException(response.message.ifBlank { "注册设备失败" })
        }
    }

    private suspend fun api(): EasyWikiApi {
        val serverUrl = settingsDataStore.getServerUrl()
        require(serverUrl.isNotBlank()) { "请先配置服务器地址" }
        return apiFactory(serverUrl, tokenProvider)
    }
}
