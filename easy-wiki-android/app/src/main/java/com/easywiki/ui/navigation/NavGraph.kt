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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easywiki.data.local.SettingsDataStore
import com.easywiki.data.repository.AuthRepository
import com.easywiki.ui.auth.LoginScreen
import com.easywiki.ui.auth.ServerConfigScreen
import com.easywiki.ui.home.HomeScreen
import com.easywiki.viewmodel.AuthUiState
import com.easywiki.viewmodel.AuthViewModel
import com.easywiki.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun EasyWikiNavGraph(
    settingsDataStore: SettingsDataStore,
    authRepository: AuthRepository,
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
            else -> Routes.HOME
        }
    }

    LaunchedEffect(authUiState.isLoggedIn) {
        if (authUiState.isLoggedIn) {
            navController.navigate(Routes.HOME) {
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

        composable(Routes.HOME) {
            val displayName = authUiState.username ?: jwtToken?.let { "用户" }
            HomeScreen(username = displayName)
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
