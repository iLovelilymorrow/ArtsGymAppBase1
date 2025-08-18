package com.example.artsgymapp_solo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserCredentialsRepository(application.applicationContext)

    val adminUsernameFlow: StateFlow<String> = repository.adminUsernameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserCredentialsRepository.DEFAULT_ADMIN_USERNAME
        )

    private val adminPasswordPlainTextFlow: StateFlow<String> = repository.adminPasswordPlainTextFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserCredentialsRepository.DEFAULT_PLAIN_TEXT_PASSWORD
        )

    val adminUsernameLiveData: LiveData<String> = adminUsernameFlow.asLiveData()

    private val _isAdminModeActive = MutableStateFlow<Boolean>(false)
    val isAdminModeActiveLiveData: LiveData<Boolean> = _isAdminModeActive.asLiveData()

    fun updateAdminCredentials(newUsername: String, newPlainTextPassword: String) {
        viewModelScope.launch {
            repository.updateCredentials(newUsername, newPlainTextPassword)
        }
    }


    fun attemptAdminLogin(enteredUsername: String, enteredPassword: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val storedUsername = adminUsernameFlow.first()

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

    private val _runCount = MutableLiveData<Int>(0)
    val runCount: LiveData<Int> get() = _runCount

    fun setRunCount(value: Int) {
        _runCount.value = value
    }

    fun incrementRunCount() {
        _runCount.value = (_runCount.value ?: 0) + 1
    }

    fun getRunCountValue(): Int {
        return _runCount.value ?: 0
    }
}