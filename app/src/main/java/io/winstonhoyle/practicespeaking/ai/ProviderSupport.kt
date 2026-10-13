package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Plumbing shared by the vendor classes, so each one only says what is specific to its API.

/** Shared limits for every provider. */
internal object TutorLimits {
    const val MAX_OUTPUT_TOKENS = 1024L
}

/**
 * Runs a blocking SDK or HTTP call on [Dispatchers.IO] and turns any failure into a
 * [TutorError]. [classify] maps the vendor's own exceptions; anything it doesn't recognize
 * becomes [TutorError.Unknown]. Cancellation is rethrown, never reported as an error.
 */
internal suspend fun <T> providerCall(classify: (Exception) -> TutorError?, block: () -> T): Result<T> =
    withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AppException) {
            Result.failure(e)
        } catch (
            // The provider boundary: whatever an SDK throws must come back as a TutorError, never a crash.
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(AppException(classify(e) ?: TutorError.Unknown(e.message.orEmpty()), e))
        }
    }

/** Maps an HTTP status (and, for 400/413, the error message) to a [TutorError]. */
internal fun errorForStatus(status: Int, message: String?): TutorError =
    when {
        status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN -> TutorError.InvalidKey
        status == HttpStatus.NOT_FOUND -> TutorError.ModelNotFound
        status == HttpStatus.TOO_MANY_REQUESTS -> TutorError.RateLimited
        status in HttpStatus.SERVER_ERRORS -> TutorError.ProviderDown
        (status == HttpStatus.BAD_REQUEST || status == HttpStatus.TOO_LARGE) && isContextTooLong(message) -> TutorError.ContextTooLong
        else -> TutorError.Unknown(message ?: "HTTP $status")
    }

internal object HttpStatus {
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val TOO_LARGE = 413
    const val TOO_MANY_REQUESTS = 429
    private const val FIRST_SERVER_ERROR = 500
    private const val LAST_SERVER_ERROR = 599
    val SERVER_ERRORS = FIRST_SERVER_ERROR..LAST_SERVER_ERROR
}

/** Each vendor words "the conversation is too long" differently; these cover Anthropic, OpenAI, and Gemini. */
private val CONTEXT_TOO_LONG =
    Regex(
        "prompt is too long|context_length_exceeded|maximum context length|context window|exceeds the maximum number of tokens",
        RegexOption.IGNORE_CASE,
    )

internal fun isContextTooLong(message: String?): Boolean = message != null && CONTEXT_TOO_LONG.containsMatchIn(message)

/**
 * Saves SDK model objects as JSON strings and reads them back, through the SDK's own
 * (internal) Jackson setup. Each SDK exposes it only via its `JsonValue` type, so the vendor
 * passes in the two conversions.
 */
internal class SdkJson(private val toTree: (Any) -> JsonNode, private val fromTree: (JsonNode, TypeReference<Any>) -> Any?) {
    fun write(value: Any): String = mapper.writeValueAsString(toTree(value))

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> read(json: String, type: TypeReference<T>): T? = fromTree(mapper.readTree(json), type as TypeReference<Any>) as T?

    companion object {
        val mapper = ObjectMapper()
    }
}
