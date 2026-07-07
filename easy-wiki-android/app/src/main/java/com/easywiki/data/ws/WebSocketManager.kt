package com.easywiki.data.ws

import com.easywiki.data.local.SettingsDataStore
import com.easywiki.model.ChatMessage
import com.easywiki.model.NotificationWsPayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class WebSocketManager(
    private val settingsDataStore: SettingsDataStore,
    private val tokenProvider: () -> String?,
    private val scope: CoroutineScope
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val chatMessageAdapter = moshi.adapter(ChatMessage::class.java)
    private val notificationAdapter = moshi.adapter(NotificationWsPayload::class.java)

    private val client = OkHttpClient.Builder()
        .pingInterval(0, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _chatMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val chatMessages: SharedFlow<ChatMessage> = _chatMessages.asSharedFlow()

    private val _notifications = MutableSharedFlow<NotificationWsPayload>(extraBufferCapacity = 64)
    val notifications: SharedFlow<NotificationWsPayload> = _notifications.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var shouldStayConnected = false

    fun connect() {
        shouldStayConnected = true
        reconnectAttempt = 0
        openConnection()
    }

    fun disconnect() {
        shouldStayConnected = false
        reconnectJob?.cancel()
        pingJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    fun sendChatMessage(groupId: Long, content: String) {
        val json = """{"type":"CHAT_MESSAGE","payload":{"groupId":$groupId,"content":${org.json.JSONObject.quote(content)}}}"""
        webSocket?.send(json)
    }

    private fun sendPing() {
        webSocket?.send("""{"type":"PING","payload":null}""")
    }

    private fun openConnection() {
        if (!shouldStayConnected) return
        scope.launch {
            val token = tokenProvider() ?: settingsDataStore.getJwtToken()
            val serverUrl = settingsDataStore.getServerUrl()
            if (token.isNullOrBlank() || serverUrl.isBlank()) {
                scheduleReconnect()
                return@launch
            }

            _connectionState.value = WsConnectionState.CONNECTING
            val wsUrl = buildWsUrl(serverUrl, token)
            val request = Request.Builder().url(wsUrl).build()

            webSocket?.cancel()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    _connectionState.value = WsConnectionState.CONNECTED
                    startPingLoop()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncoming(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    stopPingLoop()
                    _connectionState.value = WsConnectionState.DISCONNECTED
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    stopPingLoop()
                    _connectionState.value = WsConnectionState.DISCONNECTED
                    scheduleReconnect()
                }
            })
        }
    }

    private fun handleIncoming(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "PONG" -> Unit
                "CHAT_MESSAGE" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    chatMessageAdapter.fromJson(payload.toString())?.let { message ->
                        scope.launch { _chatMessages.emit(message) }
                    }
                }
                "NOTIFICATION" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    notificationAdapter.fromJson(payload.toString())?.let { notification ->
                        scope.launch { _notifications.emit(notification) }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore malformed messages
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && shouldStayConnected) {
                delay(PING_INTERVAL_MS)
                sendPing()
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun scheduleReconnect() {
        if (!shouldStayConnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = min(
                INITIAL_RECONNECT_MS * (1L shl reconnectAttempt.coerceAtMost(MAX_BACKOFF_EXPONENT)),
                MAX_RECONNECT_MS
            )
            reconnectAttempt++
            delay(delayMs)
            if (shouldStayConnected) {
                openConnection()
            }
        }
    }

    companion object {
        private const val PING_INTERVAL_MS = 30_000L
        private const val INITIAL_RECONNECT_MS = 1_000L
        private const val MAX_RECONNECT_MS = 60_000L
        private const val MAX_BACKOFF_EXPONENT = 6

        fun buildWsUrl(serverUrl: String, token: String): String {
            val trimmed = serverUrl.trim().removeSuffix("/")
            val wsBase = when {
                trimmed.startsWith("https://", ignoreCase = true) ->
                    "wss://" + trimmed.removePrefix("https://").removePrefix("HTTPS://")
                trimmed.startsWith("http://", ignoreCase = true) ->
                    "ws://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
                else -> "ws://$trimmed"
            }
            return "$wsBase/ws?token=${java.net.URLEncoder.encode(token, Charsets.UTF_8.name())}"
        }
    }
}
