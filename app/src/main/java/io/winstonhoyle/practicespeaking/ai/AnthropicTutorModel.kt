package io.winstonhoyle.practicespeaking.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError

/**
 * Claude through the official Java SDK. The system prompt and the growing history are both
 * cached, and each assistant turn is replayed from its full `content` (thinking included).
 * Low effort is sent only to models whose metadata says they support it; older models reject
 * the parameter. [lowEffort] starts unknown and is looked up once, before the first reply.
 */
class AnthropicTutorModel internal constructor(
    override val model: String,
    private val client: AnthropicClient,
    @Volatile private var lowEffort: Boolean? = null,
) : TutorModel {
    constructor(apiKey: String, model: String) : this(
        model,
        AnthropicOkHttpClient.builder().apiKey(apiKey).build(),
    )

    override val provider = Provider.ANTHROPIC

    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> =
        call {
            val cache = CacheControlEphemeral.builder().build()
            val useLowEffort = lowEffort ?: supportsLowEffort().also { lowEffort = it }
            val params =
                MessageCreateParams
                    .builder()
                    .model(model)
                    .maxTokens(TutorLimits.MAX_OUTPUT_TOKENS)
                    .apply { if (useLowEffort) outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build()) }
                    .systemOfTextBlockParams(
                        listOf(
                            TextBlockParam
                                .builder()
                                .text(systemPrompt)
                                .cacheControl(cache)
                                .build(),
                        ),
                    ).cacheControl(cache)
                    .messages(history.map(::toMessageParam))
                    .build()

            val response = client.messages().create(params)
            val usage = response.usage()
            logUsage(provider, usage.inputTokens(), usage.outputTokens(), usage.cacheReadInputTokens().orElse(0L))

            when (response.stopReason().orElse(null)) {
                StopReason.REFUSAL -> throw AppException(TutorError.Refused)
                StopReason.MODEL_CONTEXT_WINDOW_EXCEEDED -> throw AppException(TutorError.ContextTooLong)
                else -> Unit
            }
            val text = response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            val payload = json.write(response.content().map { it.toParam() })
            TutorReply(text, payload)
        }

    override suspend fun testKey(): Result<Unit> = call { client.models().retrieve(model) }

    /** Every Claude model is a chat model, and the API lists the newest first. */
    override suspend fun listModels(): Result<List<String>> =
        call {
            client
                .models()
                .list()
                .autoPager()
                .map { it.id() }
        }

    /** Reads the model's capabilities (no tokens). A model that doesn't report them gets no effort setting. */
    private fun supportsLowEffort(): Boolean =
        client
            .models()
            .retrieve(model)
            .capabilities()
            .map { runCatching { it.effort().low().supported() }.getOrDefault(false) }
            .orElse(false)

    private fun toMessageParam(turn: Turn): MessageParam {
        val builder = MessageParam.builder()
        return when (turn.role) {
            Role.USER -> {
                builder.role(MessageParam.Role.USER).content(turn.text)
            }

            Role.ASSISTANT -> {
                builder.role(MessageParam.Role.ASSISTANT).apply {
                    val blocks = turn.providerPayload?.let { json.read(it, blockListType) }
                    if (blocks != null) contentOfBlockParams(blocks) else content(turn.text)
                }
            }
        }.build()
    }

    private suspend fun <T> call(block: () -> T): Result<T> = providerCall(::classify, block)

    private fun classify(e: Exception): TutorError? =
        when (e) {
            is AnthropicServiceException -> errorForStatus(e.statusCode(), e.message)
            is AnthropicIoException -> TutorError.Offline
            else -> null
        }

    private companion object {
        val json =
            SdkJson({ JsonValue.from(it).convert(JsonNode::class.java)!! }, { node, type -> JsonValue.fromJsonNode(node).convert(type) })
        val blockListType = object : TypeReference<List<ContentBlockParam>>() {}
    }
}
