package io.winstonhoyle.practicespeaking.ai

import io.winstonhoyle.practicespeaking.error.TutorError

/** Who said a [Turn]: the learner or the tutor. */
enum class Role { USER, ASSISTANT }

/**
 * One turn of the conversation. Assistant turns carry the provider's raw response in
 * [providerPayload] so the next request can replay it exactly.
 */
data class Turn(val role: Role, val text: String, val providerPayload: String? = null)

/** Tokens one request used, as the provider reported them. [input] includes [cached]; [output] includes any reasoning. */
data class TokenUsage(val input: Long = 0, val output: Long = 0, val cached: Long = 0) {
    operator fun plus(other: TokenUsage) = TokenUsage(input + other.input, output + other.output, cached + other.cached)
}

/** The tutor's answer: the text to show, the raw response to save and replay next turn, and what it cost in tokens. */
data class TutorReply(val text: String, val providerPayload: String?, val usage: TokenUsage? = null)

/**
 * A conversation partner backed by one AI provider. Everything outside `ai/` talks to this
 * interface and never to a vendor SDK.
 */
interface TutorModel {
    val provider: Provider
    val model: String

    /** The next tutor reply. [history] ends with the learner's latest turn. Failures carry a [TutorError] in an `AppException`. */
    suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply>

    /** Checks the key and the model ID without generating any text. */
    suspend fun testKey(): Result<Unit>

    /** IDs of the text models this key can use, from the provider's list endpoint. Generates no text. */
    suspend fun listModels(): Result<List<String>>
}
