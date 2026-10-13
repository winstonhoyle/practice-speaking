package io.winstonhoyle.practicespeaking.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.TutorModelBuilder
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.data.settings.ThemeMode
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.error.appError
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface KeyCheck {
    data object NotChecked : KeyCheck

    data object Checking : KeyCheck

    data object Valid : KeyCheck

    data class Invalid(val error: AppError) : KeyCheck
}

data class SettingsUiState(
    val loading: Boolean = true,
    val provider: Provider = Provider.ANTHROPIC,
    val language: TutorLanguage = TutorLanguage.RUSSIAN,
    val apiKey: String = "",
    val model: String = Provider.ANTHROPIC.defaultModel,
    val keyCheck: KeyCheck = KeyCheck.NotChecked,
    /** The models this key can use, filled in after a passing key check. Empty means a free-text field. */
    val models: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.System,
) {
    val canStart: Boolean get() = apiKey.isNotBlank()
}

/**
 * Holds the edits on the Settings screen and saves each change right away. The key and model
 * fields show the values saved for the selected provider.
 */
class SettingsViewModel(private val settings: SettingsRepository, private val tutorModels: TutorModelBuilder) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    private var checkJob: Job? = null

    init {
        viewModelScope.launch {
            val provider = settings.provider.first()
            _state.value =
                loadProvider(provider).copy(language = settings.language.first(), themeMode = settings.themeMode.first(), loading = false)
        }
    }

    fun selectProvider(provider: Provider) {
        if (provider == _state.value.provider) return
        checkJob?.cancel()
        viewModelScope.launch {
            settings.setProvider(provider)
            _state.value =
                loadProvider(provider).copy(language = _state.value.language, themeMode = _state.value.themeMode, loading = false)
        }
    }

    fun selectLanguage(language: TutorLanguage) {
        _state.update { it.copy(language = language) }
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun selectThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun updateApiKey(key: String) {
        checkJob?.cancel()
        val provider = _state.value.provider
        _state.update { it.copy(apiKey = key, keyCheck = KeyCheck.NotChecked, models = emptyList()) }
        viewModelScope.launch { settings.setApiKey(provider, key) }
    }

    fun updateModel(model: String) {
        checkJob?.cancel()
        val provider = _state.value.provider
        // A model from the verified list keeps the passing check; anything else needs a new one.
        _state.update {
            val stillValid = it.keyCheck == KeyCheck.Valid && model.trim() in it.models
            it.copy(model = model, keyCheck = if (stillValid) KeyCheck.Valid else KeyCheck.NotChecked)
        }
        viewModelScope.launch { settings.setModel(provider, model) }
    }

    /**
     * Asks the provider whether the key and model work, then loads the models the key can use.
     * Costs nothing: no text is generated. A failed model list just leaves the field free-text.
     */
    fun testKey() {
        val current = _state.value
        if (!current.canStart) return
        checkJob?.cancel()
        _state.update { it.copy(keyCheck = KeyCheck.Checking) }
        checkJob =
            viewModelScope.launch {
                val model = current.model.trim().ifEmpty { current.provider.defaultModel }
                val tutor = tutorModels(current.provider, current.apiKey.trim(), model)
                val check = tutor.testKey().appError()?.let { KeyCheck.Invalid(it) } ?: KeyCheck.Valid
                val models = if (check == KeyCheck.Valid) tutor.listModels().getOrNull().orEmpty() else emptyList()
                _state.update { it.copy(keyCheck = check, models = models) }
            }
    }

    /** A saved key that no longer decrypts shows as a failed check, so the learner knows to paste it again. */
    private suspend fun loadProvider(provider: Provider) =
        SettingsUiState(
            provider = provider,
            apiKey = settings.apiKey(provider).first().orEmpty(),
            model = settings.model(provider).first(),
            keyCheck = if (settings.keyUnreadable(provider).first()) KeyCheck.Invalid(StorageError.KeyUnreadable) else KeyCheck.NotChecked,
        )
}
