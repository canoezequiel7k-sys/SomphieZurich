package com.somphie.staff.feature.auth.ui

import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val coffeeColor = Color(0xFF412616)
    val coffeeElementColor = Color(0xFF674A35)
    val creamColor = Color(0xFFF6F1E8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coffeeColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Plantita decorativa inferior izquierda
        Image(
            painter = painterResource(R.drawable.floor_v2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -154.dp, y = 168.dp)
                .rotate(42f)
                .size(454.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.65f,
            colorFilter = ColorFilter.tint(coffeeElementColor)
        )

        // Plantita decorativa inferior derecha
        Image(
            painter = painterResource(R.drawable.floor),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 148.dp, y = 180.dp)
                .rotate(-42f)
                .size(452.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.65f,
            colorFilter = ColorFilter.tint(coffeeElementColor)
        )
        // Plantita decorativa arriba izquierda
        Image(
            painter = painterResource(R.drawable.floor_v2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .rotate(152f)
                .offset(x = 94.dp, y = 190.dp)
                .size(454.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.65f,
            colorFilter = ColorFilter.tint(coffeeElementColor)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            //Icono de la marca
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = creamColor
            )

            //Titulo
            Text(
                text = "Somphie",
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = creamColor
            )
            Text(
                text = "Zúrich",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
                color = creamColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            //Texto descriptivo en las areas de la cafeteria
            Text(
                text = "Coffee · Bar · Cuisine",
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = creamColor.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(42.dp))

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
            Spacer(Modifier.weight(1f))

            //Pie de pagina con la version y aviso de seguridad
            Text(
                text = "v1.0.0 · Authorised staff only",
                style = MaterialTheme.typography.labelSmall,
                color = creamColor.copy(alpha = 0.6f)
            )
        }
    }
}

//Pantalla 2 Formulario de Login
@Composable
private fun SignInFormScreen(
    state: AuthUiState.SignedOut,
    onSignIn: (String, String) -> Unit,
    onBackToSplash: () -> Unit
) {
    val coffeeColor = Color(0xFF4E382A)
    val creamColor = Color(0xFFF6F1E8)
   //var para guardar lo que escribe el usuario en el campo User/Email
    var username by rememberSaveable { mutableStateOf("") }
    //var save password ingress
    var password by remember { mutableStateOf("") }
    //var para controlar la visibilidad de la contraseña
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    //var para recordar la sesion
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    //Administrador de foco para guardar el teclado cuando presiona Ingresar
    val focusManagger = LocalFocusManager.current
    //contexto de Android para mostrar el aviso emergente de contraseña
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Encabezado
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = coffeeColor
            )
            Text(
                text = "Somphie Zúrich",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light,
                color = coffeeColor
            )
            Spacer(Modifier.height(4.dp))
            //Subtitulo de la pantalla de login
            Text(
                text = stringResource(R.string.login_title),
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = coffeeColor.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomStart
            ){
                Text(
                    text = "Iniciar sesión",
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = coffeeColor
                )
            }
            Spacer(Modifier.height(18.dp))
            //Tarjeta que envuelve los campos del formulario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //Campo para ingresar
                    OutlinedTextField(
                        value = username,
                        onValueChange = {username = it},
                        modifier = Modifier.fillMaxWidth(),
                        label = {Text(stringResource(R.string.email_label))},
                        leadingIcon = {
                            //Icono de persona a la izquierda del campo
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    //Campo para ingresar la contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {Text(stringResource(R.string.password_label))},
                        leadingIcon = {
                            //Icono de candado a la izquierda del campo
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            //Boton de ojo a la derecha para mostrar y ocultar contraseña
                            val image = if(passwordVisible) R.drawable.password_disguise else R.drawable.show_password
                            IconButton(
                                onClick = {passwordVisible = !passwordVisible},
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(painter = painterResource(image), contentDescription = null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                //Ocultar teclado y enviar formulario si los datos son validos
                                focusManagger.clearFocus()
                                if (username.isNotBlank() && password.isNotEmpty()){
                                    onSignIn(username, password)
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    //Fila de casilla de verificacion para recordarme
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //Casilla para marcar recordarme
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = {rememberMe = it},
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        //Texto de recuerdame
                        Text(
                            text = "Recordarme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    //Panel de error si el servidor devuelve credenciales invalidas o error
                    state.errorCode?.let { code ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icono de advertencia de error
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                // Texto traducido del error
                                Text(
                                    text = stringResource(authErrorText(code)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    //Boton primario para Ingresar
                    Button(
                        onClick = {
                            //Ocultar el teclado y llamar a la funcion de inicio de sesion
                            focusManagger.clearFocus()
                            onSignIn(username, password)
                        },
                        enabled = !state.isSubmitting && username.isNotBlank() && password.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSubmitting){
                            //Spinner de carga mientras se envia la solicitud al backend
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            // Texto de la accion ingresar
                            Text(
                                text = "Ingresar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    //Boton de texto para consultar sobre la contraseña olvidada
                    TextButton(
                        onClick = {
                            //Muetsra aviso que el restablecimiento es mediante el encargado
                            Toast.makeText(
                                context,
                                "Comunicate con el encargado o dueño para restablecer tu contraseña",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    ) {
                        //Texto del enlace recuperar contraseña
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Pantalla para cambio obligatorio de contraseña cuando el servidor lo exige
@Composable
fun ChangePasswordScreen(
    state: AuthUiState.ChangePassword,
    onChangePassword: (String, String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Titulo de la pantalla de cambio de clave
        Text(
            stringResource(R.string.password_change_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        // Muestra el nombre del usuario conectado
        Text(state.profile.displayName)
        Spacer(Modifier.height(20.dp))

        // Campo para ingresar la contraseña actual
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.current_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))

        // Campo para ingresar la nueva contraseña
        OutlinedTextField(
            value = new,
            onValueChange = { new = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.new_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        // Mensaje de error si la contraseña es debil o invalida
        state.errorCode?.let { code ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(authErrorText(code)))
        }
        Spacer(Modifier.height(20.dp))

        // Boton para confirmar el cambio de clave
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

// Pantalla de error al intentar restaurar la sesion
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
        // Muestra el mensaje de error traducido
        Text(stringResource(authErrorText(code)))
        Spacer(Modifier.height(16.dp))

        // Boton para reintentar la lectura de sesion
        Button(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
        Spacer(Modifier.height(8.dp))

        // Boton para borrar credenciales y volver al inicio
        Button(onClick = onClearSession) { Text(stringResource(R.string.back_to_login)) }
    }
}

// Funcion auxiliar que mapea el codigo de error a un recurso de texto en strings.xml
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