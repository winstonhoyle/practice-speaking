package io.winstonhoyle.practicespeaking.data.db

import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.Stress
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/** Saves conversations and reads them back for History, detail, and search. */
class SessionRepository(private val dao: SessionDao, private val clock: () -> Long = System::currentTimeMillis) {
    /** Records which provider, model, and level the session uses; it keeps them for good, Continue included. */
    suspend fun startSession(language: TutorLanguage, provider: Provider, model: String, level: CefrLevel = CefrLevel.DEFAULT): Long =
        dao.insertSession(
            SessionEntity(language = language.name, provider = provider.name, model = model, startedAt = clock(), level = level.name),
        )

    /** One session, read once; null when it doesn't exist. */
    suspend fun getSession(sessionId: Long): SessionEntity? = dao.observeSession(sessionId).first()

    /** A session's messages, read once, in order. */
    suspend fun getMessages(sessionId: Long): List<MessageEntity> = dao.observeMessages(sessionId).first()

    /** Saves a learner message. The first one also becomes the session title. */
    suspend fun addUserMessage(sessionId: Long, text: String): Long {
        val id = dao.insertMessage(MessageEntity(sessionId = sessionId, role = MessageEntity.ROLE_USER, text = text, createdAt = clock()))
        dao.setTitleIfEmpty(sessionId, titleFrom(text))
        return id
    }

    /**
     * Saves a tutor reply with the provider's raw response, which the next turn replays. Stress
     * marks are kept for display and left out of the searchable text.
     */
    suspend fun addAssistantMessage(sessionId: Long, text: String, providerPayload: String?): Long =
        dao.insertMessage(
            MessageEntity(
                sessionId = sessionId,
                role = MessageEntity.ROLE_ASSISTANT,
                text = Stress.strip(text),
                providerPayload = providerPayload,
                createdAt = clock(),
                stressed = text.takeIf(Stress::hasMarks),
            ),
        )

    /** Attaches the tutor's correction to the learner message it corrects. */
    suspend fun setCorrection(userMessageId: Long, correction: String) = dao.setCorrection(userMessageId, correction)

    /** Stores the learner's message with stress marks, as the tutor returned it. */
    suspend fun setStressed(userMessageId: Long, stressed: String) = dao.setStressed(userMessageId, stressed)

    /** Caches a message's English translation, so it is only requested once. */
    suspend fun setTranslation(messageId: Long, translation: String) = dao.setTranslation(messageId, translation)

    /** Marks the session ended and stores its summary. A missing or unparsed summary is saved as empty. */
    suspend fun endSession(sessionId: Long, summary: Summary?) =
        dao.endSession(sessionId, clock(), summary?.mistakes, summary?.newWords, summary?.tip)

    /** Deletes a session; its messages go with it (cascade). */
    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    /** Sessions for History, newest first. Sessions without messages are left out. */
    fun observeSessions(): Flow<List<SessionRow>> = dao.observeSessions()

    /** One session's details, or null once it's deleted. */
    fun observeSession(sessionId: Long): Flow<SessionEntity?> = dao.observeSession(sessionId)

    /** A session's messages in the order they were said. */
    fun observeMessages(sessionId: Long): Flow<List<MessageEntity>> = dao.observeMessages(sessionId)

    /** Prefix search over message text and corrections. Blank or symbol-only queries match nothing. */
    fun search(query: String): Flow<List<SearchHit>> {
        val match = ftsQuery(query) ?: return flowOf(emptyList())
        return dao.search(match)
    }

    companion object {
        private const val TITLE_LENGTH = 60

        fun titleFrom(text: String): String {
            val line = text.trim().lineSequence().first()
            return if (line.length <= TITLE_LENGTH) line else line.take(TITLE_LENGTH - 1).trimEnd() + "…"
        }

        /** Turns free text into a safe FTS query: each word becomes a prefix term, so "магазин" finds "магазине". */
        fun ftsQuery(query: String): String? {
            val words = query.split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotBlank() }
            if (words.isEmpty()) return null
            return words.joinToString(" ") { "$it*" }
        }
    }
}
