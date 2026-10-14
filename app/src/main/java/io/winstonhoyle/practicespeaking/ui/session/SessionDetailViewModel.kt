package io.winstonhoyle.practicespeaking.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.TutorModelBuilder
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.MessageTranslator
import io.winstonhoyle.practicespeaking.data.db.MessageEntity
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.db.providerOrNull
import io.winstonhoyle.practicespeaking.data.db.tutorLanguage
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.speech.Speaker
import io.winstonhoyle.practicespeaking.ui.STOP_TIMEOUT_MILLIS
import io.winstonhoyle.practicespeaking.ui.TranslationState
import io.winstonhoyle.practicespeaking.ui.TranslationToggles
import io.winstonhoyle.practicespeaking.ui.catchDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val loading: Boolean = true,
    val session: SessionEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    /** The session couldn't be read. */
    val error: AppError? = null,
    val translations: TranslationState = TranslationState(),
    /** A translation request failed. */
    val translationError: AppError? = null,
)

/**
 * A saved session, read-only. Messages can be read aloud and translated; a translation uses the
 * key saved for the session's provider and is cached on the message.
 */
class SessionDetailViewModel(
    private val sessions: SessionRepository,
    private val sessionId: Long,
    private val settings: SettingsRepository,
    private val tutorModels: TutorModelBuilder,
    private val translator: MessageTranslator,
    private val speaker: Speaker,
) : ViewModel() {
    private val error = MutableStateFlow<AppError?>(null)
    private val translationError = MutableStateFlow<AppError?>(null)
    private val translations = TranslationToggles(viewModelScope) { translationError.value = it }

    val state: StateFlow<SessionDetailUiState> =
        combine(
            sessions.observeSession(sessionId).catchDatabase(null) { error.value = it },
            sessions.observeMessages(sessionId).catchDatabase(emptyList()) { error.value = it },
            error,
            translations.state,
            translationError,
        ) { session, messages, e, t, te ->
            SessionDetailUiState(
                loading = false,
                session = session,
                messages = messages,
                error = e,
                translations = t,
                translationError = te,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionDetailUiState())

    fun replay(text: String) {
        state.value.session?.let { speaker.setLanguage(it.tutorLanguage.localeTag) }
        viewModelScope.launch {
            speaker.setRate(settings.voiceSpeed.first().rate)
            speaker.speak(TutorResponseParser.forSpeech(text))
        }
    }

    fun stopSpeaking() = speaker.stop()

    fun dismissTranslationError() {
        translationError.value = null
    }

    fun toggleTranslation(messageId: Long) {
        val current = state.value
        val message = current.messages.firstOrNull { it.id == messageId } ?: return
        val session = current.session ?: return
        translations.toggle(messageId, message.translation) {
            val provider = session.providerOrNull
            val key = provider?.let { settings.apiKey(it).first() }
            if (provider == null || key == null) {
                Result.failure(AppException(TutorError.InvalidKey))
            } else {
                translator.translate(tutorModels(provider, key, session.model), messageId, message.text, session.tutorLanguage)
            }
        }
    }

    override fun onCleared() {
        speaker.stop()
    }
}
