package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.TaskRepository
import com.easywiki.model.Task
import com.easywiki.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyTasksUiState(
    val isLoading: Boolean = false,
    val inProgressTasks: List<Task> = emptyList(),
    val doneTasks: List<Task> = emptyList(),
    val selectedTab: Int = 0,
    val errorMessage: String? = null
)

class MyTasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTasksUiState())
    val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val inProgressResult = taskRepository.listMyTasks(TaskStatus.IN_PROGRESS)
            val doneResult = taskRepository.listMyTasks(TaskStatus.DONE)

            inProgressResult
                .onSuccess { inProgress ->
                    doneResult
                        .onSuccess { done ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    inProgressTasks = inProgress,
                                    doneTasks = done
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "加载失败"
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}

class MyTasksViewModelFactory(
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyTasksViewModel::class.java)) {
            return MyTasksViewModel(taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
