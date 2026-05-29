package com.mdvplayer.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue10,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = BlueGrey80,
    onSecondary = BlueGrey30,
    secondaryContainer = BlueGrey30,
    onSecondaryContainer = BlueGrey90,
    background = DarkBackground,
    onBackground = Blue90,
    surface = DarkSurface,
    onSurface = Blue90,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = LightSurface,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = BlueGrey40,
    onSecondary = LightSurface,
    secondaryContainer = BlueGrey90,
    onSecondaryContainer = BlueGrey30,
    background = LightBackground,
    onBackground = Blue10,
    surface = LightSurface,
    onSurface = Blue10,
    surfaceVariant = Blue90,
    error = ErrorRed
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MDVPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MDVTypography,
            content = content
        )
    }
}
