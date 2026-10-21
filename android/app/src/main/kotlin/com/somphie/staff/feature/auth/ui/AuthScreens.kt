package com.somphie.staff.feature.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.R

//Enumeracion para controlar el paso entre Splash y el login
enum class AuthStep{
    SPLASH, LOGIN
}

//Composable que anima la transicion entre Splash y Login
@Composable
fun SignedOutFlow(
    state: AuthUiState.SignedOut,
    onSignIn: (String, String) -> Unit,
) {
    //Estado local para saber en que paso esta el usuario(Splash o Login)
    var currentStep by remember { mutableStateOf(AuthStep.SPLASH) }

    //Contenedor animado con deslizamiento horizontal
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState == AuthStep.LOGIN){
                //Animacion al avanzar hacia el Login, se desliza de derecha a izquierda
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                //Animacion al volver hacia el Splash (Desliza de izquierda a derecha
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AuthFlowTransition"
    ) { step ->
        when(step){
            //Pantalla 1, muestra el splash de bienvenida
            AuthStep.SPLASH -> SplashScreen(
                onGoToLogin = {currentStep = AuthStep.LOGIN}
            )
            //Pantalla 2, Muestra el formulario para iniciar sesion
            AuthStep.LOGIN -> SignInFormScreen(
                state = state,
                onSignIn = onSignIn,
                onBackToSplash = {currentStep = AuthStep.SPLASH}
            )
        }
    }
}

//Pantalla 1: Splash
@Composable
private fun SplashScreen(onGoToLogin: () -> Unit) {
    val coffeeColor = Color(0xFF3A2B22)
    val creamColor = Color(0xFFF6F1E8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coffeeColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            //Icono de la marca
            Icon(
                painter = painterResource(R.drawable.ic_logo.svg),
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))

            //Titulo
            Text(
                text = "Somphie",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = creamColor
            )
            Text(
                text = "ZÚRICH",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
                color = creamColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            //Texto descriptivo en las areas de la cafeteria
            Text(
                text = "Coffee · Bar · Cuisine",
                style = MaterialTheme.typography.bodyMedium,
                color = creamColor.copy(alpha = 0.6f)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onGoToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = coffeeColor,
                    containerColor = creamColor
                )
            ) {
                Text(
                    text = stringResource(R.string.login_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))

            //Pie de pagina con la version y aviso de seguridad
            Text(
                text = "v1.0.0 · Authorised staff only",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

//Formulario de Login


@Composable
fun SessionRestoreError(
    code: String,
    onRetry: () -> Unit,
    onClearSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(authErrorText(code)))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClearSession) { Text(stringResource(R.string.back_to_login)) }
    }
}

@Composable
private fun authErrorText(code: String): Int = when (code) {
    "INVALID_CREDENTIALS" -> R.string.login_invalid
    "ACCOUNT_TEMPORARILY_LOCKED" -> R.string.login_locked
    "SESSION_EXPIRED", "TOKEN_EXPIRED", "UNAUTHENTICATED" -> R.string.session_expired
    "NETWORK_ERROR" -> R.string.network_error
    "SECURE_STORAGE_ERROR" -> R.string.secure_storage_error
    "PASSWORD_TOO_WEAK" -> R.string.password_too_weak
    else -> R.string.generic_error
}
