package io.winstonhoyle.practicespeaking.data.db

import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Saves conversations and reads them back for History, detail, and search. */
class SessionRepository(private val dao: SessionDao, private val clock: () -> Long = System::currentTimeMillis) {
    /** Records which provider and model the session uses; it keeps them until it ends. */
    suspend fun startSession(language: TutorLanguage, provider: Provider, model: String): Long =
        dao.insertSession(SessionEntity(language = language.name, provider = provider.name, model = model, startedAt = clock()))

    /** Saves a learner message. The first one also becomes the session title. */
    suspend fun addUserMessage(sessionId: Long, text: String): Long {
        val id = dao.insertMessage(MessageEntity(sessionId = sessionId, role = MessageEntity.ROLE_USER, text = text, createdAt = clock()))
        dao.setTitleIfEmpty(sessionId, titleFrom(text))
        return id
    }

    /** Saves a tutor reply with the provider's raw response, which the next turn replays. */
    suspend fun addAssistantMessage(sessionId: Long, text: String, providerPayload: String?): Long =
        dao.insertMessage(
            MessageEntity(
                sessionId = sessionId,
                role = MessageEntity.ROLE_ASSISTANT,
                text = text,
                providerPayload = providerPayload,
                createdAt = clock(),
            ),
        )

    /** Attaches the tutor's correction to the learner message it corrects. */
    suspend fun setCorrection(userMessageId: Long, correction: String) = dao.setCorrection(userMessageId, correction)

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
