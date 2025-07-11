package com.example.artsgymapp_solo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // For getting a single value from Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Ensure this repository is the plain text admin toggle version
    private val repository = UserCredentialsRepository(application.applicationContext)

    // --- Admin Credentials (Plain Text) ---
    // Expose username (as it was)
    val adminUsernameFlow: StateFlow<String> = repository.adminUsernameFlow // Assuming repository has adminUsernameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserCredentialsRepository.DEFAULT_ADMIN_USERNAME
        )

    // Expose plain text password (for verification and admin mode logic)
    // This replaces internalPasswordHashStateFlow if using plain text
    private val adminPasswordPlainTextFlow: StateFlow<String> = repository.adminPasswordPlainTextFlow // Assuming repository has adminPasswordPlainTextFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserCredentialsRepository.DEFAULT_PLAIN_TEXT_PASSWORD
        )

    // --- LiveData exposed to Java UI (if needed, or use StateFlow directly in Kotlin/Compose) ---
    val adminUsernameLiveData: LiveData<String> = adminUsernameFlow.asLiveData()
    // You might not need to expose password LiveData if all logic is internal or via StateFlow

    // --- Admin Mode State ---
    private val _isAdminModeActive = MutableStateFlow<Boolean>(false) // Default to false
    val isAdminModeActive: StateFlow<Boolean> = _isAdminModeActive.asStateFlow()
    val isAdminModeActiveLiveData: LiveData<Boolean> = _isAdminModeActive.asLiveData() // For Java UI


    /**
     * Updates the admin credentials. Stores them in plain text via the repository.
     */
    fun updateAdminCredentials(newUsername: String, newPlainTextPassword: String) {
        viewModelScope.launch {
            repository.updateCredentials(newUsername, newPlainTextPassword) // Repository handles plain text
            // Optional: Consider if changing credentials should automatically log out admin mode
            // _isAdminModeActive.value = false
        }
    }

    /**
     * Verifies the entered plain text password against the stored plain text password.
     * This is for the admin login dialog.
     * @return True if the password matches, false otherwise.
     */
    fun verifyAdminPassword(enteredPassword: String): Boolean {
        // Uses the current value from the plain text password flow
        return repository.verifyPassword(enteredPassword, adminPasswordPlainTextFlow.value)
    }

    /**
     * Attempts to log in as admin and activate admin mode.
     * This will be called from MainActivity when the user tries to turn on the admin switch.
     */
    fun attemptAdminLogin(enteredUsername: String, enteredPassword: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val storedUsername = adminUsernameFlow.first() // Get current stored username
            // Note: adminPasswordPlainTextFlow.first() could also be used if not using .value
            // but .value is fine here since it's a StateFlow and eagerly collected.

            if (enteredUsername == storedUsername && repository.verifyPassword(enteredPassword, adminPasswordPlainTextFlow.value)) {
                _isAdminModeActive.value = true
                callback(true)
            } else {
                _isAdminModeActive.value = false
                callback(false)
            }
        }
    }

    fun logoutAdmin() {
        _isAdminModeActive.value = false
    }

    // Getter for repository (if MainActivity needs it for the simplified login button handler, though ideally not)
    // It's better if all repository interactions go through ViewModel methods.
    // fun getRepository(): UserCredentialsRepository = repository
}