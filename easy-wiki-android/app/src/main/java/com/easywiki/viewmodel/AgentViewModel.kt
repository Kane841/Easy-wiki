package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.AgentRepository
import com.easywiki.model.AgentChatRequest
import com.easywiki.model.AgentChatTurn
import com.easywiki.model.AgentIntent
import com.easywiki.model.AgentTaskSuggestion
import com.easywiki.model.AgentTasksJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentMessage(
    val role: String,
    val content: String,
    val intent: AgentIntent? = null,
    val taskSuggestions: List<AgentTaskSuggestion> = emptyList()
)

data class AgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isAdopting: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class AgentViewModel(
    private val groupId: Long,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val tasksJsonAdapter = moshi.adapter(AgentTasksJson::class.java)

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<AgentChatTurn>()

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val message = _uiState.value.inputText.trim()
        if (message.isBlank() || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    inputText = "",
                    isLoading = true,
                    errorMessage = null,
                    messages = it.messages + AgentMessage(role = "user", content = message)
                )
            }

            val requestHistory = history.takeLast(MAX_HISTORY_TURNS)
            agentRepository.chat(groupId, AgentChatRequest(message = message, history = requestHistory))
                .onSuccess { response ->
                    val suggestions = if (response.intent == AgentIntent.TASK_SUGGEST) {
                        parseTaskSuggestions(response.reply)
                    } else {
                        emptyList()
                    }
                    val assistantMessage = AgentMessage(
                        role = "assistant",
                        content = stripJsonBlock(response.reply),
                        intent = response.intent,
                        taskSuggestions = suggestions
                    )
                    history.add(AgentChatTurn(role = "user", content = message))
                    history.add(AgentChatTurn(role = "assistant", content = response.reply))
                    trimHistory()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = it.messages + assistantMessage
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "发送失败")
                    }
                }
        }
    }

    fun adoptTaskSuggestions(suggestions: List<AgentTaskSuggestion>) {
        if (suggestions.isEmpty() || _uiState.value.isAdopting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAdopting = true, errorMessage = null) }
            agentRepository.createTasksFromSuggestions(groupId, suggestions)
                .onSuccess { tasks ->
                    _uiState.update {
                        it.copy(
                            isAdopting = false,
                            snackbarMessage = "已创建 ${tasks.size} 个任务"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isAdopting = false, errorMessage = error.message ?: "创建任务失败")
                    }
                }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun trimHistory() {
        while (history.size > MAX_HISTORY_TURNS) {
            history.removeAt(0)
        }
    }

    private fun parseTaskSuggestions(reply: String): List<AgentTaskSuggestion> {
        val jsonBlock = JSON_BLOCK_PATTERN.find(reply)?.groupValues?.get(1) ?: return emptyList()
        return try {
            tasksJsonAdapter.fromJson(jsonBlock)?.tasks.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun stripJsonBlock(reply: String): String {
        return reply.replace(JSON_BLOCK_PATTERN, "").trim()
    }

    companion object {
        private const val MAX_HISTORY_TURNS = 10
        private val JSON_BLOCK_PATTERN = Regex("""```json\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
    }
}

class AgentViewModelFactory(
    private val groupId: Long,
    private val agentRepository: AgentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgentViewModel::class.java)) {
            return AgentViewModel(groupId, agentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
