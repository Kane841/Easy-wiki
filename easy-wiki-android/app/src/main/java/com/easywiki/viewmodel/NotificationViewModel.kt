package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.NotificationRepository
import com.easywiki.data.ws.WebSocketManager
import com.easywiki.model.NotificationItem
import com.easywiki.util.DeepLinkDestination
import com.easywiki.util.NotificationNavigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null
)

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        viewModelScope.launch {
            webSocketManager.notifications.collect {
                loadNotifications()
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            notificationRepository.listNotifications()
                .onSuccess { notifications ->
                    _uiState.update { it.copy(isLoading = false, notifications = notifications) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun markAsReadAndNavigate(
        notification: NotificationItem,
        onNavigate: (DeepLinkDestination) -> Unit
    ) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notification.id)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map {
                                if (it.id == updated.id) updated else it
                            }
                        )
                    }
                }
            val destination = NotificationNavigation.resolveDestination(notification)
            destination?.let(onNavigate)
        }
    }
}

class NotificationViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val webSocketManager: WebSocketManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(notificationRepository, webSocketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
