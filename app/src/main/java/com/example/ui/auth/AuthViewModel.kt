package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.security.AuthResult
import com.example.data.security.SecureAuthRepository
import com.example.data.security.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isAuthenticated: Boolean = false,
    val currentUser: UserSession? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRegisterMode: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = SecureAuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingSession = true)
            val session = authRepository.getActiveSession()
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    isCheckingSession = false,
                    isAuthenticated = true,
                    currentUser = session
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCheckingSession = false,
                    isAuthenticated = false
                )
            }
        }
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null,
            successMessage = null
        )
    }

    fun login(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese usuario y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (val result = authRepository.login(user, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.userSession,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun register(user: String, pass: String, passConfirm: String) {
        if (user.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Complete todos los campos")
            return
        }
        if (pass != passConfirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "Las contraseñas no coinciden")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            when (val result = authRepository.register(user, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.userSession,
                        errorMessage = null,
                        successMessage = "¡Cuenta creada exitosamente con cifrado KeyStore!"
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState(
                isCheckingSession = false,
                isAuthenticated = false,
                currentUser = null
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
