package io.winstonhoyle.practicespeaking.data

import android.database.SQLException
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.ai.TutorModel
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.prompt.Stress
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage

/**
 * Translates one saved message into English with a single request of its own, outside the
 * conversation history, and caches the result on the message.
 */
class MessageTranslator(private val sessions: SessionRepository, private val prompts: PromptRenderer) {
    suspend fun translate(tutor: TutorModel, messageId: Long, text: String, language: TutorLanguage): Result<String> {
        val reply =
            tutor
                .reply(prompts.translationPrompt(language), listOf(Turn(Role.USER, Stress.strip(text))))
                .getOrElse { return Result.failure(it) }
        val translation = reply.text.trim().ifEmpty { return Result.failure(AppException(TutorError.EmptyReply)) }
        // The translation is still worth showing if caching it fails; it is just requested again next time.
        try {
            sessions.setTranslation(messageId, translation)
        } catch (_: SQLException) {
        }
        return Result.success(translation)
    }
}
