package io.winstonhoyle.practicespeaking.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.enumByName
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage

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
    /** [io.winstonhoyle.practicespeaking.prompt.CefrLevel] name, so Continue rebuilds the same prompt. */
    val level: String? = null,
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
    /** What was said, without the CORRECTION line and without stress marks (it feeds the search index). */
    val text: String,
    /** On a learner message: the tutor's correction of it, as `wrong → right | explanation`. */
    val correction: String? = null,
    /** On a tutor message: the provider's raw response, replayed as-is on the next turn. */
    val providerPayload: String? = null,
    val createdAt: Long,
    /** [text] with Russian stress marks, for display. Null when there are none. */
    val stressed: String? = null,
    /** The English translation, once the learner has asked for it. */
    val translation: String? = null,
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

/** The session's practice language; sessions from an unknown build fall back to Russian. */
val SessionEntity.tutorLanguage: TutorLanguage get() = enumByName<TutorLanguage>(language) ?: TutorLanguage.RUSSIAN

/** The session's provider, or null when this build doesn't know it (it can then be read but not continued). */
val SessionEntity.providerOrNull: Provider? get() = enumByName<Provider>(provider)

/** The level the session started at, or the default if it's missing. */
val SessionEntity.cefrLevel: CefrLevel get() = enumByName<CefrLevel>(level) ?: CefrLevel.DEFAULT

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
