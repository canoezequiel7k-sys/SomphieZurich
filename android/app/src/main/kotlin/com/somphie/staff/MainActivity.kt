package com.somphie.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.somphie.staff.app.StaffApp
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.feature.auth.ui.AuthViewModel

/** Punto de entrada administrado por Hilt para resolver el ViewModel desde Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La paleta de la app es clara; los íconos oscuros mantienen legibles las barras transparentes.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            SomphieTheme {
                StaffApp(viewModel = hiltViewModel<AuthViewModel>())
            }
        }
    }
}
