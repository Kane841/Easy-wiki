package com.easywiki

import android.app.Application
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.data.repository.AgentRepository
import com.easywiki.data.repository.AuthRepository
import com.easywiki.data.repository.ChatRepository
import com.easywiki.data.repository.DeviceRepository
import com.easywiki.data.repository.GroupRepository
import com.easywiki.data.repository.NotificationRepository
import com.easywiki.data.repository.TaskRepository
import com.easywiki.data.repository.WikiRepository
import com.easywiki.data.ws.WebSocketManager
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

    lateinit var groupRepository: GroupRepository
        private set

    lateinit var wikiRepository: WikiRepository
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var notificationRepository: NotificationRepository
        private set

    lateinit var agentRepository: AgentRepository
        private set

    lateinit var deviceRepository: DeviceRepository
        private set

    lateinit var webSocketManager: WebSocketManager
        private set

    private var cachedToken: String? = null

    override fun onCreate() {
        super.onCreate()

        settingsDataStore = SettingsDataStore(this)
        settingsDataStore.jwtToken
            .onEach { cachedToken = it }
            .launchIn(applicationScope)

        val tokenProvider = { cachedToken }

        authRepository = AuthRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        groupRepository = GroupRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        wikiRepository = WikiRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        taskRepository = TaskRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        chatRepository = ChatRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        notificationRepository = NotificationRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        agentRepository = AgentRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        deviceRepository = DeviceRepository(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider
        )
        webSocketManager = WebSocketManager(
            settingsDataStore = settingsDataStore,
            tokenProvider = tokenProvider,
            scope = applicationScope
        )
    }
}
