package com.somphie.staff.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.StaffProfile
import com.somphie.staff.core.storage.EncryptedTokenStore
import com.somphie.staff.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface AuthUiState {
    data object LoadingSession : AuthUiState
    data class SignedOut(
        val isSubmitting: Boolean = false,
        val errorCode: String? = null,
    ) : AuthUiState
    data class ChangePassword(
        val profile: StaffProfile,
        val isSubmitting: Boolean = false,
        val errorCode: String? = null,
    ) : AuthUiState
    data class SignedIn(val profile: StaffProfile) : AuthUiState
    data class RestoreError(val errorCode: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokens: EncryptedTokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.LoadingSession)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var sessionObserved = false
    private var savedSessionJob: Job? = null

    init {
        observeSavedSession()
    }

    private fun observeSavedSession() {
        savedSessionJob?.cancel()
        savedSessionJob = viewModelScope.launch {
            tokens.tokens
                .catch { _state.value = AuthUiState.RestoreError("SECURE_STORAGE_ERROR") }
                .collect { savedTokens ->
                    if (savedTokens == null) {
                        sessionObserved = false
                        if (_state.value !is AuthUiState.RestoreError) {
                            _state.value = AuthUiState.SignedOut()
                        }
                    } else if (!sessionObserved) {
                        sessionObserved = true
                        loadProfile()
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        if ((_state.value as? AuthUiState.SignedOut)?.isSubmitting == true) return
        _state.value = AuthUiState.SignedOut(isSubmitting = true)
        sessionObserved = true
        viewModelScope.launch {
            try {
                repository.signIn(email, password)
                loadProfile()
            } catch (failure: ApiFailure) {
                sessionObserved = false
                _state.value = AuthUiState.SignedOut(errorCode = failure.code)
            } catch (_: IOException) {
                sessionObserved = false
                _state.value = AuthUiState.SignedOut(errorCode = "NETWORK_ERROR")
            } catch (_: SerializationException) {
                sessionObserved = false
                _state.value = AuthUiState.SignedOut(errorCode = "INVALID_RESPONSE")
            } catch (_: IllegalStateException) {
                sessionObserved = false
                _state.value = AuthUiState.RestoreError("SECURE_STORAGE_ERROR")
            }
        }
    }

    fun changePassword(current: String, new: String) {
        val profile = (_state.value as? AuthUiState.ChangePassword)?.profile ?: return
        if ((_state.value as? AuthUiState.ChangePassword)?.isSubmitting == true) return
        _state.value = AuthUiState.ChangePassword(profile, isSubmitting = true)
        viewModelScope.launch {
            try {
                repository.changePassword(current, new)
                loadProfile()
            } catch (failure: ApiFailure) {
                _state.value = AuthUiState.ChangePassword(profile, errorCode = failure.code)
            } catch (_: IOException) {
                _state.value = AuthUiState.ChangePassword(profile, errorCode = "NETWORK_ERROR")
            } catch (_: SerializationException) {
                _state.value = AuthUiState.ChangePassword(profile, errorCode = "INVALID_RESPONSE")
            }
        }
    }

    fun retryProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun clearUnreadableSession() {
        viewModelScope.launch {
            tokens.clear()
            sessionObserved = false
            _state.value = AuthUiState.SignedOut()
            observeSavedSession()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                repository.signOut()
                _state.value = AuthUiState.SignedOut()
            } catch (_: IOException) {
                _state.value = AuthUiState.SignedOut(errorCode = "LOGOUT_NETWORK_ERROR")
            } catch (failure: ApiFailure) {
                _state.value = AuthUiState.SignedOut(errorCode = failure.code)
            } catch (_: HttpException) {
                _state.value = AuthUiState.SignedOut(errorCode = "LOGOUT_ERROR")
            }
        }
    }

    private suspend fun loadProfile() {
        _state.value = AuthUiState.LoadingSession
        try {
            val profile = repository.currentProfile()
            _state.value = if (profile.mustChangePassword) {
                AuthUiState.ChangePassword(profile)
            } else {
                AuthUiState.SignedIn(profile)
            }
        } catch (failure: ApiFailure) {
            val saved = tokens.current()
            _state.value = if (saved == null) {
                AuthUiState.SignedOut(errorCode = "SESSION_EXPIRED")
            } else {
                AuthUiState.RestoreError(failure.code)
            }
        } catch (_: IOException) {
            _state.value = AuthUiState.RestoreError("NETWORK_ERROR")
        } catch (_: SerializationException) {
            _state.value = AuthUiState.RestoreError("INVALID_RESPONSE")
        } catch (_: HttpException) {
            _state.value = AuthUiState.RestoreError("HTTP_ERROR")
        }
    }
}
