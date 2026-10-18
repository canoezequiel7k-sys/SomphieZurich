package com.somphie.staff.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.somphie.staff.feature.auth.ui.AuthUiState
import com.somphie.staff.feature.auth.ui.AuthViewModel
import com.somphie.staff.feature.auth.ui.ChangePasswordScreen
import com.somphie.staff.feature.auth.ui.SessionRestoreError
import com.somphie.staff.feature.auth.ui.SignInScreen
import com.somphie.staff.feature.stations.ui.StationPreparationScreen

@Composable
fun StaffApp(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        AuthUiState.LoadingSession -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is AuthUiState.SignedOut -> SignInScreen(current, viewModel::signIn)
        is AuthUiState.ChangePassword -> ChangePasswordScreen(current, viewModel::changePassword)
        is AuthUiState.SignedIn -> StationPreparationScreen(current.profile, viewModel::signOut)
        is AuthUiState.RestoreError -> SessionRestoreError(
            code = current.errorCode,
            onRetry = viewModel::retryProfile,
            onClearSession = viewModel::clearUnreadableSession,
        )
    }
}
