package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.databind.JsonNode
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.error.appError
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiTutorModelTest : TutorModelContractTest() {
    override fun baseUrl() = server.url("/v1beta").toString().trimEnd('/')

    override fun model(baseUrl: String): TutorModel = GeminiTutorModel("test-key", "gemini-test", baseUrl, OkHttpClient())

    override fun replyBody(text: String) =
        """{"candidates":[{"content":{"role":"model","parts":[{"text":"thinking","thought":true},{"text":"$text","thoughtSignature":"sig-1"}]},
            "finishReason":"STOP"}],"usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":5}}"""

    override fun refusalBody() = """{"candidates":[{"finishReason":"SAFETY"}]}"""

    override fun errorBody() = """{"error":{"message":"nope"}}"""

    override fun contextTooLongBody() =
        """{"error":{"code":400,"message":"The input token count (1200000) exceeds the maximum number of tokens allowed (1048576).","status":"INVALID_ARGUMENT"}}"""

    override fun testKeyBody() = """{"name":"models/gemini-test"}"""

    override val testKeyPath = "/v1beta/models/gemini-test"

    override fun modelListBody() =
        """{"models":[
            {"name":"models/gemini-flash-x","supportedGenerationMethods":["generateContent","countTokens"]},
            {"name":"models/text-embedding-004","supportedGenerationMethods":["embedContent"]},
            {"name":"models/gemini-flash-x-image","supportedGenerationMethods":["generateContent"]},
            {"name":"models/gemini-pro-x","supportedGenerationMethods":["generateContent"]}]}"""

    override val expectedModels = listOf("gemini-flash-x", "gemini-pro-x")

    @Test
    fun listModelsFollowsPageTokens() {
        enqueue("""{"models":[{"name":"models/gemini-a","supportedGenerationMethods":["generateContent"]}],"nextPageToken":"p2"}""")
        enqueue("""{"models":[{"name":"models/gemini-b","supportedGenerationMethods":["generateContent"]}]}""")
        assertEquals(listOf("gemini-a", "gemini-b"), runBlocking { model().listModels() }.getOrThrow())
        assertEquals("/v1beta/models?pageSize=1000", server.takeRequest().path)
        assertEquals("/v1beta/models?pageSize=1000&pageToken=p2", server.takeRequest().path)
    }

    override fun replayedAssistantTurn(body: JsonNode): JsonNode = body.path("contents").path(1)

    @Test
    fun requestSendsTheKeyHeaderSystemInstructionAndLimit() {
        enqueue(replyBody("Hallo!"))
        replyTo(model(), Turn(Role.USER, "Hallo"))
        val request = server.takeRequest()
        assertEquals("/v1beta/models/gemini-test:generateContent", request.path)
        assertEquals("test-key", request.getHeader("x-goog-api-key"))
        val body = mapper.readTree(request.body.readUtf8())
        assertEquals(
            "SYSTEM",
            body
                .path("systemInstruction")
                .path("parts")
                .path(0)
                .path("text")
                .asText(),
        )
        assertEquals(1024, body.path("generationConfig").path("maxOutputTokens").asInt())
    }

    @Test
    fun thoughtPartsAreHiddenButTheirSignatureIsReplayed() {
        enqueue(replyBody("Hallo!"))
        enqueue(replyBody("Gut."))
        val model = model()
        val first = replyTo(model, Turn(Role.USER, "Hallo")).getOrThrow()
        assertEquals("Hallo!", first.text)
        replyTo(model, Turn(Role.USER, "Hallo"), Turn(Role.ASSISTANT, first.text, first.providerPayload), Turn(Role.USER, "Gut"))
        server.takeRequest()
        val replayed = replayedAssistantTurn(mapper.readTree(server.takeRequest().body.readUtf8()))
        assertEquals("model", replayed.path("role").asText())
        assertEquals(
            "sig-1",
            replayed
                .path("parts")
                .path(1)
                .path("thoughtSignature")
                .asText(),
        )
    }

    @Test
    fun blockedPrompt_mapsToRefused() {
        enqueue("""{"promptFeedback":{"blockReason":"SAFETY"}}""")
        assertEquals(TutorError.Refused, replyTo(model(), Turn(Role.USER, "x")).appError())
    }

    @Test
    fun invalidKeyAs400_mapsToInvalidKey() {
        enqueue("""{"error":{"code":400,"message":"API key not valid.","details":[{"reason":"API_KEY_INVALID"}]}}""", 400)
        assertEquals(TutorError.InvalidKey, replyTo(model(), Turn(Role.USER, "x")).appError())
    }
}
