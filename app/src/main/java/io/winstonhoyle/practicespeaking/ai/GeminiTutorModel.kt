package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Gemini over plain REST (`generateContent`), using the OkHttp and Jackson the other SDKs
 * already bring in. Model turns are replayed from the saved `content` object, which keeps any
 * thought signatures Gemini attached.
 */
class GeminiTutorModel internal constructor(
    private val apiKey: String,
    override val model: String,
    private val baseUrl: String,
    private val http: OkHttpClient,
) : TutorModel {
    constructor(apiKey: String, model: String) : this(apiKey, model, BASE_URL, OkHttpClient())

    override val provider = Provider.GEMINI

    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> =
        call {
            val body =
                mapper.createObjectNode().apply {
                    putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt)
                    val contents = putArray("contents")
                    history.forEach { contents.add(toContent(it)) }
                    putObject("generationConfig").put("maxOutputTokens", TutorLimits.MAX_OUTPUT_TOKENS)
                }
            val json =
                execute(
                    Request
                        .Builder()
                        .url(modelUrl(":generateContent"))
                        .post(mapper.writeValueAsString(body).toRequestBody(JSON)),
                )

            val usage = json.path("usageMetadata")
            val tokens =
                usageOf(
                    provider,
                    usage.path("promptTokenCount").asLong(),
                    usage.path("candidatesTokenCount").asLong() + usage.path("thoughtsTokenCount").asLong(),
                    usage.path("cachedContentTokenCount").asLong(),
                )
            if (json.path("promptFeedback").has("blockReason")) throw AppException(TutorError.Refused)
            val candidate = json.path("candidates").path(0)
            if (candidate.path("finishReason").asText() in BLOCKED) throw AppException(TutorError.Refused)

            val content = candidate.path("content")
            val text =
                content
                    .path("parts")
                    .filter { !it.path("thought").asBoolean(false) && it.has("text") }
                    .joinToString("") { it.path("text").asText() }
            TutorReply(text, mapper.writeValueAsString(content), tokens)
        }

    override suspend fun testKey(): Result<Unit> =
        call {
            execute(Request.Builder().url(modelUrl()).get())
            Unit
        }

    /** Models that support `generateContent`, without the embedding, image, speech, and live-audio ones. */
    override suspend fun listModels(): Result<List<String>> =
        call {
            val ids = mutableListOf<String>()
            var pageToken: String? = null
            do {
                val url =
                    modelsUrl()
                        .addQueryParameter("pageSize", PAGE_SIZE.toString())
                        .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                        .build()
                val page = execute(Request.Builder().url(url).get())
                page.path("models").forEach { m ->
                    val id = m.path("name").asText().removePrefix("models/")
                    val generates = m.path("supportedGenerationMethods").any { it.asText() == "generateContent" }
                    if (generates && !NOT_TEXT.containsMatchIn(id)) ids += id
                }
                pageToken = page.path("nextPageToken").asText().ifEmpty { null }
            } while (pageToken != null)
            ids
        }

    // The model ID is typed by the user, so it is escaped as one path segment rather than pasted into the URL.
    private fun modelUrl(suffix: String = "") = modelsUrl().addPathSegment(model + suffix).build()

    private fun modelsUrl() = baseUrl.toHttpUrl().newBuilder().addPathSegment("models")

    private fun toContent(turn: Turn): JsonNode {
        if (turn.role == Role.ASSISTANT && turn.providerPayload != null) return mapper.readTree(turn.providerPayload)
        return mapper.createObjectNode().apply {
            put("role", if (turn.role == Role.USER) "user" else "model")
            putArray("parts").addObject().put("text", turn.text)
        }
    }

    private fun execute(request: Request.Builder): JsonNode {
        val response = http.newCall(request.header("x-goog-api-key", apiKey).build()).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw AppException(errorFor(it.code, body))
            return mapper.readTree(body)
        }
    }

    /** Gemini reports a bad key as 400 `API_KEY_INVALID`, so check the body as well as the status. */
    private fun errorFor(status: Int, body: String): TutorError {
        val error = runCatching { mapper.readTree(body).path("error") }.getOrNull()
        val reasons = error?.path("details")?.map { it.path("reason").asText() }.orEmpty()
        if (status == HttpStatus.BAD_REQUEST && "API_KEY_INVALID" in reasons) return TutorError.InvalidKey
        return errorForStatus(status, error?.path("message")?.asText()?.ifBlank { null })
    }

    private suspend fun <T> call(block: () -> T): Result<T> = providerCall(::classify, block)

    private fun classify(e: Exception): TutorError? =
        when (e) {
            is JsonProcessingException -> TutorError.Unknown("Unexpected response from Gemini")
            is IOException -> TutorError.Offline
            else -> null
        }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        val JSON = "application/json".toMediaType()
        val BLOCKED = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION", "IMAGE_SAFETY")
        val mapper = SdkJson.mapper
        const val PAGE_SIZE = 1000
        val NOT_TEXT = Regex("embedding|image|tts|audio|live|aqa")
    }
}
