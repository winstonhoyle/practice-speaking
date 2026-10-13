package io.winstonhoyle.practicespeaking.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.data.db.MessageEntity
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.ui.STOP_TIMEOUT_MILLIS
import io.winstonhoyle.practicespeaking.ui.catchDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionDetailUiState(
    val loading: Boolean = true,
    val session: SessionEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    /** The session couldn't be read. */
    val error: AppError? = null,
)

/** A saved session, read-only. */
class SessionDetailViewModel(sessions: SessionRepository, sessionId: Long) : ViewModel() {
    private val error = MutableStateFlow<AppError?>(null)

    val state: StateFlow<SessionDetailUiState> =
        combine(
            sessions.observeSession(sessionId).catchDatabase(null) { error.value = it },
            sessions.observeMessages(sessionId).catchDatabase(emptyList()) { error.value = it },
            error,
        ) { session, messages, e ->
            SessionDetailUiState(loading = false, session = session, messages = messages, error = e)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionDetailUiState())
}
