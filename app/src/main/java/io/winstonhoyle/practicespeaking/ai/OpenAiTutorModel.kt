package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.errors.OpenAIIoException
import com.openai.errors.OpenAIServiceException
import com.openai.models.Reasoning
import com.openai.models.ReasoningEffort
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseIncludable
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.ResponseOutputItem
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError

/**
 * OpenAI through the official Java SDK and the Responses API. Requests are stateless
 * (`store = false`), so each assistant turn is replayed from its saved output items,
 * including encrypted reasoning.
 */
class OpenAiTutorModel internal constructor(override val model: String, private val client: OpenAIClient) : TutorModel {
    constructor(apiKey: String, model: String) : this(
        model,
        OpenAIOkHttpClient.builder().apiKey(apiKey).build(),
    )

    override val provider = Provider.OPENAI

    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> =
        call {
            val params =
                ResponseCreateParams
                    .builder()
                    .model(model)
                    .instructions(systemPrompt)
                    .inputOfResponse(history.flatMap(::toInputItems))
                    .maxOutputTokens(TutorLimits.MAX_OUTPUT_TOKENS)
                    .reasoning(Reasoning.builder().effort(ReasoningEffort.LOW).build())
                    .store(false)
                    .addInclude(ResponseIncludable.REASONING_ENCRYPTED_CONTENT)
                    .build()

            val response = client.responses().create(params)
            response.usage().ifPresent {
                logUsage(provider, it.inputTokens(), it.outputTokens(), it.inputTokensDetails().cachedTokens())
            }
            if (isRefusal(response)) throw AppException(TutorError.Refused)

            val text =
                response
                    .output()
                    .filter { it.isMessage() }
                    .flatMap { it.asMessage().content() }
                    .filter { it.isOutputText() }
                    .joinToString("") { it.asOutputText().text() }
            TutorReply(text, json.write(response.output()))
        }

    override suspend fun testKey(): Result<Unit> = call { client.models().retrieve(model) }

    /** The list mixes chat, audio, image, and embedding models; keep the chat ones, newest first. */
    override suspend fun listModels(): Result<List<String>> =
        call {
            client
                .models()
                .list()
                .autoPager()
                .filter { isChatModel(it.id()) }
                .sortedByDescending { it.created() }
                .map { it.id() }
        }

    private fun isChatModel(id: String): Boolean = CHAT_FAMILY.containsMatchIn(id) && !NOT_CHAT.containsMatchIn(id)

    private fun isRefusal(response: Response): Boolean {
        val filtered =
            response.incompleteDetails().flatMap { it.reason() }.orElse(null) ==
                Response.IncompleteDetails.Reason.CONTENT_FILTER
        val refused = response.output().any { item -> item.isMessage() && item.asMessage().content().any { it.isRefusal() } }
        return filtered || refused
    }

    private fun toInputItems(turn: Turn): List<ResponseInputItem> {
        val saved = turn.providerPayload?.takeIf { turn.role == Role.ASSISTANT }?.let { json.read(it, outputListType) }
        if (saved != null) {
            return saved.mapNotNull { item ->
                when {
                    item.isMessage() -> ResponseInputItem.ofResponseOutputMessage(item.asMessage())
                    item.isReasoning() -> ResponseInputItem.ofReasoning(item.asReasoning())
                    else -> null
                }
            }
        }
        val role = if (turn.role == Role.USER) EasyInputMessage.Role.USER else EasyInputMessage.Role.ASSISTANT
        return listOf(
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage
                    .builder()
                    .role(role)
                    .content(turn.text)
                    .build(),
            ),
        )
    }

    private suspend fun <T> call(block: () -> T): Result<T> = providerCall(::classify, block)

    private fun classify(e: Exception): TutorError? =
        when (e) {
            is OpenAIServiceException -> errorForStatus(e.statusCode(), e.message)
            is OpenAIIoException -> TutorError.Offline
            else -> null
        }

    private companion object {
        val json =
            SdkJson({ JsonValue.from(it).convert(JsonNode::class.java)!! }, { node, type -> JsonValue.fromJsonNode(node).convert(type) })
        val outputListType = object : TypeReference<List<ResponseOutputItem>>() {}

        /** GPT and o-series models answer text. */
        val CHAT_FAMILY = Regex("^(gpt-|chatgpt-|o\\d)")

        /** Family members that don't: speech, images, search, and older completion-only models. */
        val NOT_CHAT = Regex("audio|realtime|tts|transcribe|image|search|embedding|instruct|moderation")
    }
}
