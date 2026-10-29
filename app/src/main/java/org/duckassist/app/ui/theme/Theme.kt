package org.duckassist.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.duckassist.app.data.ThemeMode

/**
 * Paleta oficial do duck.ai, extraída do design system do site
 * (variáveis --ds-surface-canvas, --ds-accent-primary, --ds-text-primary).
 */
private val DuckCanvasDark = Color(0xFF161616)
private val DuckSurfaceDark = Color(0xFF222222)
private val DuckContainerDark = Color(0xFF333333)
private val DuckAccentDark = Color(0xFF75B6EB)
private val DuckTextDark = Color(0xFFFFFFFF)
private val DuckTextSecondaryDark = Color(0xFFA8A8A8)
private val DuckBorderDark = Color(0xFF484848)

private val DuckCanvasLight = Color(0xFFFBFAF9)
private val DuckSurfaceLight = Color(0xFFFFFFFF)
private val DuckContainerLight = Color(0xFFF0F4FF)
private val DuckAccentLight = Color(0xFF1074CC)
private val DuckTextLight = Color(0xFF222222)
private val DuckTextSecondaryLight = Color(0xFF686868)
private val DuckBorderLight = Color(0xFFB2B0AD)

@Composable
fun DuckAssistTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = DuckAccentDark,
            onPrimary = Color(0xFF01142D),
            primaryContainer = DuckSurfaceDark,
            onPrimaryContainer = DuckAccentDark,
            secondary = DuckAccentDark,
            background = DuckCanvasDark,
            onBackground = DuckTextDark,
            surface = DuckCanvasDark,
            onSurface = DuckTextDark,
            surfaceVariant = DuckContainerDark,
            onSurfaceVariant = DuckTextSecondaryDark,
            surfaceContainer = DuckSurfaceDark,
            surfaceContainerHigh = DuckSurfaceDark,
            surfaceContainerHighest = DuckBorderDark,
            outline = DuckBorderDark,
            error = Color(0xFFE44D55)
        )
    } else {
        lightColorScheme(
            primary = DuckAccentLight,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = DuckContainerLight,
            onPrimaryContainer = Color(0xFF034180),
            secondary = DuckAccentLight,
            background = DuckCanvasLight,
            onBackground = DuckTextLight,
            surface = DuckCanvasLight,
            onSurface = DuckTextLight,
            surfaceVariant = DuckContainerLight,
            onSurfaceVariant = DuckTextSecondaryLight,
            surfaceContainer = DuckSurfaceLight,
            surfaceContainerHigh = Color(0xFFECEBE7),
            surfaceContainerHighest = Color(0xFFD1CECB),
            outline = DuckBorderLight,
            error = Color(0xFFCA2B3D)
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}
