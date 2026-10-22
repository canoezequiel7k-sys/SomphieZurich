package com.somphie.staff.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.somphie.staff.feature.auth.ui.SignedOutFlow
import com.somphie.staff.feature.stations.ui.StationPreparationScreen

//Composable principal de la app que reacciona al estado de autenticacion
@Composable
fun StaffApp(viewModel: AuthViewModel) {
    //Observa el StateFlow de autenticacion del viewModel
    val state by viewModel.state.collectAsState()

    //Animacion de fundido(fade in / fade out) al cambiar entre estados de sesion
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        label = "AppStateTransition"
    ) { current ->
        when (current) {
            // Muestra el indicador de carga al iniciar la app
            AuthUiState.LoadingSession -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Spinner circular centrado
                CircularProgressIndicator()
            }

            // Muestra el flujo de autenticacion (Splash + Login) cuando no hay sesion
            is AuthUiState.SignedOut -> SignedOutFlow(current, viewModel::signIn)

            // Muestra pantalla de cambio obligatorio de clave si el backend lo requiere
            is AuthUiState.ChangePassword -> ChangePasswordScreen(current, viewModel::changePassword)

            // Muestra la pantalla operativa cuando el usuario ya inicio sesion
            is AuthUiState.SignedIn -> StationPreparationScreen(current.profile, viewModel::signOut)

            // Muestra pantalla de error si falla la lectura de credenciales cifradas
            is AuthUiState.RestoreError -> SessionRestoreError(
                code = current.errorCode,
                onRetry = viewModel::retryProfile,
                onClearSession = viewModel::clearUnreadableSession,
            )
        }
    }
}
