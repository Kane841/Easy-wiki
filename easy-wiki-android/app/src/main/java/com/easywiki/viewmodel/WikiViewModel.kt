package com.easywiki.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easywiki.data.api.WikiConflictException
import com.easywiki.data.repository.WikiRepository
import com.easywiki.model.WikiPage
import com.easywiki.model.WikiTreeNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlatWikiNode(
    val id: Long,
    val title: String,
    val depth: Int
)

data class WikiTreeUiState(
    val isLoading: Boolean = false,
    val tree: List<WikiTreeNode> = emptyList(),
    val flatNodes: List<FlatWikiNode> = emptyList(),
    val errorMessage: String? = null
)

data class WikiDetailUiState(
    val isLoading: Boolean = false,
    val page: WikiPage? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editContent: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class WikiViewModel(
    private val groupId: Long,
    private val wikiRepository: WikiRepository
) : ViewModel() {

    private val _treeState = MutableStateFlow(WikiTreeUiState())
    val treeState: StateFlow<WikiTreeUiState> = _treeState.asStateFlow()

    private val _detailState = MutableStateFlow(WikiDetailUiState())
    val detailState: StateFlow<WikiDetailUiState> = _detailState.asStateFlow()

    fun loadTree() {
        viewModelScope.launch {
            _treeState.update { it.copy(isLoading = true, errorMessage = null) }
            wikiRepository.getTree(groupId)
                .onSuccess { tree ->
                    _treeState.update {
                        it.copy(
                            isLoading = false,
                            tree = tree,
                            flatNodes = flattenTree(tree)
                        )
                    }
                }
                .onFailure { error ->
                    _treeState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun loadPage(pageId: Long) {
        viewModelScope.launch {
            _detailState.update { WikiDetailUiState(isLoading = true) }
            wikiRepository.getPage(groupId, pageId)
                .onSuccess { page ->
                    _detailState.update {
                        WikiDetailUiState(
                            isLoading = false,
                            page = page,
                            editTitle = page.title,
                            editContent = page.content.orEmpty()
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

    fun toggleEditMode() {
        _detailState.update { state ->
            val page = state.page ?: return@update state
            if (state.isEditing) {
                state.copy(
                    isEditing = false,
                    editTitle = page.title,
                    editContent = page.content.orEmpty()
                )
            } else {
                state.copy(isEditing = true)
            }
        }
    }

    fun updateEditTitle(title: String) {
        _detailState.update { it.copy(editTitle = title) }
    }

    fun updateEditContent(content: String) {
        _detailState.update { it.copy(editContent = content) }
    }

    fun savePage(pageId: Long) {
        val state = _detailState.value
        val page = state.page ?: return
        val version = page.version ?: return

        if (state.editTitle.isBlank()) {
            _detailState.update { it.copy(errorMessage = "标题不能为空") }
            return
        }

        viewModelScope.launch {
            _detailState.update { it.copy(isSaving = true, errorMessage = null) }
            wikiRepository.updatePage(
                groupId = groupId,
                pageId = pageId,
                title = state.editTitle.trim(),
                content = state.editContent,
                version = version
            )
                .onSuccess { updated ->
                    _detailState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            page = updated,
                            editTitle = updated.title,
                            editContent = updated.content.orEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    when (error) {
                        is WikiConflictException -> {
                            _detailState.update {
                                it.copy(
                                    isSaving = false,
                                    snackbarMessage = "页面已被他人更新，请刷新"
                                )
                            }
                        }
                        else -> {
                            _detailState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = error.message ?: "保存失败"
                                )
                            }
                        }
                    }
                }
        }
    }

    fun refreshPage(pageId: Long) {
        loadPage(pageId)
    }

    fun clearSnackbar() {
        _detailState.update { it.copy(snackbarMessage = null) }
    }

    private fun flattenTree(nodes: List<WikiTreeNode>, depth: Int = 0): List<FlatWikiNode> {
        val result = mutableListOf<FlatWikiNode>()
        for (node in nodes) {
            result.add(FlatWikiNode(id = node.id, title = node.title, depth = depth))
            result.addAll(flattenTree(node.children, depth + 1))
        }
        return result
    }
}

class WikiViewModelFactory(
    private val groupId: Long,
    private val wikiRepository: WikiRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WikiViewModel::class.java)) {
            return WikiViewModel(groupId, wikiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
