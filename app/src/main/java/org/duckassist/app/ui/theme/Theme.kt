package org.duckassist.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.duckassist.app.data.ThemeMode

private val DuckOrange = Color(0xFFDE5833)
private val DuckOrangeContainer = Color(0xFFFFDBCC)
private val DuckDarkSurface = Color(0xFF1C1B1F)
private val DuckDarkBackground = Color(0xFF121212)

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
            primary = DuckOrange,
            primaryContainer = Color(0xFF5C2A16),
            onPrimaryContainer = DuckOrangeContainer,
            background = DuckDarkBackground,
            surface = DuckDarkSurface
        )
    } else {
        lightColorScheme(
            primary = DuckOrange,
            primaryContainer = DuckOrangeContainer,
            onPrimaryContainer = Color(0xFF3A1405),
            background = Color(0xFFFDF7F5),
            surface = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}
