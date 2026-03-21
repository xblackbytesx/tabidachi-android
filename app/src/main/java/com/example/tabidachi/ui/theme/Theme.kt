package com.example.tabidachi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsOled = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = IndigoAccentDim,
    onPrimaryContainer = Color.White,
    secondary = IndigoAccentLight,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderLight,
    error = ErrorRed,
    onError = Color.White,
)

private val OLEDColorScheme = DarkColorScheme.copy(
    background = OLEDBackground,
    surface = OLEDSurface,
    surfaceVariant = OLEDSurfaceVariant,
)

@Composable
fun TabidachiTheme(
    isOled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isOled) OLEDColorScheme else DarkColorScheme

    CompositionLocalProvider(LocalIsOled provides isOled) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TabidachiTypography,
            content = content,
        )
    }
}
