package com.example.ezanvakti.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ezanvakti.model.Vakit

val Teal = Color(0xFF0D8C73)

private val LightColors = lightColorScheme(primary = Teal, secondary = Color(0xFF268C8C))
private val DarkColors = darkColorScheme(primary = Color(0xFF4FD1B5), secondary = Color(0xFF6CC5C5))

@Composable
fun EzanVaktiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

/** Vakte göre arka plan degradesi (üst, alt). */
fun vakitColors(vakit: Vakit): Pair<Color, Color> = when (vakit) {
    Vakit.IMSAK -> Color(0xFF1A1F54) to Color(0xFF5C458C)
    Vakit.GUNES -> Color(0xFFF28C59) to Color(0xFF8C5999)
    Vakit.OGLE -> Color(0xFF2980D9) to Color(0xFF1AB3BF)
    Vakit.IKINDI -> Color(0xFFD99940) to Color(0xFF268C8C)
    Vakit.AKSAM -> Color(0xFFE65940) to Color(0xFF592E73)
    Vakit.YATSI -> Color(0xFF0A0F2E) to Color(0xFF1F2959)
}
