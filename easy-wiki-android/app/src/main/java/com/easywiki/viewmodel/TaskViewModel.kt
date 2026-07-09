package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.TaskRepository
import com.easywiki.model.AssignmentStatus
import com.easywiki.model.Task
import com.easywiki.model.TaskPriority
import com.easywiki.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskBoardUiState(
    val isLoading: Boolean = false,
    val allTasks: List<Task> = emptyList(),
    val selectedTab: TaskStatus = TaskStatus.TODO,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false
)

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val isActionLoading: Boolean = false,
    val assigneeIdInput: String = "",
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class TaskViewModel(
    private val groupId: Long,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _boardState = MutableStateFlow(TaskBoardUiState())
    val boardState: StateFlow<TaskBoardUiState> = _boardState.asStateFlow()

    private val _detailState = MutableStateFlow(TaskDetailUiState())
    val detailState: StateFlow<TaskDetailUiState> = _detailState.asStateFlow()

    fun loadTasks() {
        viewModelScope.launch {
            _boardState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.listTasks(groupId)
                .onSuccess { tasks ->
                    _boardState.update { it.copy(isLoading = false, allTasks = tasks) }
                }
                .onFailure { error ->
                    _boardState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun selectTab(status: TaskStatus) {
        _boardState.update { it.copy(selectedTab = status) }
    }

    fun tasksForTab(status: TaskStatus): List<Task> {
        return _boardState.value.allTasks.filter { it.status == status }
    }

    fun showCreateDialog() {
        _boardState.update { it.copy(showCreateDialog = true, errorMessage = null) }
    }

    fun hideCreateDialog() {
        _boardState.update { it.copy(showCreateDialog = false) }
    }

    fun createTask(title: String, description: String?, priority: TaskPriority?) {
        if (title.isBlank()) {
            _boardState.update { it.copy(errorMessage = "请输入任务标题") }
            return
        }
        viewModelScope.launch {
            _boardState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.createTask(groupId, title.trim(), description?.trim()?.ifBlank { null }, priority)
                .onSuccess {
                    _boardState.update { it.copy(isLoading = false, showCreateDialog = false) }
                    loadTasks()
                }
                .onFailure { error ->
                    _boardState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "创建失败")
                    }
                }
        }
    }

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _detailState.update { TaskDetailUiState(isLoading = true) }
            taskRepository.getTask(groupId, taskId)
                .onSuccess { task ->
                    _detailState.update {
                        TaskDetailUiState(
                            isLoading = false,
                            task = task,
                            assigneeIdInput = task.assigneeId?.toString().orEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    _detailState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun updateAssigneeIdInput(value: String) {
        _detailState.update { it.copy(assigneeIdInput = value) }
    }

    fun assignTask(taskId: Long) {
        val assigneeId = _detailState.value.assigneeIdInput.toLongOrNull()
        if (assigneeId == null) {
            _detailState.update { it.copy(errorMessage = "请输入有效的用户 ID") }
            return
        }
        performAction(taskId) { taskRepository.assignTask(groupId, taskId, assigneeId) }
    }

    fun acceptTask(taskId: Long) {
        performAction(taskId) { taskRepository.acceptTask(groupId, taskId) }
    }

    fun rejectTask(taskId: Long) {
        performAction(taskId) { taskRepository.rejectTask(groupId, taskId) }
    }

    fun claimTask(taskId: Long) {
        performAction(taskId) { taskRepository.claimTask(groupId, taskId) }
    }

    fun clearSnackbar() {
        _detailState.update { it.copy(snackbarMessage = null) }
    }

    private fun performAction(taskId: Long, action: suspend () -> Result<Task>) {
        viewModelScope.launch {
            _detailState.update { it.copy(isActionLoading = true, errorMessage = null) }
            action()
                .onSuccess { task ->
                    _detailState.update {
                        it.copy(
                            isActionLoading = false,
                            task = task,
                            assigneeIdInput = task.assigneeId?.toString().orEmpty(),
                            snackbarMessage = "操作成功"
                        )
                    }
                    loadTasks()
                }
                .onFailure { error ->
                    _detailState.update {
                        it.copy(isActionLoading = false, errorMessage = error.message ?: "操作失败")
                    }
                }
        }
    }
}

class TaskViewModelFactory(
    private val groupId: Long,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(groupId, taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

fun Task.canClaim(): Boolean =
    assignmentStatus == AssignmentStatus.UNASSIGNED || assigneeId == null

fun Task.canAccept(): Boolean = assignmentStatus == AssignmentStatus.PENDING_ACCEPT

fun Task.canReject(): Boolean = assignmentStatus == AssignmentStatus.PENDING_ACCEPT

fun Task.canAssign(): Boolean =
    assignmentStatus == AssignmentStatus.UNASSIGNED || assignmentStatus == null
