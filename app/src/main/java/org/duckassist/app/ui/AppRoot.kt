package org.duckassist.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.duckassist.app.DuckViewModel

/** Telas do aplicativo, navegadas por estado (sem dependência externa). */
sealed class Screen {
    object Chat : Screen()
    object History : Screen()
    object Settings : Screen()
}

@Composable
fun AppRoot(
    vm: DuckViewModel,
    pendingPrompt: String?,
    onConsumePrompt: () -> Unit
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Chat) }

    when (screen) {
        Screen.Chat -> ChatScreen(
            vm = vm,
            pendingPrompt = pendingPrompt,
            onConsumePrompt = onConsumePrompt,
            onHistory = { screen = Screen.History },
            onSettings = { screen = Screen.Settings }
        )
        Screen.History -> HistoryScreen(
            vm = vm,
            onBack = { screen = Screen.Chat },
            onOpen = { id ->
                vm.openConversation(id)
                screen = Screen.Chat
            }
        )
        Screen.Settings -> SettingsScreen(
            vm = vm,
            onBack = { screen = Screen.Chat }
        )
    }
}
