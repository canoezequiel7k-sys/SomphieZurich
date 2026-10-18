package com.somphie.staff.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Coffee = Color(0xFF3A2B22)
private val Cream = Color(0xFFF6F1E8)
private val Caramel = Color(0xFF945C34)
private val Sage = Color(0xFF61715B)
private val BlueGray = Color(0xFF59696E)
private val RaisedCream = Color(0xFFFBF8F2)
private val SubtleBorder = Color(0xFFDED5C8)

@Composable
fun SomphieTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Caramel,
        onPrimary = Cream,
        secondary = Sage,
        onSecondary = Cream,
        tertiary = BlueGray,
        background = Cream,
        onBackground = Coffee,
        surface = RaisedCream,
        onSurface = Coffee,
        outline = SubtleBorder,
    )
    MaterialTheme(colorScheme = colors, content = content)
}
