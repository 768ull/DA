package org.duckassist.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.duckassist.app.data.AppSettings
import org.duckassist.app.data.ChatRepository
import org.duckassist.app.data.ConversationEntity
import org.duckassist.app.data.MessageEntity
import org.duckassist.app.data.SettingsStore
import org.duckassist.app.data.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
class DuckViewModel(
    private val repository: ChatRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val currentConversationId = MutableStateFlow<Long?>(null)

    val conversations: StateFlow<List<ConversationEntity>> = repository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<MessageEntity>> = currentConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.DEFAULT)

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun newChat() {
        currentConversationId.value = null
    }

    fun openConversation(id: Long) {
        currentConversationId.value = id
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (currentConversationId.value == id) currentConversationId.value = null
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAll()
            currentConversationId.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun send(rawText: String) {
        if (rawText.isBlank() || _running.value) return
        val s = settings.value
        val text = if (s.askSuffix.isBlank()) rawText
        else rawText.trimEnd() + "\n\n" + s.askSuffix
        viewModelScope.launch {
            _running.value = true
            _error.value = null
            try {
                val id = repository.send(currentConversationId.value, rawText, text, s.defaultModel)
                currentConversationId.value = id
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
            } finally {
                _running.value = false
            }
        }
    }

    fun updateDefaultModel(model: String) = launchSetting { settingsStore.setDefaultModel(model) }
    fun updateTheme(mode: ThemeMode) = launchSetting { settingsStore.setTheme(mode) }
    fun updateSystemPrompt(value: String) = launchSetting { settingsStore.setSystemPrompt(value) }
    fun updateAskSuffix(value: String) = launchSetting { settingsStore.setAskSuffix(value) }
    fun updateAutoPrompt(value: Boolean) = launchSetting { settingsStore.setAutoPrompt(value) }
    fun updateCustomModels(value: String) = launchSetting { settingsStore.setCustomModels(value) }
    fun updateApiBaseUrl(value: String) = launchSetting { settingsStore.setApiBaseUrl(value) }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
