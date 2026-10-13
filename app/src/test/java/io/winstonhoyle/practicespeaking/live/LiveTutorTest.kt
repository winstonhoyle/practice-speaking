package io.winstonhoyle.practicespeaking.live

import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.ai.TutorModelFactory
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * Real calls through each provider's [io.winstonhoyle.practicespeaking.ai.TutorModel], with the
 * real Russian prompt. Runs only with `-Plive` and skips a provider whose `<PROVIDER>_API_KEY`
 * isn't in `.env`. Each run costs a few cents at most, so run it a few times in total, not in a loop.
 */
@RunWith(Parameterized::class)
class LiveTutorTest(private val provider: Provider) {
    private val tutor by lazy { TutorModelFactory.create(provider, LiveEnv.requireKey("${provider.name}_API_KEY")) }
    private val prompt =
        PromptRenderer(File("src/main/assets/${PromptRenderer.TEMPLATE_ASSET}").readText()).render(TutorLanguage.RUSSIAN)

    @Test
    fun keyCheckAndModelList() =
        runBlocking {
            tutor.testKey().getOrThrow()
            val models = tutor.listModels().getOrThrow()
            println("${provider.shortName}: ${models.size} models, first ${models.take(3)}")
            assertTrue(models.isNotEmpty())
        }

    /** A deliberate mistake must come back as a correction, and the next turn must replay the saved payload. */
    @Test
    fun aMistakeGetsACorrectionAndTheNextTurnReplays() =
        runBlocking {
            val first = Turn(Role.USER, "Привет! Вчера я ходил в магазине и купил хлеб.")
            val reply = tutor.reply(prompt, listOf(first)).getOrThrow()
            val parsed = TutorResponseParser.parse(reply.text)
            println("${provider.shortName} reply: ${reply.text}")
            assertTrue(parsed.spokenText.isNotBlank())
            assertNotNull("expected a CORRECTION line", parsed.correction ?: parsed.rawCorrection)
            assertNotNull("expected a payload to replay", reply.providerPayload)

            val history = listOf(first, Turn(Role.ASSISTANT, reply.text, reply.providerPayload), Turn(Role.USER, "Я купил хлеб и сыр."))
            val second = tutor.reply(prompt, history).getOrThrow()
            println("${provider.shortName} second reply: ${second.text}")
            assertTrue(TutorResponseParser.parse(second.text).spokenText.isNotBlank())
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun providers() = Provider.entries.toList()
    }
}
