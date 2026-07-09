package com.easywiki

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.easywiki.data.fcm.FcmService
import com.easywiki.ui.navigation.EasyWikiNavGraph
import com.easywiki.ui.theme.EasyWikiTheme
import com.easywiki.util.DeepLinkDestination
import com.easywiki.util.DeepLinkParser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingDeepLink = mutableStateOf<DeepLinkDestination?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as EasyWikiApplication
        requestNotificationPermission()
        registerFcmToken(app)
        handleIntent(intent)

        setContent {
            EasyWikiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EasyWikiNavGraph(
                        settingsDataStore = app.settingsDataStore,
                        authRepository = app.authRepository,
                        groupRepository = app.groupRepository,
                        wikiRepository = app.wikiRepository,
                        taskRepository = app.taskRepository,
                        chatRepository = app.chatRepository,
                        notificationRepository = app.notificationRepository,
                        agentRepository = app.agentRepository,
                        webSocketManager = app.webSocketManager,
                        pendingDeepLink = pendingDeepLink.value,
                        onDeepLinkConsumed = { pendingDeepLink.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val targetUrl = intent?.getStringExtra(FcmService.EXTRA_TARGET_URL)
        if (!targetUrl.isNullOrBlank()) {
            pendingDeepLink.value = DeepLinkParser.parse(targetUrl)
            return
        }
        val groupId = intent?.getStringExtra(FcmService.EXTRA_GROUP_ID)?.toLongOrNull()
        if (groupId != null) {
            pendingDeepLink.value = DeepLinkDestination.Workspace(groupId)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun registerFcmToken(app: EasyWikiApplication) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            activityScope.launch {
                app.deviceRepository.registerDevice(token)
            }
        }
    }
}
