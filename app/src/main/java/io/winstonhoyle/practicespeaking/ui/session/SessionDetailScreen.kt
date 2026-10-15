package io.winstonhoyle.practicespeaking.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.data.db.displayText
import io.winstonhoyle.practicespeaking.data.db.isUser
import io.winstonhoyle.practicespeaking.data.db.summary
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.ui.AppErrorBanner
import io.winstonhoyle.practicespeaking.ui.ErrorAction
import io.winstonhoyle.practicespeaking.ui.components.ChatMessageItem
import io.winstonhoyle.practicespeaking.ui.components.EmptyState
import io.winstonhoyle.practicespeaking.ui.components.LanguageBadge
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.components.MetaRow
import io.winstonhoyle.practicespeaking.ui.components.ProviderBadge
import io.winstonhoyle.practicespeaking.ui.components.PsButton
import io.winstonhoyle.practicespeaking.ui.components.PsIcons
import io.winstonhoyle.practicespeaking.ui.components.PsTopBar
import io.winstonhoyle.practicespeaking.ui.components.SecondaryText
import io.winstonhoyle.practicespeaking.ui.components.SummaryCard
import io.winstonhoyle.practicespeaking.ui.errorMessage
import io.winstonhoyle.practicespeaking.ui.formatSessionDate
import io.winstonhoyle.practicespeaking.ui.history.languageOf
import io.winstonhoyle.practicespeaking.ui.history.providerLabel
import io.winstonhoyle.practicespeaking.ui.history.providerOf
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

@Composable
fun SessionDetailRoute(
    viewModel: SessionDetailViewModel,
    scrollToMessageId: Long?,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { onPauseOrDispose { viewModel.stopSpeaking() } }
    SessionDetailScreen(
        state = state,
        scrollToMessageId = scrollToMessageId,
        onBack = onBack,
        onHear = viewModel::replay,
        onTranslate = viewModel::toggleTranslation,
        onContinue = onContinue,
        onErrorAction = { action -> if (action == ErrorAction.OpenSettings) onOpenSettings() else viewModel.dismissTranslationError() },
    )
}

@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    scrollToMessageId: Long?,
    onBack: () -> Unit,
    onHear: (String) -> Unit,
    onTranslate: (Long) -> Unit,
    onContinue: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    Scaffold(
        modifier = modifier,
        topBar = {
            PsTopBar(
                title = session?.let { languageOf(it.language).tutorName } ?: "",
                onBack = onBack,
                actions = {
                    if (session != null) {
                        MetaRow(Modifier.padding(end = PsTheme.spacing.lg)) {
                            LanguageBadge(languageOf(session.language))
                            ProviderBadge(providerLabel(session.provider), logo = providerOf(session.provider)?.logo)
                        }
                    }
                },
            )
        },
        bottomBar = {
            // A session can only continue on a provider this build still knows.
            if (session != null && providerOf(session.provider) != null) {
                PsButton(
                    text = stringResource(R.string.session_continue),
                    onClick = onContinue,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = PsTheme.spacing.xl, vertical = PsTheme.spacing.md),
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> {
                LoadingState(Modifier.padding(padding))
            }

            state.error != null -> {
                EmptyState(PsIcons.Error, errorMessage(state.error), Modifier.padding(padding))
            }

            session == null -> {
                EmptyState(PsIcons.Error, stringResource(R.string.session_missing), Modifier.padding(padding))
            }

            else -> {
                Column(Modifier.padding(padding)) {
                    state.translationError?.let { error ->
                        AppErrorBanner(
                            error,
                            onErrorAction,
                            Modifier.padding(horizontal = PsTheme.spacing.lg, vertical = PsTheme.spacing.sm),
                        )
                    }
                    Transcript(
                        state = state,
                        header = "${formatSessionDate(session.startedAt)} · ${session.model}",
                        summary = session.summary,
                        scrollToMessageId = scrollToMessageId,
                        onHear = onHear,
                        onTranslate = onTranslate,
                    )
                }
            }
        }
    }
}

@Composable
private fun Transcript(
    state: SessionDetailUiState,
    header: String,
    summary: Summary?,
    scrollToMessageId: Long?,
    onHear: (String) -> Unit,
    onTranslate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = state.messages
    val listState = rememberLazyListState()
    LaunchedEffect(messages.isNotEmpty(), scrollToMessageId) {
        val index = messages.indexOfFirst { it.id == scrollToMessageId }
        // +1 skips the header row.
        if (index >= 0) listState.scrollToItem(index + 1)
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md),
        contentPadding = PaddingValues(PsTheme.spacing.lg),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header") { SecondaryText(header) }
        items(messages, key = { it.id }) { message ->
            ChatMessageItem(
                text = message.displayText,
                isUser = message.isUser,
                correction = message.correction,
                translation = state.translations.visible(message.id, saved = message.translation),
                translating = message.id in state.translations.loading,
                onHear = { onHear(message.text) },
                onTranslate = { onTranslate(message.id) },
                onHearPhrase = onHear,
            )
        }
        if (summary != null) {
            item(key = "summary") { SummaryCard(summary.mistakes, summary.newWords, summary.tip) }
        }
    }
}
