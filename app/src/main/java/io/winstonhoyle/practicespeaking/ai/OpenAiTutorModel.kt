package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.errors.BadRequestException
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
 * including encrypted reasoning. Older chat models reject the reasoning settings; the first
 * such rejection turns them off for this model and the request is sent again without them.
 */
class OpenAiTutorModel internal constructor(override val model: String, private val client: OpenAIClient) : TutorModel {
    constructor(apiKey: String, model: String) : this(
        model,
        OpenAIOkHttpClient.builder().apiKey(apiKey).build(),
    )

    override val provider = Provider.OPENAI

    @Volatile private var reasoning = true

    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> =
        call {
            val input = history.flatMap(::toInputItems)
            val response =
                try {
                    client.responses().create(params(systemPrompt, input))
                } catch (e: BadRequestException) {
                    if (!reasoning || !UNSUPPORTED_REASONING.containsMatchIn(e.message.orEmpty())) throw e
                    reasoning = false
                    client.responses().create(params(systemPrompt, input))
                }
            val tokens =
                response
                    .usage()
                    .map {
                        usageOf(
                            provider,
                            it.inputTokens(),
                            it.outputTokens(),
                            it.inputTokensDetails().cachedTokens(),
                        )
                    }.orElse(null)
            if (isRefusal(response)) throw AppException(TutorError.Refused)

            val text =
                response
                    .output()
                    .filter { it.isMessage() }
                    .flatMap { it.asMessage().content() }
                    .filter { it.isOutputText() }
                    .joinToString("") { it.asOutputText().text() }
            TutorReply(text, json.write(response.output()), tokens)
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

    private fun params(systemPrompt: String, input: List<ResponseInputItem>): ResponseCreateParams =
        ResponseCreateParams
            .builder()
            .model(model)
            .instructions(systemPrompt)
            .inputOfResponse(input)
            .maxOutputTokens(TutorLimits.MAX_OUTPUT_TOKENS)
            .store(false)
            .apply {
                if (reasoning) {
                    reasoning(Reasoning.builder().effort(ReasoningEffort.LOW).build())
                    addInclude(ResponseIncludable.REASONING_ENCRYPTED_CONTENT)
                }
            }.build()

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

        /** OpenAI's 400 for a reasoning setting on a model without reasoning, e.g. "Unsupported parameter: 'reasoning.effort'". */
        val UNSUPPORTED_REASONING =
            Regex("reasoning[^\\n]*(not supported|unsupported)|unsupported[^\\n]*reasoning", RegexOption.IGNORE_CASE)

        /** GPT and o-series models answer text. */
        val CHAT_FAMILY = Regex("^(gpt-|chatgpt-|o\\d)")

        /** Family members that don't: speech, images, search, and older completion-only models. */
        val NOT_CHAT = Regex("audio|realtime|tts|transcribe|image|search|embedding|instruct|moderation")
    }
}
