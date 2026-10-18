package com.somphie.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Confirma en Android real que Hilt y Compose llegan a una pantalla visible sin sesión guardada. */
@RunWith(AndroidJUnit4::class)
class AppStartupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchWithoutSavedSessionShowsSignInScreen() {
        composeRule.onNodeWithText("Ingresá al sistema").assertIsDisplayed()
        composeRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
    }

    @Test
    fun lightThemeUsesDarkSystemBarIcons() {
        val insets = WindowCompat.getInsetsController(
            composeRule.activity.window,
            composeRule.activity.window.decorView,
        )

        assertTrue(insets.isAppearanceLightStatusBars)
        assertTrue(insets.isAppearanceLightNavigationBars)
    }

    @Test
    fun unavailableBackendShowsRecoverableSignInError() {
        // El puerto 1 se reserva para esta prueba: garantiza un rechazo de conexión local e inmediato.
        assumeTrue(BuildConfig.API_BASE_URL.startsWith("http://127.0.0.1:1/"))

        composeRule.onNodeWithText("Correo electrónico").performTextInput("offline@example.invalid")
        composeRule.onNodeWithText("Contraseña").performTextInput("not-a-real-password")
        composeRule.onNodeWithText("Iniciar sesión").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(
                "No pudimos conectar con el servicio. Revisá la conexión e intentá de nuevo.",
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            "No pudimos conectar con el servicio. Revisá la conexión e intentá de nuevo.",
        ).assertIsDisplayed()
    }
}
