package io.winstonhoyle.practicespeaking.ui.conversation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.error.SpeechError
import io.winstonhoyle.practicespeaking.speech.SpeechInput
import io.winstonhoyle.practicespeaking.ui.AppErrorBanner
import io.winstonhoyle.practicespeaking.ui.ErrorAction
import io.winstonhoyle.practicespeaking.ui.components.BannerTone
import io.winstonhoyle.practicespeaking.ui.components.ChatInputBar
import io.winstonhoyle.practicespeaking.ui.components.ChatMessageItem
import io.winstonhoyle.practicespeaking.ui.components.CommandChipRow
import io.winstonhoyle.practicespeaking.ui.components.EmptyState
import io.winstonhoyle.practicespeaking.ui.components.LanguageBadge
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.components.MessageBubble
import io.winstonhoyle.practicespeaking.ui.components.MetaRow
import io.winstonhoyle.practicespeaking.ui.components.MicButton
import io.winstonhoyle.practicespeaking.ui.components.MicState
import io.winstonhoyle.practicespeaking.ui.components.ProviderBadge
import io.winstonhoyle.practicespeaking.ui.components.PsBanner
import io.winstonhoyle.practicespeaking.ui.components.PsIcons
import io.winstonhoyle.practicespeaking.ui.components.PsTopBar
import io.winstonhoyle.practicespeaking.ui.components.SummarySheet
import io.winstonhoyle.practicespeaking.ui.components.ThinkingIndicator
import io.winstonhoyle.practicespeaking.ui.components.TutorCommand
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** Wires the screen to the ViewModel, the microphone, and the mic permission. */
@Composable
fun ConversationRoute(viewModel: ConversationViewModel, onOpenSettings: () -> Unit, onFinish: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.reloadSettings()
        onPauseOrDispose { viewModel.stopSpeaking() }
    }

    val speechInput =
        remember {
            SpeechInput(
                context,
                object : SpeechInput.Listener {
                    override fun onPartial(text: String) = viewModel.onPartialTranscript(text)

                    override fun onFinal(text: String) = viewModel.onListenResult(text)

                    override fun onFailed(error: SpeechError) = viewModel.onListenFailed(error)
                },
            )
        }
    DisposableEffect(speechInput) { onDispose { speechInput.destroy() } }

    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
            if (!granted) viewModel.onListenFailed(SpeechError.NoPermission)
        }
    val currentLanguage by rememberUpdatedState(state.language)

    ConversationScreen(
        state = state,
        micAvailable = speechInput.available,
        onPressStart = {
            if (hasMicPermission) {
                viewModel.onListenStart()
                speechInput.start(currentLanguage.localeTag)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onPressEnd = { if (hasMicPermission) speechInput.stop() },
        onSend = viewModel::send,
        onCommand = viewModel::onCommand,
        onReplay = viewModel::replay,
        onErrorAction = { action ->
            when (action) {
                ErrorAction.OpenSettings -> {
                    onOpenSettings()
                }

                ErrorAction.Retry -> {
                    viewModel.retry()
                }

                ErrorAction.EndSession -> {
                    viewModel.endSession()
                }

                ErrorAction.OpenAppSettings -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                }

                ErrorAction.InstallVoice -> {
                    context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                }

                ErrorAction.Dismiss -> {
                    viewModel.dismissError()
                }
            }
        },
        onEndSession = viewModel::endSession,
        onOpenSettings = onOpenSettings,
        onFinish = onFinish,
        onBack = onBack,
    )
}

@Composable
fun ConversationScreen(
    state: ConversationUiState,
    micAvailable: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onSend: (String) -> Unit,
    onCommand: (TutorCommand) -> Unit,
    onReplay: (String) -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onEndSession: () -> Unit,
    onOpenSettings: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            PsTopBar(
                title = state.language.tutorName,
                onBack = onBack,
                actions = {
                    MetaRow(Modifier.padding(end = PsTheme.spacing.lg)) {
                        LanguageBadge(state.language)
                        ProviderBadge(state.provider)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
        ) {
            when {
                state.loading -> {
                    LoadingState(Modifier.weight(1f))
                }

                state.needsKey -> {
                    EmptyState(
                        icon = PsIcons.Settings,
                        message = stringResource(R.string.conversation_needs_key),
                        actionLabel = stringResource(R.string.action_open_settings),
                        onAction = onOpenSettings,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    Transcript(state, onReplay, Modifier.weight(1f))
                }
            }
            Banners(state, onErrorAction, onEndSession)
            Controls(state, micAvailable, onPressStart, onPressEnd, onSend, onCommand)
        }
    }

    if (state.ended) {
        val summary = state.summary
        if (summary != null) {
            SummarySheet(summary.mistakes, summary.newWords, summary.tip, onDismiss = onFinish)
        } else {
            val currentOnFinish by rememberUpdatedState(onFinish)
            LaunchedEffect(Unit) { currentOnFinish() }
        }
    }
}

@Composable
private fun Transcript(state: ConversationUiState, onReplay: (String) -> Unit, modifier: Modifier = Modifier) {
    if (state.messages.isEmpty() && !state.isListening) {
        EmptyState(
            icon = PsIcons.Mic,
            message = stringResource(R.string.conversation_empty, state.language.tutorName),
            modifier = modifier,
        )
        return
    }
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.isThinking) 1 else 0) + (if (state.isListening) 1 else 0)
    LaunchedEffect(itemCount) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md),
        contentPadding = PaddingValues(PsTheme.spacing.lg),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(state.messages, key = { it.id }) { message ->
            ChatMessageItem(
                text = message.text,
                isUser = message.isUser,
                correction = message.correction,
                onReplay = if (message.isUser) null else ({ onReplay(message.text) }),
                modifier = Modifier.animateItem(),
            )
        }
        if (state.isListening) {
            item(key = "partial") {
                MessageBubble(text = state.partialTranscript.ifEmpty { "…" }, isUser = true)
            }
        }
        if (state.isThinking) {
            item(key = "thinking") { ThinkingIndicator() }
        }
    }
}

@Composable
private fun Banners(state: ConversationUiState, onErrorAction: (ErrorAction) -> Unit, onEndSession: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
        modifier = Modifier.padding(horizontal = PsTheme.spacing.lg),
    ) {
        state.error?.let { AppErrorBanner(it, onErrorAction) }
        AnimatedVisibility(state.showLongSessionBanner) {
            PsBanner(
                stringResource(R.string.conversation_long_session),
                BannerTone.Info,
                actionLabel = stringResource(R.string.command_end_session),
                onAction = onEndSession,
            )
        }
        if (state.voiceMissing) AppErrorBanner(SpeechError.VoiceMissing, onErrorAction)
    }
}

@Composable
private fun Controls(
    state: ConversationUiState,
    micAvailable: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onSend: (String) -> Unit,
    onCommand: (TutorCommand) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val micState =
        when {
            !micAvailable || state.needsKey || state.ended || state.loading -> MicState.Disabled
            state.isListening -> MicState.Listening
            state.isThinking -> MicState.Thinking
            state.isSpeaking -> MicState.Speaking
            else -> MicState.Idle
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(PsTheme.spacing.lg),
    ) {
        CommandChipRow(onCommand = onCommand, enabled = state.canSend && state.messages.isNotEmpty())
        MicButton(micState, onPressStart = onPressStart, onPressEnd = onPressEnd)
        ChatInputBar(
            value = draft,
            onValueChange = { draft = it },
            onSend = {
                onSend(draft)
                draft = ""
            },
            enabled = state.canSend,
        )
    }
}
