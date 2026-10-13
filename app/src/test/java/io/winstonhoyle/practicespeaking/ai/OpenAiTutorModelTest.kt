package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.databind.JsonNode
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpenAiTutorModelTest : TutorModelContractTest() {
    override fun baseUrl() = server.url("/v1/").toString()

    override fun model(baseUrl: String): TutorModel =
        OpenAiTutorModel(
            "gpt-test",
            OpenAIOkHttpClient
                .builder()
                .apiKey("test-key")
                .baseUrl(baseUrl)
                .maxRetries(0)
                .build(),
        )

    override fun replyBody(text: String) =
        response(
            """[{"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":"enc-1"},
            {"type":"message","id":"msg_1","role":"assistant","status":"completed","content":[{"type":"output_text","text":"$text","annotations":[]}]}]""",
        )

    override fun refusalBody() =
        response("""[{"type":"message","id":"m","role":"assistant","status":"completed","content":[{"type":"refusal","refusal":"no"}]}]""")

    override fun errorBody() = """{"error":{"message":"nope"}}"""

    override fun contextTooLongBody() =
        """{"error":{"message":"Your input exceeds the context window of this model.","type":"invalid_request_error","code":"context_length_exceeded"}}"""

    override fun testKeyBody() = """{"id":"gpt-test","object":"model","created":1,"owned_by":"openai"}"""

    override val testKeyPath = "/v1/models/gpt-test"

    override fun modelListBody() =
        """{"object":"list","data":[
            {"id":"gpt-old","object":"model","created":1,"owned_by":"openai"},
            {"id":"text-embedding-3-small","object":"model","created":2,"owned_by":"openai"},
            {"id":"o4-mini","object":"model","created":3,"owned_by":"openai"},
            {"id":"gpt-realtime","object":"model","created":4,"owned_by":"openai"},
            {"id":"gpt-image-1","object":"model","created":5,"owned_by":"openai"},
            {"id":"gpt-new","object":"model","created":6,"owned_by":"openai"},
            {"id":"whisper-1","object":"model","created":7,"owned_by":"openai"}]}"""

    override val expectedModels = listOf("gpt-new", "o4-mini", "gpt-old")

    /** Input items: [0] user, [1] reasoning, [2] the assistant message, [3] user. */
    override fun replayedAssistantTurn(body: JsonNode): JsonNode = body.path("input").path(2)

    @Test
    fun requestIsStatelessWithLowEffortAndEncryptedReasoning() {
        enqueue(replyBody("Bonjour !"))
        replyTo(model(), Turn(Role.USER, "Salut"))
        val body = mapper.readTree(server.takeRequest().body.readUtf8())
        assertEquals("SYSTEM", body.path("instructions").asText())
        assertEquals("low", body.path("reasoning").path("effort").asText())
        assertFalse(body.path("store").asBoolean(true))
        assertEquals("reasoning.encrypted_content", body.path("include").path(0).asText())
    }

    @Test
    fun reasoningItemsAreReplayed() {
        enqueue(replyBody("Bonjour !"))
        enqueue(replyBody("Ça va."))
        val model = model()
        val first = replyTo(model, Turn(Role.USER, "Salut")).getOrThrow()
        replyTo(model, Turn(Role.USER, "Salut"), Turn(Role.ASSISTANT, first.text, first.providerPayload), Turn(Role.USER, "Ça va"))
        server.takeRequest()
        val input = mapper.readTree(server.takeRequest().body.readUtf8()).path("input")
        assertEquals("reasoning", input.path(1).path("type").asText())
        assertEquals("enc-1", input.path(1).path("encrypted_content").asText())
        assertEquals("user", input.path(3).path("role").asText())
    }

    private fun response(output: String) =
        """
        {"id":"resp_1","object":"response","created_at":1,"model":"gpt-test","status":"completed","output":$output,
         "parallel_tool_calls":true,"tool_choice":"auto","tools":[],"error":null,"incomplete_details":null,
         "instructions":null,"metadata":{},"temperature":1.0,"top_p":1.0,
         "usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15,"input_tokens_details":{"cached_tokens":0},"output_tokens_details":{"reasoning_tokens":0}}}
        """.trimIndent()
}
