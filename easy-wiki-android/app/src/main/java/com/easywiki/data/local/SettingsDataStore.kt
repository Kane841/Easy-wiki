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

    suspend fun getServerUrl(): String = serverUrl.first()

    suspend fun getJwtToken(): String? = jwtToken.first()

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

    suspend fun clearJwtToken() {
        setJwtToken(null)
    }

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }
}
