package io.winstonhoyle.practicespeaking.ai

import io.winstonhoyle.practicespeaking.error.TutorError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CountingTutorModelTest {
    private val recorded = mutableListOf<Pair<Provider, TokenUsage>>()
    private val inner = FakeTutorModel(provider = Provider.GEMINI)
    private val model = CountingTutorModel(inner) { provider, usage -> recorded += provider to usage }

    @Test
    fun aSuccessfulReplyIsCountedForItsProvider() {
        inner.enqueueReply(TutorReply("Hola", null, TokenUsage(input = 120, output = 30, cached = 100)))
        runBlocking { model.reply("SYSTEM", listOf(Turn(Role.USER, "hola"))) }
        assertEquals(listOf(Provider.GEMINI to TokenUsage(120, 30, 100)), recorded)
    }

    @Test
    fun failuresAndRepliesWithoutUsageAreNotCounted() {
        inner.enqueueError(TutorError.RateLimited)
        inner.enqueue("sin datos")
        runBlocking {
            model.reply("SYSTEM", listOf(Turn(Role.USER, "a")))
            model.reply("SYSTEM", listOf(Turn(Role.USER, "b")))
        }
        assertEquals(emptyList<Pair<Provider, TokenUsage>>(), recorded)
    }
}
