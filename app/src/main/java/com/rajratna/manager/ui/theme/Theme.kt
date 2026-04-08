package com.rajratna.manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E6FF),
    secondary = Amber600,
    onSecondary = Color.Black,
    error = Red600,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Gray100,
    onSurface = Gray800,
    onSurfaceVariant = Gray600,
    outline = Gray400
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = Color.Black,
    primaryContainer = Blue800,
    secondary = Amber300,
    onSecondary = Color.Black,
    error = Red400,
    background = DarkSurface,
    surface = DarkCard,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurface = Color.White,
    onSurfaceVariant = Gray400,
    outline = Gray600
)

@Composable
fun RajratnaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
