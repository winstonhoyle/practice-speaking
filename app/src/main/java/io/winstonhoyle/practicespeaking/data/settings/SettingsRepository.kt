package io.winstonhoyle.practicespeaking.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * User settings in DataStore. API keys and models are stored per provider, so switching
 * provider keeps the other keys. Keys are encrypted with [cipher] before they are written.
 */
class SettingsRepository(private val store: DataStore<Preferences>, private val cipher: SecretCipher) {
    val provider: Flow<Provider> =
        store.data
            .map { prefs -> Provider.entries.firstOrNull { it.name == prefs[PROVIDER] } ?: Provider.ANTHROPIC }
            .distinctUntilChanged()

    val language: Flow<TutorLanguage> =
        store.data
            .map { prefs -> TutorLanguage.entries.firstOrNull { it.name == prefs[LANGUAGE] } ?: TutorLanguage.RUSSIAN }
            .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> =
        store.data
            .map { prefs -> ThemeMode.entries.firstOrNull { it.name == prefs[THEME] } ?: ThemeMode.System }
            .distinctUntilChanged()

    /** The decrypted key, or null when none is saved or it can no longer be decrypted. */
    fun apiKey(provider: Provider): Flow<String?> =
        store.data
            .map { prefs -> prefs[keyFor(provider)]?.let { runCatching { cipher.decrypt(it) }.getOrNull() } }
            .distinctUntilChanged()

    /** True when a key is saved for [provider] but can't be decrypted (after a backup restore or a Keystore reset). */
    fun keyUnreadable(provider: Provider): Flow<Boolean> =
        store.data
            .map { prefs -> prefs[keyFor(provider)]?.let { runCatching { cipher.decrypt(it) }.isFailure } ?: false }
            .distinctUntilChanged()

    /** The model to use for [provider]: the user's override, or the provider default. */
    fun model(provider: Provider): Flow<String> =
        store.data
            .map { prefs -> prefs[modelFor(provider)]?.ifBlank { null } ?: provider.defaultModel }
            .distinctUntilChanged()

    /** Chooses the provider for new sessions. Each provider's key and model stay saved. */
    suspend fun setProvider(provider: Provider) = store.edit { it[PROVIDER] = provider.name }

    /** Light, dark, or follow the phone. */
    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }

    /** The language new sessions practise. */
    suspend fun setLanguage(language: TutorLanguage) = store.edit { it[LANGUAGE] = language.name }

    /** Saves the key for [provider]. A blank key removes it. */
    suspend fun setApiKey(provider: Provider, key: String) =
        store.edit {
            val trimmed = key.trim()
            if (trimmed.isEmpty()) it.remove(keyFor(provider)) else it[keyFor(provider)] = cipher.encrypt(trimmed)
        }

    /** Saves a model override for [provider]. A blank value goes back to the default. */
    suspend fun setModel(provider: Provider, model: String) =
        store.edit {
            val trimmed = model.trim()
            if (trimmed.isEmpty() || trimmed == provider.defaultModel) it.remove(modelFor(provider)) else it[modelFor(provider)] = trimmed
        }

    private companion object {
        val PROVIDER = stringPreferencesKey("provider")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")

        fun keyFor(provider: Provider) = stringPreferencesKey("api_key_${provider.name}")

        fun modelFor(provider: Provider) = stringPreferencesKey("model_${provider.name}")
    }
}
