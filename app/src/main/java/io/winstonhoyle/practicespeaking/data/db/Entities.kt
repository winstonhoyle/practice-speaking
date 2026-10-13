package io.winstonhoyle.practicespeaking.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [io.winstonhoyle.practicespeaking.prompt.TutorLanguage] name. */
    val language: String,
    /** [io.winstonhoyle.practicespeaking.ai.Provider] name. */
    val provider: String,
    /** The exact model ID used. */
    val model: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    /** The first learner message, truncated. Empty until the learner says something. */
    val title: String = "",
    val summaryMistakes: String? = null,
    val summaryNewWords: String? = null,
    val summaryTip: String? = null,
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** "user" or "assistant". */
    val role: String,
    /** What was said, without the CORRECTION line. */
    val text: String,
    /** On a learner message: the tutor's correction of it, as `wrong → right | explanation`. */
    val correction: String? = null,
    /** On a tutor message: the provider's raw response, replayed as-is on the next turn. */
    val providerPayload: String? = null,
    val createdAt: Long,
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

/** Full-text index over messages. `unicode61` folds case for Cyrillic and accented letters too. */
@Fts4(contentEntity = MessageEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "messages_fts")
data class MessageFts(val text: String, val correction: String?)

/** A session row for History, with its message count. */
data class SessionRow(
    val id: Long,
    val language: String,
    val provider: String,
    val model: String,
    val startedAt: Long,
    val endedAt: Long?,
    val title: String,
    val messageCount: Int,
)

/** One search result: the matching message and the session it belongs to. */
data class SearchHit(
    val messageId: Long,
    val sessionId: Long,
    val text: String,
    val correction: String?,
    val language: String,
    val sessionTitle: String,
    val startedAt: Long,
)
