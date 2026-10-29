package org.duckassist.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.duckassist.app.BuildConfig
import org.duckassist.app.DuckViewModel
import org.duckassist.app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: DuckViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Modelo padrão", style = MaterialTheme.typography.titleMedium)
            ModelPickerButton(
                models = settings.models,
                selected = settings.defaultModel,
                onSelect = { vm.updateDefaultModel(it) }
            )

            HorizontalDivider()

            Text("Tema", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeMode.values().forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = settings.theme == mode,
                            onClick = { vm.updateTheme(mode) }
                        )
                        Text(themeLabel(mode))
                    }
                }
            }

            Text("Prompt do sistema", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enviado antes de cada conversa para orientar as respostas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = settings.systemPrompt,
                onValueChange = { vm.updateSystemPrompt(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Sufixo das perguntas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Texto acrescentado automaticamente ao final de cada pergunta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = settings.askSuffix,
                onValueChange = { vm.updateAskSuffix(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Envio automático", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ao receber texto do assistente ou do compartilhamento, envia imediatamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.autoPrompt,
                    onCheckedChange = { vm.updateAutoPrompt(it) }
                )
            }

            Text("Modelos personalizados", style = MaterialTheme.typography.titleMedium)
            Text(
                "IDs de modelos do duck.ai separados por vírgula. Use quando novos modelos surgirem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = settings.customModels,
                onValueChange = { vm.updateCustomModels(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Text("URL base da API", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = settings.apiBaseUrl,
                onValueChange = { vm.updateApiBaseUrl(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            Text("Sobre", style = MaterialTheme.typography.titleMedium)
            Text(
                "DuckAssist ${BuildConfig.VERSION_NAME} — cliente Android nativo " +
                    "(Kotlin + Jetpack Compose), sem WebView, com zoom bloqueado e interface estática.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Reescrita nativa do projeto DuckAssist (diekjiu). O chat utiliza a API " +
                    "não oficial do duck.ai; a disponibilidade de modelos pode variar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Automático"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}
