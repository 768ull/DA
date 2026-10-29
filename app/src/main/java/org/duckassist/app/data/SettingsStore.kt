package org.duckassist.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultModel: String = DuckModels.DEFAULT_MODEL_ID,
    val systemPrompt: String = "",
    val askSuffix: String = "",
    val autoPrompt: Boolean = true,
    val customModels: String = "",
    val apiBaseUrl: String = DuckApiDefaults.BASE_URL
) {
    val models: List<DuckModel>
        get() = DuckModels.mergeCustom(customModels)

    companion object {
        val DEFAULT = AppSettings()
    }
}

/** Armazenamento de preferências (nome preservado do app original). */
private val Context.dataStore by preferencesDataStore(name = "duck_assist_prefs")

class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val ASK_SUFFIX = stringPreferencesKey("ask_suffix")
        val AUTO_PROMPT = booleanPreferencesKey("auto_prompt")
        val CUSTOM_MODELS = stringPreferencesKey("custom_models")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = when (p[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            defaultModel = p[Keys.DEFAULT_MODEL] ?: DuckModels.DEFAULT_MODEL_ID,
            systemPrompt = p[Keys.SYSTEM_PROMPT] ?: "",
            askSuffix = p[Keys.ASK_SUFFIX] ?: "",
            autoPrompt = p[Keys.AUTO_PROMPT] ?: true,
            customModels = p[Keys.CUSTOM_MODELS] ?: "",
            apiBaseUrl = p[Keys.API_BASE_URL]?.takeIf { it.isNotBlank() }
                ?: DuckApiDefaults.BASE_URL
        )
    }

    suspend fun setTheme(value: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME] = value.name }

    suspend fun setDefaultModel(value: String) =
        context.dataStore.edit { it[Keys.DEFAULT_MODEL] = value }

    suspend fun setSystemPrompt(value: String) =
        context.dataStore.edit { it[Keys.SYSTEM_PROMPT] = value }

    suspend fun setAskSuffix(value: String) =
        context.dataStore.edit { it[Keys.ASK_SUFFIX] = value }

    suspend fun setAutoPrompt(value: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_PROMPT] = value }

    suspend fun setCustomModels(value: String) =
        context.dataStore.edit { it[Keys.CUSTOM_MODELS] = value }

    suspend fun setApiBaseUrl(value: String) =
        context.dataStore.edit { it[Keys.API_BASE_URL] = value }
}
