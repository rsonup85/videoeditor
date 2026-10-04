package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VistaraColorScheme = darkColorScheme(
    primary = VistaraPrimary,
    onPrimary = VistaraTextPrimary,
    primaryContainer = VistaraDarkSurfaceHighlight,
    onPrimaryContainer = VistaraTextPrimary,
    secondary = VistaraSecondary,
    onSecondary = VistaraDarkBackground,
    secondaryContainer = VistaraDarkSurfaceVariant,
    onSecondaryContainer = VistaraSecondary,
    tertiary = VistaraTertiary,
    onTertiary = VistaraDarkBackground,
    background = VistaraDarkBackground,
    onBackground = VistaraTextPrimary,
    surface = VistaraDarkSurface,
    onSurface = VistaraTextPrimary,
    surfaceVariant = VistaraDarkSurfaceVariant,
    onSurfaceVariant = VistaraTextSecondary,
    outline = VistaraDarkSurfaceBorder,
    outlineVariant = VistaraDarkSurfaceHighlight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce studio dark theme for professional editing consistency
    content: @Composable () -> Unit
) {
    val colorScheme = VistaraColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VistaraDarkBackground.toArgb()
                window.navigationBarColor = VistaraDarkBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
