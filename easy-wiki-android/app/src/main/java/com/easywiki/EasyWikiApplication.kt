package com.easywiki

import android.app.Application
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EasyWikiApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    private var cachedToken: String? = null

    override fun onCreate() {
        super.onCreate()

        settingsDataStore = SettingsDataStore(this)
        settingsDataStore.jwtToken
            .onEach { cachedToken = it }
            .launchIn(applicationScope)

        authRepository = AuthRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = { cachedToken }
        )
    }
}
