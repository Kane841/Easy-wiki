package com.easywiki.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.data.repository.AuthRepository
import com.easywiki.data.repository.GroupRepository
import com.easywiki.data.repository.TaskRepository
import com.easywiki.data.repository.WikiRepository
import com.easywiki.ui.auth.LoginScreen
import com.easywiki.ui.auth.ServerConfigScreen
import com.easywiki.ui.group.GroupListScreen
import com.easywiki.ui.task.TaskDetailScreen
import com.easywiki.ui.wiki.WikiDetailScreen
import com.easywiki.ui.workspace.WorkspaceScreen
import com.easywiki.viewmodel.AuthUiState
import com.easywiki.viewmodel.AuthViewModel
import com.easywiki.viewmodel.AuthViewModelFactory
import com.easywiki.viewmodel.GroupViewModel
import com.easywiki.viewmodel.GroupViewModelFactory
import com.easywiki.viewmodel.TaskViewModel
import com.easywiki.viewmodel.TaskViewModelFactory
import com.easywiki.viewmodel.WikiViewModel
import com.easywiki.viewmodel.WikiViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun EasyWikiNavGraph(
    settingsDataStore: SettingsDataStore,
    authRepository: AuthRepository,
    groupRepository: GroupRepository,
    wikiRepository: WikiRepository,
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val serverUrl by settingsDataStore.serverUrl.collectAsState(initial = "")
    val jwtToken by settingsDataStore.jwtToken.collectAsState(initial = null)
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val authUiState by authViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var isSavingServerUrl by remember { mutableStateOf(false) }
    var serverConfigError by remember { mutableStateOf<String?>(null) }

    val startDestination = remember(serverUrl, jwtToken) {
        when {
            serverUrl.isBlank() -> Routes.SERVER_CONFIG
            jwtToken.isNullOrBlank() && !authUiState.isLoggedIn -> Routes.LOGIN
            else -> Routes.GROUP_LIST
        }
    }

    LaunchedEffect(authUiState.isLoggedIn) {
        if (authUiState.isLoggedIn) {
            navController.navigate(Routes.GROUP_LIST) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    key(startDestination) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {
            composable(Routes.SERVER_CONFIG) {
                ServerConfigScreen(
                    isSaving = isSavingServerUrl,
                    errorMessage = serverConfigError,
                    onSave = { url ->
                        scope.launch {
                            isSavingServerUrl = true
                            serverConfigError = null
                            authRepository.checkHealth(url)
                                .onSuccess {
                                    settingsDataStore.setServerUrl(url)
                                    isSavingServerUrl = false
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.SERVER_CONFIG) { inclusive = true }
                                    }
                                }
                                .onFailure { error ->
                                    isSavingServerUrl = false
                                    serverConfigError = error.message ?: "无法连接服务器"
                                }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginRoute(
                    authUiState = authUiState,
                    authViewModel = authViewModel
                )
            }

            composable(Routes.GROUP_LIST) {
                val groupViewModel: GroupViewModel = viewModel(
                    factory = GroupViewModelFactory(groupRepository)
                )
                val groupUiState by groupViewModel.uiState.collectAsState()

                LaunchedEffect(groupUiState.joinedGroupId) {
                    groupUiState.joinedGroupId?.let { groupId ->
                        navController.navigate(Routes.workspace(groupId))
                        groupViewModel.clearJoinedGroupId()
                    }
                }

                GroupListScreen(
                    uiState = groupUiState,
                    onGroupClick = { groupId ->
                        navController.navigate(Routes.workspace(groupId))
                    },
                    onCreateGroup = groupViewModel::createGroup,
                    onJoinByToken = groupViewModel::joinByInviteToken,
                    onShowCreateDialog = groupViewModel::showCreateDialog,
                    onHideCreateDialog = groupViewModel::hideCreateDialog,
                    onShowJoinDialog = groupViewModel::showJoinDialog,
                    onHideJoinDialog = groupViewModel::hideJoinDialog
                )
            }

            composable(
                route = Routes.WORKSPACE,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                val groupViewModel: GroupViewModel = viewModel(
                    factory = GroupViewModelFactory(groupRepository)
                )
                val groupUiState by groupViewModel.uiState.collectAsState()
                val groupName = groupUiState.groups.find { it.id == groupId }?.name

                LaunchedEffect(groupId) {
                    if (groupUiState.groups.isEmpty()) {
                        groupViewModel.loadGroups()
                    }
                }

                val wikiViewModel: WikiViewModel = viewModel(
                    factory = WikiViewModelFactory(groupId, wikiRepository)
                )
                val wikiTreeState by wikiViewModel.treeState.collectAsState()

                val taskViewModel: TaskViewModel = viewModel(
                    factory = TaskViewModelFactory(groupId, taskRepository)
                )
                val taskBoardState by taskViewModel.boardState.collectAsState()

                WorkspaceScreen(
                    groupId = groupId,
                    groupName = groupName,
                    wikiViewModel = wikiViewModel,
                    wikiTreeState = wikiTreeState,
                    taskViewModel = taskViewModel,
                    taskBoardState = taskBoardState,
                    onWikiPageClick = { pageId ->
                        navController.navigate(Routes.wikiDetail(groupId, pageId))
                    },
                    onTaskClick = { taskId ->
                        navController.navigate(Routes.taskDetail(groupId, taskId))
                    }
                )
            }

            composable(
                route = Routes.WIKI_DETAIL,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.LongType },
                    navArgument("pageId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                val pageId = backStackEntry.arguments?.getLong("pageId") ?: return@composable
                val wikiViewModel: WikiViewModel = viewModel(
                    factory = WikiViewModelFactory(groupId, wikiRepository)
                )
                val detailState by wikiViewModel.detailState.collectAsState()

                WikiDetailScreen(
                    pageId = pageId,
                    uiState = detailState,
                    onLoad = wikiViewModel::loadPage,
                    onToggleEdit = wikiViewModel::toggleEditMode,
                    onTitleChange = wikiViewModel::updateEditTitle,
                    onContentChange = wikiViewModel::updateEditContent,
                    onSave = wikiViewModel::savePage,
                    onRefresh = wikiViewModel::refreshPage,
                    onSnackbarDismiss = wikiViewModel::clearSnackbar,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.TASK_DETAIL,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
                val taskViewModel: TaskViewModel = viewModel(
                    factory = TaskViewModelFactory(groupId, taskRepository)
                )
                val detailState by taskViewModel.detailState.collectAsState()

                TaskDetailScreen(
                    taskId = taskId,
                    uiState = detailState,
                    onLoad = taskViewModel::loadTask,
                    onAssigneeIdChange = taskViewModel::updateAssigneeIdInput,
                    onAssign = taskViewModel::assignTask,
                    onAccept = taskViewModel::acceptTask,
                    onReject = taskViewModel::rejectTask,
                    onClaim = taskViewModel::claimTask,
                    onSnackbarDismiss = taskViewModel::clearSnackbar,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun LoginRoute(
    authUiState: AuthUiState,
    authViewModel: AuthViewModel
) {
    LoginScreen(
        uiState = authUiState,
        onLogin = authViewModel::login,
        onRegister = authViewModel::register,
        onToggleMode = authViewModel::toggleRegisterMode
    )
}
