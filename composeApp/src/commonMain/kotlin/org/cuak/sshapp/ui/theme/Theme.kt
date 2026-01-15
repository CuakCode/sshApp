package org.cuak.sshapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkColor,
    secondary = SecondaryDarkColor,
    tertiary = TertiaryDarkColor,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLightColor,
    secondary = SecondaryLightColor,
    tertiary = TertiaryLightColor
    // El resto de colores por defecto
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}