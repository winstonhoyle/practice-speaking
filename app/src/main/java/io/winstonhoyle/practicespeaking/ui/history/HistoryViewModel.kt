package io.winstonhoyle.practicespeaking.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.data.db.SearchHit
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.db.SessionRow
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.ui.STOP_TIMEOUT_MILLIS
import io.winstonhoyle.practicespeaking.ui.catchDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val sessions: List<SessionRow> = emptyList(),
    val query: String = "",
    val results: List<SearchHit> = emptyList(),
    /** A failed read, search, or delete, shown as a banner until dismissed. */
    val error: AppError? = null,
) {
    val searching: Boolean get() = query.isNotBlank()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val sessions: SessionRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val error = MutableStateFlow<AppError?>(null)

    private val results =
        query
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { sessions.search(it).catchDatabase(emptyList()) { e -> error.value = e } }

    val state: StateFlow<HistoryUiState> =
        combine(sessions.observeSessions().catchDatabase(emptyList()) { error.value = it }, query, results, error) { rows, q, hits, e ->
            HistoryUiState(loading = false, sessions = rows, query = q, results = if (q.isBlank()) emptyList() else hits, error = e)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun updateQuery(text: String) {
        query.value = text
    }

    fun delete(sessionId: Long) {
        viewModelScope.launch {
            try {
                sessions.deleteSession(sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (
                // Room reports every SQLite failure as some RuntimeException; any of them means the delete didn't happen.
                @Suppress("TooGenericExceptionCaught") e: RuntimeException,
            ) {
                error.value = StorageError.DatabaseFailed(e.message.orEmpty())
            }
        }
    }

    fun dismissError() {
        error.value = null
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
