package io.winstonhoyle.practicespeaking.ui.conversation

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.TutorModelBuilder
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.ai.TutorModel
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.SpeechError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.error.appError
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.speech.Speaker
import io.winstonhoyle.practicespeaking.speech.VoiceStatus
import io.winstonhoyle.practicespeaking.ui.components.TutorCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A message as the conversation screen shows it. [correction] is set on learner messages the tutor corrected. */
data class ChatMessage(val id: Long, val isUser: Boolean, val text: String, val correction: String? = null)

data class ConversationUiState(
    val loading: Boolean = true,
    val language: TutorLanguage = TutorLanguage.RUSSIAN,
    val provider: Provider = Provider.ANTHROPIC,
    val messages: List<ChatMessage> = emptyList(),
    val partialTranscript: String = "",
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val voiceStatus: VoiceStatus = VoiceStatus.Loading,
    val error: AppError? = null,
    val needsKey: Boolean = false,
    val ended: Boolean = false,
    val summary: Summary? = null,
) {
    /** No voice data for this language: a standing hint, separate from [error]. */
    val voiceMissing: Boolean get() = voiceStatus == VoiceStatus.MissingData

    val learnerTurns: Int get() = messages.count { it.isUser }
    val showLongSessionBanner: Boolean get() = learnerTurns >= LONG_SESSION_TURNS && !ended
    val canSend: Boolean get() = !loading && !needsKey && !isThinking && !ended

    companion object {
        /** History is never trimmed, so long sessions get a nudge to start fresh instead. */
        const val LONG_SESSION_TURNS = 60
    }
}

/**
 * Runs one conversation: save the learner's message, ask the [TutorModel], parse the reply,
 * save it with its correction and raw payload, then speak it. Works the same for every provider.
 * The session is created on the first message and keeps its provider and model until it ends.
 */
