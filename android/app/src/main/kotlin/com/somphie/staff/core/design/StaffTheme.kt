package com.somphie.staff.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// core/design/StaffTheme.kt
object StaffColors {
    val Coffee = Color(0xFF3A2B22)
    val Cream = Color(0xFFF6F1E8)
    val Caramel = Color(0xFF945C34)
    val BadgeBrown = Color(0xFFB07255) // Color indicador para badges/notificaciones
    val Sage = Color(0xFF61715B)       // Verde Salvia para la estación activa "Cocina"
    val SoftSageBg = Color(0xFFE2EBE0) // Fondo suave para "En curso"
    val BlueGray = Color(0xFF59696E)
    val RaisedCream = Color(0xFFFBF8F2)
    val SubtleBorder = Color(0xFFDED5C8)

    // Colores semánticos para Estados
    val StatusPendingBg = Color(0xFFE6BEA5)
    val StatusPendingText = Color(0xFF5C3317)
    val StatusInPrepBg = Color(0xFFD4A373)
    val StatusInPrepText = Color(0xFF4A2810)
    val StatusReadyBg = Color(0xFF61715B)
    val StatusReadyText = Color(0xFFFFFFFF)
}

@Composable
fun SomphieTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = StaffColors.Caramel,
        onPrimary = StaffColors.Cream,
        secondary = StaffColors.Sage,
        onSecondary = StaffColors.Cream,
        tertiary = StaffColors.BlueGray,
        background = StaffColors.Cream,
        onBackground = StaffColors.Coffee,
        surface = StaffColors.RaisedCream,
        onSurface = StaffColors.Coffee,
        outline = StaffColors.SubtleBorder,
        secondaryContainer = StaffColors.SoftSageBg,
        onSecondaryContainer = StaffColors.Sage,
    )
    MaterialTheme(colorScheme = colors, content = content)
}

