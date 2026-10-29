package org.duckassist.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.duckassist.app.ui.AppRoot
import org.duckassist.app.ui.theme.DuckAssistTheme

class MainActivity : ComponentActivity() {

    /** Texto recebido por intent (assistente, texto selecionado ou compartilhado). */
    var pendingPrompt by mutableStateOf<String?>(null)
        private set

    override fun attachBaseContext(newBase: Context) {
        // Zoom bloqueado: a interface é sempre renderizada com a escala de
        // fonte padrão, independentemente da configuração do sistema.
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPrompt = intent.getStringExtra(EXTRA_PROMPT)

        val app = application as DuckAssistApp
        setContent {
            val vm: DuckViewModel = viewModel {
                DuckViewModel(app.repository, app.settingsStore)
            }
            val settings by vm.settings.collectAsState()
            DuckAssistTheme(themeMode = settings.theme) {
                AppRoot(
                    vm = vm,
                    pendingPrompt = pendingPrompt,
                    onConsumePrompt = { pendingPrompt = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_PROMPT)?.let { pendingPrompt = it }
    }

    companion object {
        const val EXTRA_PROMPT = "org.duckassist.app.EXTRA_PROMPT"
    }
}
