package io.winstonhoyle.practicespeaking.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.fasterxml.jackson.databind.JsonNode
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.error.appError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicTutorModelTest : TutorModelContractTest() {
    override fun baseUrl() = server.url("/").toString()

    // The shared suite's request order has no capability lookup, so it starts with the answer known.
    override fun model(baseUrl: String): TutorModel = anthropic(baseUrl, lowEffort = true)

    private fun anthropic(baseUrl: String = baseUrl(), lowEffort: Boolean? = null) =
        AnthropicTutorModel(
            "claude-test",
            AnthropicOkHttpClient
                .builder()
                .apiKey("test-key")
                .baseUrl(baseUrl)
                .maxRetries(0)
                .build(),
            lowEffort,
        )

    private fun modelInfo(effortLow: Boolean) =
        """{"type":"model","id":"claude-test","display_name":"Test","created_at":"2026-01-01T00:00:00Z",
            "capabilities":{"effort":{"supported":$effortLow,"low":{"supported":$effortLow}}}}"""

    override fun replyBody(text: String) =
        message("""[{"type":"thinking","thinking":"hmm","signature":"sig-1"},{"type":"text","text":"$text"}]""")

    override fun refusalBody() = message("[]", stopReason = "refusal")

    override fun errorBody() = """{"type":"error","error":{"type":"error","message":"nope"}}"""

    override fun contextTooLongBody() =
        """{"type":"error","error":{"type":"invalid_request_error","message":"prompt is too long: 210000 tokens > 200000 maximum"}}"""

    override fun testKeyBody() = """{"type":"model","id":"claude-test","display_name":"Test","created_at":"2026-01-01T00:00:00Z"}"""

    override val testKeyPath = "/v1/models/claude-test"

    override fun modelListBody() =
        """{"data":[{"type":"model","id":"claude-new","display_name":"New","created_at":"2026-06-01T00:00:00Z"},
            {"type":"model","id":"claude-old","display_name":"Old","created_at":"2025-01-01T00:00:00Z"}],
            "has_more":false,"first_id":"claude-new","last_id":"claude-old"}"""

    override val expectedModels = listOf("claude-new", "claude-old")

    override fun replayedAssistantTurn(body: JsonNode): JsonNode = body.path("messages").path(1)

    @Test
    fun requestUsesLowEffortAndCachesSystemAndHistory() {
        enqueue(replyBody("Привет!"))
        replyTo(model(), Turn(Role.USER, "Привет"))
        val body = mapper.readTree(server.takeRequest().body.readUtf8())
        assertEquals("low", body.path("output_config").path("effort").asText())
        assertEquals(1024, body.path("max_tokens").asInt())
        assertEquals(
            "ephemeral",
            body
                .path("system")
                .path(0)
                .path("cache_control")
                .path("type")
                .asText(),
        )
        assertEquals("ephemeral", body.path("cache_control").path("type").asText())
    }

    @Test
    fun aModelWithoutEffortSupportGetsNoEffortSetting() {
        enqueue(modelInfo(effortLow = false))
        enqueue(replyBody("Привет!"))
        assertTrue(replyTo(anthropic(), Turn(Role.USER, "Привет")).isSuccess)
        assertEquals("/v1/models/claude-test", server.takeRequest().path)
        assertTrue(mapper.readTree(server.takeRequest().body.readUtf8()).path("output_config").isMissingNode)
    }

    @Test
    fun capabilitiesAreLookedUpOnceThenLowEffortIsSent() {
        enqueue(modelInfo(effortLow = true))
        enqueue(replyBody("Привет!"))
        enqueue(replyBody("Хорошо."))
        val model = anthropic()
        replyTo(model, Turn(Role.USER, "Привет"))
        replyTo(model, Turn(Role.USER, "Привет"))
        assertEquals("/v1/models/claude-test", server.takeRequest().path)
        repeat(
            2,
        ) {
            assertEquals(
                "low",
                mapper
                    .readTree(server.takeRequest().body.readUtf8())
                    .path("output_config")
                    .path("effort")
                    .asText(),
            )
        }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun aModelWithoutCapabilityDataGetsNoEffortSetting() {
        enqueue(testKeyBody())
        enqueue(replyBody("Привет!"))
        replyTo(anthropic(), Turn(Role.USER, "Привет"))
        server.takeRequest()
        assertTrue(mapper.readTree(server.takeRequest().body.readUtf8()).path("output_config").isMissingNode)
    }

    @Test
    fun thinkingBlocksAreReplayedWithTheirSignature() {
        enqueue(replyBody("Привет!"))
        enqueue(replyBody("Хорошо."))
        val model = model()
        val first = replyTo(model, Turn(Role.USER, "Привет")).getOrThrow()
        replyTo(model, Turn(Role.USER, "Привет"), Turn(Role.ASSISTANT, first.text, first.providerPayload), Turn(Role.USER, "Хорошо"))
        server.takeRequest()
        val content = replayedAssistantTurn(mapper.readTree(server.takeRequest().body.readUtf8())).path("content")
        assertEquals("thinking", content.path(0).path("type").asText())
        assertEquals("sig-1", content.path(0).path("signature").asText())
    }

    @Test
    fun usageCountsCacheReadsAndWritesAsInput() {
        enqueue(
            """{"id":"m","type":"message","role":"assistant","model":"claude-test","content":[{"type":"text","text":"Да"}],
             "stop_reason":"end_turn","stop_sequence":null,
             "usage":{"input_tokens":10,"output_tokens":5,"cache_read_input_tokens":90,"cache_creation_input_tokens":4}}""",
        )
        assertEquals(TokenUsage(input = 104, output = 5, cached = 90), replyTo(model(), Turn(Role.USER, "x")).getOrThrow().usage)
    }

    @Test
    fun contextWindowStopReason_mapsToContextTooLong() {
        enqueue(message("[]", stopReason = "model_context_window_exceeded"))
        assertEquals(TutorError.ContextTooLong, replyTo(model(), Turn(Role.USER, "x")).appError())
    }

    private fun message(content: String, stopReason: String = "end_turn") =
        """
        {"id":"msg_1","type":"message","role":"assistant","model":"claude-test","content":$content,
         "stop_reason":"$stopReason","stop_sequence":null,
         "usage":{"input_tokens":10,"output_tokens":5,"cache_read_input_tokens":0,"cache_creation_input_tokens":0}}
        """.trimIndent()
}
