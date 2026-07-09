package com.easywiki.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val serverUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[SERVER_URL_KEY].orEmpty()
    }

    val jwtToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[JWT_TOKEN_KEY]
    }

    val userId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]?.toLongOrNull()
    }

    suspend fun getServerUrl(): String = serverUrl.first()

    suspend fun getJwtToken(): String? = jwtToken.first()

    suspend fun getUserId(): Long? = userId.first()

    suspend fun setServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url.trim()
        }
    }

    suspend fun setJwtToken(token: String?) {
        dataStore.edit { preferences ->
            if (token.isNullOrBlank()) {
                preferences.remove(JWT_TOKEN_KEY)
            } else {
                preferences[JWT_TOKEN_KEY] = token
            }
        }
    }

    suspend fun setUserId(id: Long?) {
        dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(USER_ID_KEY)
            } else {
                preferences[USER_ID_KEY] = id.toString()
            }
        }
    }

    suspend fun clearJwtToken() {
        setJwtToken(null)
        setUserId(null)
    }

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }
}
