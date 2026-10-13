package io.winstonhoyle.practicespeaking.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.data.db.MessageEntity
import io.winstonhoyle.practicespeaking.ui.components.ChatMessageItem
import io.winstonhoyle.practicespeaking.ui.components.EmptyState
import io.winstonhoyle.practicespeaking.ui.components.LanguageBadge
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.components.MetaRow
import io.winstonhoyle.practicespeaking.ui.components.ProviderBadge
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
fun SessionDetailRoute(viewModel: SessionDetailViewModel, scrollToMessageId: Long?, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionDetailScreen(state, scrollToMessageId, onBack)
}

@Composable
fun SessionDetailScreen(state: SessionDetailUiState, scrollToMessageId: Long?, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
                Transcript(
                    messages = state.messages,
                    header = "${formatSessionDate(session.startedAt)} · ${session.model}",
                    summary = Triple(session.summaryMistakes, session.summaryNewWords, session.summaryTip),
                    scrollToMessageId = scrollToMessageId,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun Transcript(
    messages: List<MessageEntity>,
    header: String,
    summary: Triple<String?, String?, String?>,
    scrollToMessageId: Long?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.isNotEmpty(), scrollToMessageId) {
        val index = messages.indexOfFirst { it.id == scrollToMessageId }
        // +1 skips the header row.
        if (index >= 0) listState.scrollToItem(index + 1)
    }
    val hasSummary = summary.toList().any { !it.isNullOrBlank() }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md),
        contentPadding = PaddingValues(PsTheme.spacing.lg),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header") { SecondaryText(header) }
        items(messages, key = { it.id }) { message ->
            ChatMessageItem(
                text = message.text,
                isUser = message.role == MessageEntity.ROLE_USER,
                correction = message.correction,
            )
        }
        if (hasSummary) {
            item(key = "summary") {
                SummaryCard(summary.first, summary.second, summary.third)
            }
        }
    }
}
