package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.repository.GroupRepository
import com.easywiki.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupUiState(
    val isLoading: Boolean = false,
    val groups: List<Group> = emptyList(),
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
    val inviteToken: String? = null,
    val inviteExpiresAt: String? = null,
    val inviteError: String? = null,
    val isGeneratingInvite: Boolean = false,
    val joinedGroupId: Long? = null
)

class GroupViewModel(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.listMyGroups()
                .onSuccess { groups ->
                    _uiState.update { it.copy(isLoading = false, groups = groups) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, errorMessage = null) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = true, errorMessage = null) }
    }

    fun hideJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = false) }
    }

    fun createGroup(name: String, description: String?) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入群组名称") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.createGroup(name.trim(), description?.trim()?.ifBlank { null }, false)
                .onSuccess { group ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showCreateDialog = false,
                            joinedGroupId = group.id
                        )
                    }
                    loadGroups()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "创建失败")
                    }
                }
        }
    }

    fun joinByInviteToken(token: String) {
        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入邀请令牌") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.joinByInviteToken(token)
                .onSuccess { group ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showJoinDialog = false,
                            joinedGroupId = group.id
                        )
                    }
                    loadGroups()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加入失败")
                    }
                }
        }
    }

    fun clearJoinedGroupId() {
        _uiState.update { it.copy(joinedGroupId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showInviteDialog() {
        _uiState.update { it.copy(showInviteDialog = true, inviteToken = null, inviteError = null) }
    }

    fun hideInviteDialog() {
        _uiState.update { it.copy(showInviteDialog = false, inviteToken = null, inviteError = null) }
    }

    fun generateInviteToken(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingInvite = true, inviteError = null) }
            groupRepository.createInvite(groupId)
                .onSuccess { invite ->
                    _uiState.update {
                        it.copy(
                            isGeneratingInvite = false,
                            inviteToken = invite.token,
                            inviteExpiresAt = invite.expiresAt
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingInvite = false,
                            inviteError = error.message ?: "生成邀请令牌失败"
                        )
                    }
                }
        }
    }
}

class GroupViewModelFactory(
    private val groupRepository: GroupRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            return GroupViewModel(groupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
