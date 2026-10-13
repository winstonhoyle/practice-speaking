package io.winstonhoyle.practicespeaking.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.db.SessionRow
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.ui.AppErrorBanner
import io.winstonhoyle.practicespeaking.ui.ErrorAction
import io.winstonhoyle.practicespeaking.ui.components.EmptyState
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.components.PsButton
import io.winstonhoyle.practicespeaking.ui.components.PsButtonStyle
import io.winstonhoyle.practicespeaking.ui.components.PsIconAction
import io.winstonhoyle.practicespeaking.ui.components.PsIcons
import io.winstonhoyle.practicespeaking.ui.components.PsTopBar
import io.winstonhoyle.practicespeaking.ui.components.SearchField
import io.winstonhoyle.practicespeaking.ui.components.SearchResultItem
import io.winstonhoyle.practicespeaking.ui.components.SessionListItem
import io.winstonhoyle.practicespeaking.ui.components.SwipeToDelete
import io.winstonhoyle.practicespeaking.ui.formatSessionDate
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onOpenSession: (sessionId: Long, messageId: Long?) -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state,
        viewModel::updateQuery,
        viewModel::delete,
        onOpenSession,
        onNewConversation,
        onOpenSettings,
        viewModel::dismissError,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenSession: (sessionId: Long, messageId: Long?) -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<SessionRow?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            PsTopBar(
                title = stringResource(R.string.app_name),
                actions = { PsIconAction(PsIcons.Settings, stringResource(R.string.settings_title), onOpenSettings) },
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(onClick = onNewConversation) {
                Icon(PsIcons.Mic, contentDescription = stringResource(R.string.history_new_conversation))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = PsTheme.spacing.lg, vertical = PsTheme.spacing.sm),
            )
            state.error?.let { error ->
                AppErrorBanner(
                    error = error,
                    onAction = { if (it == ErrorAction.OpenSettings) onOpenSettings() else onDismissError() },
                    modifier = Modifier.padding(horizontal = PsTheme.spacing.lg, vertical = PsTheme.spacing.sm),
                )
            }
            when {
                state.loading -> {
                    LoadingState()
                }

                state.searching -> {
                    SearchResults(state, onOpenSession)
                }

                state.sessions.isEmpty() -> {
                    EmptyState(
                        icon = PsIcons.Conversation,
                        message = stringResource(R.string.history_empty),
                        actionLabel = stringResource(R.string.history_new_conversation),
                        onAction = onNewConversation,
                    )
                }

                else -> {
                    SessionList(state.sessions, onOpen = { onOpenSession(it, null) }, onDelete = { pendingDelete = it })
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_body, session.title)) },
            confirmButton = {
                PsButton(stringResource(R.string.action_delete), onClick = {
                    onDelete(session.id)
                    pendingDelete = null
                }, style = PsButtonStyle.Text)
            },
            dismissButton = {
                PsButton(stringResource(R.string.action_cancel), onClick = { pendingDelete = null }, style = PsButtonStyle.Text)
            },
        )
    }
}

@Composable
private fun SessionList(sessions: List<SessionRow>, onOpen: (Long) -> Unit, onDelete: (SessionRow) -> Unit) {
    LazyColumn {
        items(sessions, key = { it.id }) { session ->
            SwipeToDelete(onDelete = { onDelete(session) }) {
                SessionListItem(
                    language = languageOf(session.language),
                    providerLabel = providerLabel(session.provider),
                    providerLogo = providerOf(session.provider)?.logo,
                    title = session.title,
                    dateText = formatSessionDate(session.startedAt),
                    messageCount = session.messageCount,
                    onClick = { onOpen(session.id) },
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun SearchResults(state: HistoryUiState, onOpenSession: (Long, Long?) -> Unit) {
    if (state.results.isEmpty()) {
        EmptyState(icon = PsIcons.Search, message = stringResource(R.string.search_no_results, state.query))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs)) {
        items(state.results, key = { it.messageId }) { hit ->
            SearchResultItem(
                snippet = listOfNotNull(hit.text, hit.correction?.let(::correctionLine)).joinToString("\n"),
                query = state.query,
                language = languageOf(hit.language),
                sessionTitle = hit.sessionTitle,
                dateText = formatSessionDate(hit.startedAt),
                onClick = { onOpenSession(hit.sessionId, hit.messageId) },
            )
        }
    }
}

/** A stored correction as one readable line; an unparseable one shows as stored. */
private fun correctionLine(raw: String): String = TutorResponseParser.parseCorrection(raw)?.display() ?: raw

internal fun languageOf(name: String): TutorLanguage = TutorLanguage.entries.firstOrNull { it.name == name } ?: TutorLanguage.RUSSIAN

/** The [Provider] a saved session names, or null for one this build doesn't know. */
internal fun providerOf(name: String): Provider? = Provider.entries.firstOrNull { it.name == name }

internal fun providerLabel(name: String): String = providerOf(name)?.shortName ?: name
