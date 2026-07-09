package com.easywiki.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.easywiki.data.repository.AuthRepository
import com.easywiki.model.AuthResponse

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginSuccess_updatesTokenState() = runTest {
        val repository = mock<AuthRepository>()
        whenever(repository.login("alice", "secret")).thenReturn(
            Result.success(AuthResponse(token = "token123", userId = 1L, username = "alice"))
        )

        val viewModel = AuthViewModel(repository)
        viewModel.login("alice", "secret")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("token123", state.token)
        assertEquals("alice", state.username)
        assertTrue(state.isLoggedIn)
        verify(repository).login("alice", "secret")
    }
}