class ConversationViewModel(
    private val settings: SettingsRepository,
    private val sessions: SessionRepository,
    private val prompts: PromptRenderer,
    private val tutorModels: TutorModelBuilder,
    private val speaker: Speaker,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state

    private var tutor: TutorModel? = null
    private var systemPrompt = ""
    private var sessionId: Long? = null
    private val turns = mutableListOf<Turn>()

    /** The learner message waiting for a reply after a failed request, for [retry]. */
    private var pendingUserMessageId: Long? = null

    init {
        viewModelScope.launch { speaker.isSpeaking.collect { speaking -> _state.update { it.copy(isSpeaking = speaking) } } }
        viewModelScope.launch { speaker.voiceStatus.collect { status -> _state.update { it.copy(voiceStatus = status) } } }
        reloadSettings()
    }

    /** Picks up changes made in Settings. Ignored once the session has started. */
    fun reloadSettings() {
        if (sessionId != null) return
        viewModelScope.launch {
            val provider = settings.provider.first()
            val language = settings.language.first()
            val key = settings.apiKey(provider).first()
            val unreadable = key == null && settings.keyUnreadable(provider).first()
            tutor = key?.let { tutorModels(provider, it, settings.model(provider).first()) }
            systemPrompt = prompts.render(language)
            speaker.setLanguage(language.localeTag)
            _state.update {
                it.copy(
                    loading = false,
                    provider = provider,
                    language = language,
                    needsKey = key == null,
                    error = if (unreadable) StorageError.KeyUnreadable else null,
                )
            }
        }
    }

    fun onListenStart() {
        speaker.stop()
        _state.update { it.copy(isListening = true, partialTranscript = "") }
    }

    fun onPartialTranscript(text: String) = _state.update { it.copy(partialTranscript = text) }

    fun onListenResult(text: String) {
        _state.update { it.copy(isListening = false, partialTranscript = "") }
        send(text)
    }

    /** Recognition ended without text, or the mic permission was refused. */
    fun onListenFailed(error: SpeechError) {
        _state.update { it.copy(isListening = false, partialTranscript = "", error = error) }
    }

    fun onCommand(command: TutorCommand) {
        if (command == TutorCommand.EndSession) endSession() else send(command.triggerWord)
    }

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || !_state.value.canSend) return
        speaker.stop()
        _state.update { it.copy(isThinking = true, error = null) }
        viewModelScope.launch {
            guarded {
                val session = sessionId ?: startSession()
                val id = sessions.addUserMessage(session, message)
                turns += Turn(Role.USER, message)
                _state.update { it.copy(messages = it.messages + ChatMessage(id, isUser = true, text = message)) }
                requestReply(session, id, endRequested = message == TutorCommand.EndSession.triggerWord)
            }
        }
    }

    fun retry() {
        val userMessageId = pendingUserMessageId ?: return
        val session = sessionId ?: return
        _state.update { it.copy(isThinking = true, error = null) }
        viewModelScope.launch {
            guarded { requestReply(session, userMessageId, endRequested = turns.lastOrNull()?.text == TutorCommand.EndSession.triggerWord) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    /** Asks the tutor for the summary. With nothing said yet there is nothing to summarize, so it just ends. */
    fun endSession() {
        if (sessionId == null) {
            _state.update { it.copy(ended = true) }
        } else {
            send(TutorCommand.EndSession.triggerWord)
        }
    }

    fun replay(text: String) = speaker.speak(TutorResponseParser.forSpeech(text))

    fun stopSpeaking() = speaker.stop()

    override fun onCleared() {
        speaker.stop()
    }

    /** Runs database work; a storage failure becomes an error banner instead of a crash. */
    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: SQLException) {
            _state.update { it.copy(isThinking = false, error = StorageError.DatabaseFailed(e.message.orEmpty())) }
        }
    }

    private suspend fun startSession(): Long {
        val current = _state.value
        val model = tutor ?: error("No tutor model: the screen should have sent the learner to Settings")
        return sessions.startSession(current.language, current.provider, model.model).also { sessionId = it }
    }

    private suspend fun requestReply(session: Long, userMessageId: Long, endRequested: Boolean) {
        val model = tutor ?: return
        val result = model.reply(systemPrompt, turns.toList())
        val reply = result.getOrNull()
        // A reply with neither text nor a summary is treated as a failure, so it isn't saved or spoken.
        val parsed = reply?.let { TutorResponseParser.parse(it.text) }?.takeIf { it.spokenText.isNotBlank() || it.summary != null }
        if (reply == null || parsed == null) {
            pendingUserMessageId = userMessageId
            _state.update { it.copy(isThinking = false, error = result.appError() ?: TutorError.EmptyReply) }
            return
        }
        pendingUserMessageId = null

        val correction = parsed.correction?.format() ?: parsed.rawCorrection
        val assistantId = sessions.addAssistantMessage(session, parsed.spokenText, reply.providerPayload)
        turns += Turn(Role.ASSISTANT, parsed.spokenText, reply.providerPayload)
        if (correction != null) sessions.setCorrection(userMessageId, correction)

        val ended = endRequested || parsed.summary != null
        if (ended) sessions.endSession(session, parsed.summary)
        _state.update { state ->
            val messages = state.messages.map { if (it.id == userMessageId && correction != null) it.copy(correction = correction) else it }
            state.copy(
                messages = messages + ChatMessage(assistantId, isUser = false, text = parsed.spokenText),
                isThinking = false,
                ended = ended,
                summary = parsed.summary,
            )
        }
        speaker.speak(parsed.speechText)
    }
}

/** The English trigger words from the tutor prompt. They work the same in every language and with every provider. */
val TutorCommand.triggerWord: String
    get() =
        when (this) {
            TutorCommand.Slower -> "slower"
            TutorCommand.Harder -> "harder"
            TutorCommand.Translate -> "translate"
            TutorCommand.EndSession -> "end session"
        }
