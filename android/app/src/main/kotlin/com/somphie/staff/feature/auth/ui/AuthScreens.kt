package com.somphie.staff.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.somphie.staff.R

@Composable
fun SignInScreen(
    state: AuthUiState.SignedOut,
    onSignIn: (String, String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.login_title), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        state.errorCode?.let { code ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(authErrorText(code)))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onSignIn(email, password) },
            enabled = !state.isSubmitting && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) CircularProgressIndicator()
            else Text(stringResource(R.string.login_action))
        }
    }
}

@Composable
fun ChangePasswordScreen(
    state: AuthUiState.ChangePassword,
    onChangePassword: (String, String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.password_change_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(state.profile.displayName)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.current_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = new,
            onValueChange = { new = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.new_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        state.errorCode?.let { code ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(authErrorText(code)))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onChangePassword(current, new) },
            enabled = !state.isSubmitting && current.isNotEmpty() && new.length >= 10,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) CircularProgressIndicator()
            else Text(stringResource(R.string.change_password_action))
        }
    }
}

@Composable
fun SessionRestoreError(
    code: String,
    onRetry: () -> Unit,
    onClearSession: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
