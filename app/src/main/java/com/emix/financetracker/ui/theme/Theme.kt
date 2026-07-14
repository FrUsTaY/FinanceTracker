package com.emix.financetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = Black,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    primary = Primary,
    onPrimary = OnPrimary,
    onBackground = OnSurface,
    onSurface = OnSurface,
    secondary = AccentGreen,
    error = AccentRed
)

@Composable
fun FinanceTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
