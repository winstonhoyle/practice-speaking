package io.winstonhoyle.practicespeaking.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.error.appError
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The behavior every [TutorModel] must have, run against a mock server. A new provider gets
 * this whole suite by subclassing and describing its API's responses; vendor-specific request
 * details go in the subclass as extra tests.
 */
abstract class TutorModelContractTest {
    protected val server = MockWebServer().apply { start() }
    protected val mapper = ObjectMapper()

    /** A model pointed at [baseUrl] with retries off. */
    protected abstract fun model(baseUrl: String = baseUrl()): TutorModel

    /** The base URL for [model], built from [server]. */
    protected abstract fun baseUrl(): String

    /** A successful reply whose text is [text]. */
    protected abstract fun replyBody(text: String): String

    /** A reply the provider marks as a safety refusal or block. */
    protected abstract fun refusalBody(): String

    /** An error body in the provider's format, for status-code mapping. */
    protected abstract fun errorBody(): String

    /** The provider's 400 body for a conversation that no longer fits the context window. */
    protected abstract fun contextTooLongBody(): String

    /** A model-metadata response and the path [TutorModel.testKey] must request. */
    protected abstract fun testKeyBody(): String

    protected abstract val testKeyPath: String

    /** One page from the provider's model list, mixing chat models with ones the tutor can't use. */
    protected abstract fun modelListBody(): String

    /** What [TutorModel.listModels] should return for [modelListBody], in order. */
    protected abstract val expectedModels: List<String>

    /** The prior assistant turn as it appears in the second request body, given the parsed body. */
    protected abstract fun replayedAssistantTurn(body: JsonNode): JsonNode

    @After
    fun shutDown() = server.shutdown()

    protected fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))

    protected fun replyTo(model: TutorModel, vararg turns: Turn) = runBlocking { model.reply("SYSTEM", turns.toList()) }

    @Test
    fun reply_returnsTheTextAndAPayload() {
        enqueue(replyBody("Привет!"))
        val reply = replyTo(model(), Turn(Role.USER, "Привет")).getOrThrow()
        assertEquals("Привет!", reply.text)
        assertNotNull(reply.providerPayload)
    }

    @Test
    fun theSavedPayloadIsReplayedNextTurn() {
        enqueue(replyBody("Привет!"))
        enqueue(replyBody("Хорошо."))
        val model = model()
        val first = replyTo(model, Turn(Role.USER, "Привет")).getOrThrow()
        replyTo(model, Turn(Role.USER, "Привет"), Turn(Role.ASSISTANT, first.text, first.providerPayload), Turn(Role.USER, "Как дела?"))

        server.takeRequest()
        val replayed = replayedAssistantTurn(mapper.readTree(server.takeRequest().body.readUtf8()))
        assertTrue("replayed turn should carry the reply text: $replayed", "Привет!" in replayed.toString())
    }

    @Test
    fun refusal_mapsToRefused() {
        enqueue(refusalBody())
        assertEquals(TutorError.Refused, replyTo(model(), Turn(Role.USER, "x")).appError())
    }

    @Test
    fun httpStatuses_mapToTutorErrors() {
        val cases =
            listOf(
                401 to TutorError.InvalidKey,
                403 to TutorError.InvalidKey,
                404 to TutorError.ModelNotFound,
                429 to TutorError.RateLimited,
                500 to TutorError.ProviderDown,
                529 to TutorError.ProviderDown,
            )
        val model = model()
        cases.forEach { (status, expected) ->
            enqueue(errorBody(), status)
            assertEquals("HTTP $status", expected, replyTo(model, Turn(Role.USER, "hi")).appError())
        }
    }

    @Test
    fun contextTooLong_mapsToContextTooLong() {
        enqueue(contextTooLongBody(), 400)
        assertEquals(TutorError.ContextTooLong, replyTo(model(), Turn(Role.USER, "hi")).appError())
    }

    @Test
    fun noConnection_mapsToOffline() {
        val dead = MockWebServer().apply { start() }
        val url = dead.url("/").toString()
        dead.shutdown()
        assertEquals(TutorError.Offline, replyTo(model(url.trimEnd('/')), Turn(Role.USER, "hi")).appError())
    }

    @Test
    fun testKey_fetchesTheModelWithoutGenerating() {
        enqueue(testKeyBody())
        assertTrue(runBlocking { model().testKey() }.isSuccess)
        assertEquals(testKeyPath, server.takeRequest().path)
    }

    @Test
    fun listModels_keepsTheChatModels() {
        enqueue(modelListBody())
        assertEquals(expectedModels, runBlocking { model().listModels() }.getOrThrow())
    }

    @Test
    fun listModels_withABadKeyIsInvalidKey() {
        enqueue(errorBody(), 401)
        assertEquals(TutorError.InvalidKey, runBlocking { model().listModels() }.appError())
    }
}
