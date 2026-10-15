package io.winstonhoyle.practicespeaking.ui.conversation

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.winstonhoyle.practicespeaking.TutorModelBuilder
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.ai.TutorModel
import io.winstonhoyle.practicespeaking.data.MessageTranslator
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.db.cefrLevel
import io.winstonhoyle.practicespeaking.data.db.displayText
import io.winstonhoyle.practicespeaking.data.db.isUser
import io.winstonhoyle.practicespeaking.data.db.providerOrNull
import io.winstonhoyle.practicespeaking.data.db.tutorLanguage
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.SpeechError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.error.appError
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.ParsedReply
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.prompt.Stress
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.speech.Speaker
import io.winstonhoyle.practicespeaking.speech.VoiceStatus
import io.winstonhoyle.practicespeaking.ui.TranslationState
import io.winstonhoyle.practicespeaking.ui.TranslationToggles
import io.winstonhoyle.practicespeaking.ui.components.TutorCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A message as the conversation screen shows it. [text] is the display text, with stress marks
 * when there are any. [correction] is set on learner messages the tutor corrected.
 */
data class ChatMessage(val id: Long, val isUser: Boolean, val text: String, val correction: String? = null)

data class ConversationUiState(
    val loading: Boolean = true,
    val language: TutorLanguage = TutorLanguage.RUSSIAN,
    val provider: Provider = Provider.ANTHROPIC,
    val level: CefrLevel = CefrLevel.DEFAULT,
    val voiceSpeed: VoiceSpeed = VoiceSpeed.DEFAULT,
    val messages: List<ChatMessage> = emptyList(),
    val translations: TranslationState = TranslationState(),
    val partialTranscript: String = "",
    /** What's typed in the text box. Going back to a message puts its text here. */
    val draft: String = "",
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
 * The session is created on the first message, or continued from History, and keeps its
 * provider, model, and level for good.
 *
 * Dependencies come in through the constructor (see `AppContainer`); each argument is a separate
 * app service, and grouping them would only hide what the ViewModel uses. Each public function is
 * one thing the screen can do, so the function count grows with the screen.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ConversationViewModel(
    private val settings: SettingsRepository,
    private val sessions: SessionRepository,
    private val prompts: PromptRenderer,
    private val tutorModels: TutorModelBuilder,
    private val speaker: Speaker,
    private val translator: MessageTranslator,
    /** A saved session to continue instead of starting a new one. */
    private val continueSessionId: Long? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state

    private var tutor: TutorModel? = null
    private var systemPrompt = ""
    private var sessionId: Long? = null

    /** The key [tutor] was built with, so a key changed in Settings is noticed. */
    private var currentKey: String? = null
    private val turns = mutableListOf<Turn>()

    /** The learner message waiting for a reply after a failed request, for [retry]. */
    private var pendingUserMessageId: Long? = null

    private val translations = TranslationToggles(viewModelScope) { error -> _state.update { it.copy(error = error) } }

    init {
        viewModelScope.launch { translations.state.collect { t -> _state.update { it.copy(translations = t) } } }
        viewModelScope.launch { speaker.isSpeaking.collect { speaking -> _state.update { it.copy(isSpeaking = speaking) } } }
        viewModelScope.launch { speaker.voiceStatus.collect { status -> _state.update { it.copy(voiceStatus = status) } } }
        reloadSettings()
    }

    /**
     * Loads what the screen needs, and again on every return from Settings. A new conversation
     * reads Settings. Once a session exists, or one is being continued, its provider, model,
     * language, and level stay fixed; only the key is read again, so a key replaced in Settings
     * takes effect in the same session.
     */
    fun reloadSettings() {
        viewModelScope.launch {
            guarded {
                val existing = (sessionId ?: continueSessionId)?.let { sessions.getSession(it) }
                if (existing != null) loadSession(existing) else loadNew()
            }
        }
    }

    private suspend fun loadNew() {
        val provider = settings.provider.first()
        val language = settings.language.first()
        val level = settings.level.first()
        val speed = settings.voiceSpeed.first()
        val key = settings.apiKey(provider).first()
        val unreadable = key == null && settings.keyUnreadable(provider).first()
        tutor = key?.let { tutorModels(provider, it, settings.model(provider).first()) }
        currentKey = key
        systemPrompt = prompts.render(language, level)
        useVoice(language, speed)
        _state.update {
            it.copy(
                loading = false,
                provider = provider,
                language = language,
                level = level,
                voiceSpeed = speed,
                needsKey = key == null,
                error = if (unreadable) StorageError.KeyUnreadable else null,
            )
        }
    }

    private suspend fun loadSession(session: SessionEntity) {
        val provider = session.providerOrNull ?: Provider.ANTHROPIC
        if (sessionId == null) restore(session)
        val key = settings.apiKey(provider).first()
        if (key != currentKey) {
            tutor = key?.let { tutorModels(provider, it, session.model) }
            currentKey = key
        }
        val unreadable = key == null && settings.keyUnreadable(provider).first()
        _state.update {
            it.copy(
                loading = false,
                provider = provider,
                needsKey = key == null,
                // A new key clears the old key's error; anything else stays until it's dismissed.
                error = if (unreadable) StorageError.KeyUnreadable else it.error.takeUnless { key != null && it == TutorError.InvalidKey },
            )
        }
    }

    /** Continues a saved session: the same prompt, and every turn replayed from its saved payload. */
    private suspend fun restore(session: SessionEntity) {
        val language = session.tutorLanguage
        val level = session.cefrLevel
        val speed = settings.voiceSpeed.first()
        val saved = sessions.getMessages(session.id)
        sessionId = session.id
        systemPrompt = prompts.render(language, level)
        turns.clear()
        saved.forEach { message ->
            turns +=
                if (message.isUser) Turn(Role.USER, message.text) else Turn(Role.ASSISTANT, message.displayText, message.providerPayload)
        }
        translations.remember(saved.mapNotNull { message -> message.translation?.let { message.id to it } }.toMap())
        useVoice(language, speed)
        _state.update { state ->
            state.copy(
                language = language,
                level = level,
                voiceSpeed = speed,
                messages = saved.map { ChatMessage(it.id, isUser = it.isUser, text = it.displayText, correction = it.correction) },
            )
        }
    }

    private fun useVoice(language: TutorLanguage, speed: VoiceSpeed) {
        speaker.setLanguage(language.localeTag)
        speaker.setRate(speed.rate)
    }

    fun onDraftChange(text: String) = _state.update { it.copy(draft = text) }

    /**
     * Goes back to just before a learner message: it and everything after it are deleted, and its
     * text goes into the text box to fix and send, or the learner just says it again.
     */
    fun editFrom(messageId: Long) {
        val current = _state.value
        val session = sessionId?.takeIf { current.canSend && !current.isListening } ?: return
        val index = current.messages.indexOfFirst { it.id == messageId && it.isUser }
        if (index < 0) return
        speaker.stop()
        viewModelScope.launch {
            guarded {
                sessions.deleteMessagesFrom(session, messageId)
                // Every saved message added one turn, so the two lists line up.
                turns.subList(index, turns.size).clear()
                pendingUserMessageId = null
                _state.update {
                    it.copy(messages = it.messages.take(index), draft = Stress.strip(current.messages[index].text), error = null)
                }
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
        when (command) {
            TutorCommand.EndSession -> {
                endSession()
            }

            // The tutor simplifies its words; the voice itself also slows down one step for this session.
            TutorCommand.Slower -> {
                val speed = _state.value.voiceSpeed.slower()
                speaker.setRate(speed.rate)
                _state.update { it.copy(voiceSpeed = speed) }
                send(command.triggerWord)
            }

            else -> {
                send(command.triggerWord)
            }
        }
    }

    /**
     * Shows or hides a message's English translation. The first time, it asks the tutor's model
     * for one with a separate request that never enters the conversation.
     */
    fun toggleTranslation(messageId: Long) {
        val message = _state.value.messages.firstOrNull { it.id == messageId } ?: return
        val model = tutor ?: return
        translations.toggle(messageId, saved = null) { translator.translate(model, messageId, message.text, _state.value.language) }
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

    /** Reads any text aloud: a tutor reply, the learner's own sentence, or a corrected phrase. */
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

    /**
     * Saves what the reply says about the learner's message: the correction and, in Russian, the
     * stressed copy (only when it keeps the learner's words). Returns how the message changes on screen.
     */
    private suspend fun saveLearnerFeedback(userMessageId: Long, parsed: ParsedReply): (ChatMessage) -> ChatMessage {
        val correction = parsed.correction?.format() ?: parsed.rawCorrection
        val userText =
            _state.value.messages
                .firstOrNull { it.id == userMessageId }
                ?.text
                .orEmpty()
        val stressed = parsed.stressed?.let { Stress.matchOrNull(userText, it) }
        if (correction != null) sessions.setCorrection(userMessageId, correction)
        if (stressed != null) sessions.setStressed(userMessageId, stressed)
        return { it.copy(correction = correction ?: it.correction, text = stressed ?: it.text) }
    }

    private suspend fun startSession(): Long {
        val current = _state.value
        val model = tutor ?: error("No tutor model: the screen should have sent the learner to Settings")
        return sessions.startSession(current.language, current.provider, model.model, current.level).also { sessionId = it }
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

        val assistantId = sessions.addAssistantMessage(session, parsed.spokenText, reply.providerPayload)
        turns += Turn(Role.ASSISTANT, parsed.spokenText, reply.providerPayload)
        val learner = saveLearnerFeedback(userMessageId, parsed)

        val ended = endRequested || parsed.summary != null
        if (ended) sessions.endSession(session, parsed.summary)
        _state.update { state ->
            val messages = state.messages.map { if (it.id == userMessageId) learner(it) else it }
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
