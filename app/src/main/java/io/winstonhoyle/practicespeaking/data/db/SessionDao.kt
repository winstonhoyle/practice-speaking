package io.winstonhoyle.practicespeaking.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// One query per method is how Room DAOs are written; splitting it would only scatter the SQL.
@Suppress("TooManyFunctions")
@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE sessions SET title = :title WHERE id = :sessionId AND title = ''")
    suspend fun setTitleIfEmpty(sessionId: Long, title: String)

    @Query("UPDATE messages SET correction = :correction WHERE id = :messageId")
    suspend fun setCorrection(messageId: Long, correction: String)

    @Query("UPDATE messages SET stressed = :stressed WHERE id = :messageId")
    suspend fun setStressed(messageId: Long, stressed: String)

    @Query("UPDATE messages SET translation = :translation WHERE id = :messageId")
    suspend fun setTranslation(messageId: Long, translation: String)

    @Query(
        """UPDATE sessions SET endedAt = :endedAt, summaryMistakes = :mistakes, summaryNewWords = :newWords,
        summaryTip = :tip WHERE id = :sessionId""",
    )
    suspend fun endSession(sessionId: Long, endedAt: Long, mistakes: String?, newWords: String?, tip: String?)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    /** Sessions with at least one message, newest first. */
    @Query(
        """SELECT s.id, s.language, s.provider, s.model, s.startedAt, s.endedAt, s.title,
        (SELECT COUNT(*) FROM messages m WHERE m.sessionId = s.id) AS messageCount
        FROM sessions s WHERE messageCount > 0 ORDER BY s.startedAt DESC""",
    )
    fun observeSessions(): Flow<List<SessionRow>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY id")
    fun observeMessages(sessionId: Long): Flow<List<MessageEntity>>

    @Query(
        """SELECT m.id AS messageId, m.sessionId, m.text, m.correction, s.language, s.title AS sessionTitle, s.startedAt
        FROM messages_fts JOIN messages m ON m.id = messages_fts.rowid JOIN sessions s ON s.id = m.sessionId
        WHERE messages_fts MATCH :query ORDER BY s.startedAt DESC, m.id""",
    )
    fun search(query: String): Flow<List<SearchHit>>
}
