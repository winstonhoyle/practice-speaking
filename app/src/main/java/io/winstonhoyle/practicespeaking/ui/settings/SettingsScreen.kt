package io.winstonhoyle.practicespeaking.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.settings.ThemeMode
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.components.FlagImage
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.components.MetaRow
import io.winstonhoyle.practicespeaking.ui.components.ProviderLogo
import io.winstonhoyle.practicespeaking.ui.components.PsButton
import io.winstonhoyle.practicespeaking.ui.components.PsButtonStyle
import io.winstonhoyle.practicespeaking.ui.components.PsDropdown
import io.winstonhoyle.practicespeaking.ui.components.PsTextField
import io.winstonhoyle.practicespeaking.ui.components.PsTopBar
import io.winstonhoyle.practicespeaking.ui.components.SecondaryText
import io.winstonhoyle.practicespeaking.ui.components.SecretTextField
import io.winstonhoyle.practicespeaking.ui.components.StatusLine
import io.winstonhoyle.practicespeaking.ui.errorMessage
import io.winstonhoyle.practicespeaking.ui.formatCount
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: (() -> Unit)?, onStart: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onProvider = viewModel::selectProvider,
        onApiKey = viewModel::updateApiKey,
        onModel = viewModel::updateModel,
        onLanguage = viewModel::selectLanguage,
        onLevel = viewModel::selectLevel,
        onVoiceSpeed = viewModel::selectVoiceSpeed,
        onThemeMode = viewModel::selectThemeMode,
        onTestKey = viewModel::testKey,
        onStart = onStart,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: (() -> Unit)?,
    onProvider: (Provider) -> Unit,
    onApiKey: (String) -> Unit,
    onModel: (String) -> Unit,
    onLanguage: (TutorLanguage) -> Unit,
    onLevel: (CefrLevel) -> Unit,
    onVoiceSpeed: (VoiceSpeed) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onTestKey: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { PsTopBar(stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PsTheme.spacing.xl, vertical = PsTheme.spacing.lg),
        ) {
            PsDropdown(
                label = stringResource(R.string.settings_provider),
                options = Provider.entries,
                selected = state.provider,
                optionLabel = { it.displayName },
                onSelect = onProvider,
                optionIcon = { ProviderLogo(it.logo, size = PsTheme.sizes.iconSmall) },
            )
            ApiKeySection(state, onApiKey, onTestKey)
            PsTextField(
                value = state.model,
                onValueChange = onModel,
                label = stringResource(R.string.settings_model),
                placeholder = state.provider.defaultModel,
                supportingText = stringResource(R.string.settings_model_hint, state.provider.defaultModel),
                suggestions = state.models,
            )
            val usage = state.usage
            SecondaryText(
                stringResource(
                    R.string.settings_usage,
                    state.provider.shortName,
                    formatCount(usage.input),
                    formatCount(usage.cached),
                    formatCount(usage.output),
                ),
                maxLines = Int.MAX_VALUE,
            )
            PsDropdown(
                label = stringResource(R.string.settings_language),
                options = TutorLanguage.entries,
                selected = state.language,
                optionLabel = { it.displayName },
                onSelect = onLanguage,
                optionIcon = { FlagImage(it, width = PsTheme.sizes.icon) },
            )
            PsDropdown(
                label = stringResource(R.string.settings_level),
                options = CefrLevel.entries,
                selected = state.level,
                optionLabel = { "${it.name} · ${it.label}" },
                onSelect = onLevel,
            )
            val speedLabels = VoiceSpeed.entries.associateWith { stringResource(it.label) }
            PsDropdown(
                label = stringResource(R.string.settings_voice_speed),
                options = VoiceSpeed.entries,
                selected = state.voiceSpeed,
                optionLabel = speedLabels::getValue,
                onSelect = onVoiceSpeed,
            )
            val themeLabels = ThemeMode.entries.associateWith { stringResource(it.label) }
            PsDropdown(
                label = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = state.themeMode,
                optionLabel = themeLabels::getValue,
                onSelect = onThemeMode,
            )
            PsButton(
                text = stringResource(R.string.settings_start),
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ApiKeySection(state: SettingsUiState, onApiKey: (String) -> Unit, onTestKey: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
        SecretTextField(
            value = state.apiKey,
            onValueChange = onApiKey,
            label = stringResource(R.string.settings_api_key, state.provider.shortName),
            placeholder = state.provider.keyHint,
            supportingText = { Text(stringResource(R.string.settings_key_stays_here)) },
        )
        MetaRow {
            PsButton(
                text = stringResource(R.string.settings_test_key),
                onClick = onTestKey,
                style = PsButtonStyle.Outlined,
                enabled = state.canStart,
                loading = state.keyCheck == KeyCheck.Checking,
            )
            PsButton(
                text = stringResource(R.string.settings_get_key),
                onClick = { uriHandler.openUri(state.provider.keyPageUrl) },
                style = PsButtonStyle.Text,
            )
        }
        when (val check = state.keyCheck) {
            KeyCheck.Valid -> StatusLine(ok = true, text = stringResource(R.string.settings_key_ok))
            is KeyCheck.Invalid -> StatusLine(ok = false, text = errorMessage(check.error))
            else -> Unit
        }
    }
}

private val VoiceSpeed.label: Int
    get() =
        when (this) {
            VoiceSpeed.Slow -> R.string.voice_speed_slow
            VoiceSpeed.Relaxed -> R.string.voice_speed_relaxed
            VoiceSpeed.Normal -> R.string.voice_speed_normal
        }

private val ThemeMode.label: Int
    get() =
        when (this) {
            ThemeMode.System -> R.string.theme_system
            ThemeMode.Light -> R.string.theme_light
            ThemeMode.Dark -> R.string.theme_dark
        }
