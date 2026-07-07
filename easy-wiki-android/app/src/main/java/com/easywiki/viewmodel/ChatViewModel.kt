package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.ChatRepository
import com.easywiki.data.ws.WebSocketManager
import com.easywiki.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val groupId: Long,
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            webSocketManager.chatMessages.collect { message ->
                if (message.groupId == groupId) {
                    appendMessage(message)
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            chatRepository.listMessages(groupId)
                .onSuccess { messages ->
                    _uiState.update { it.copy(isLoading = false, messages = messages) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            webSocketManager.sendChatMessage(groupId, content)
            _uiState.update { it.copy(isSending = false, inputText = "") }
        }
    }

    private fun appendMessage(message: ChatMessage) {
        _uiState.update { state ->
            if (state.messages.any { it.id == message.id }) {
                state
            } else {
                state.copy(messages = state.messages + message)
            }
        }
    }
}

class ChatViewModelFactory(
    private val groupId: Long,
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(groupId, chatRepository, webSocketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
