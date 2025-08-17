package com.example.artsgymapp_solo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserCredentialsRepository(private val context: Context) {

    private val dataStore = context.appSettingsDataStore

    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password_plaintext_or_hash")
    }

    companion object {
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_PLAIN_TEXT_PASSWORD = "pass"
    }

    val adminUsernameFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.USERNAME] ?: DEFAULT_ADMIN_USERNAME
        }

    val adminPasswordPlainTextFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.PASSWORD] ?: DEFAULT_PLAIN_TEXT_PASSWORD
        }

    suspend fun updateCredentials(username: String, plainTextPasswordToStore: String)
    {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USERNAME] = username
            preferences[PreferencesKeys.PASSWORD] = plainTextPasswordToStore
        }
    }

    fun verifyPassword(enteredPassword: String, storedPassword: String): Boolean
    {
        if (enteredPassword.isEmpty() || storedPassword.isEmpty()) {
            return false
        }

        val directMatch = enteredPassword == storedPassword
        return directMatch
    }
}