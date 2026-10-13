package io.winstonhoyle.practicespeaking.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.db.AppDatabase
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repo = SessionRepository(db.sessionDao(), clock = { 1_000L })

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndObserve() =
        runBlocking {
            val id = repo.startSession(TutorLanguage.RUSSIAN, Provider.ANTHROPIC, "claude-haiku-5-5")
            val userId = repo.addUserMessage(id, "Я иду в магазине")
            repo.addAssistantMessage(id, "Ты идёшь в магазин?", """{"p":1}""")
            repo.setCorrection(userId, "в магазине → в магазин | Accusative.")
            repo.endSession(id, Summary("в магазине → в магазин", "хлеб — bread", "Practise."))

            val row = repo.observeSessions().first().single()
            assertEquals("Я иду в магазине", row.title)
            assertEquals(2, row.messageCount)
            assertEquals("ANTHROPIC", row.provider)
            assertEquals("claude-haiku-5-5", row.model)

            val messages = repo.observeMessages(id).first()
            assertEquals("в магазине → в магазин | Accusative.", messages[0].correction)
            assertEquals("""{"p":1}""", messages[1].providerPayload)
            val session = repo.observeSession(id).first()!!
            assertEquals("Practise.", session.summaryTip)
            assertEquals(1_000L, session.endedAt)
        }

    @Test
    fun emptySessionsAreHiddenFromHistory() =
        runBlocking {
            repo.startSession(TutorLanguage.FRENCH, Provider.OPENAI, "gpt")
            assertTrue(repo.observeSessions().first().isEmpty())
        }

    @Test
    fun deletingASessionCascadesToMessagesAndSearch() =
        runBlocking {
            val id = repo.startSession(TutorLanguage.RUSSIAN, Provider.GEMINI, "g")
            repo.addUserMessage(id, "магазин")
            repo.deleteSession(id)

            assertNull(repo.observeSession(id).first())
            assertTrue(repo.observeMessages(id).first().isEmpty())
            assertTrue(repo.search("магазин").first().isEmpty())
        }

    @Test
    fun ftsFindsWordsInTextAndCorrectionWithPrefixAndCase() =
        runBlocking {
            val id = repo.startSession(TutorLanguage.RUSSIAN, Provider.ANTHROPIC, "m")
            val userId = repo.addUserMessage(id, "Я иду в магазине")
            repo.addAssistantMessage(id, "Что ты купишь?", null)
            repo.setCorrection(userId, "в магазине → в магазин | Accusative after a verb of motion.")

            assertEquals(listOf(userId), repo.search("магазин").first().map { it.messageId })
            assertEquals(listOf(userId), repo.search("Магазин").first().map { it.messageId })
            assertEquals(listOf(userId), repo.search("accusative").first().map { it.messageId })
            assertEquals(
                "Что ты купишь?",
                repo
                    .search("купишь")
                    .first()
                    .single()
                    .text,
            )
            assertTrue(repo.search("\"*(").first().isEmpty())
        }

    @Test
    fun longTitlesAreTruncated() {
        assertEquals(60, SessionRepository.titleFrom("a".repeat(100)).length)
    }
}
