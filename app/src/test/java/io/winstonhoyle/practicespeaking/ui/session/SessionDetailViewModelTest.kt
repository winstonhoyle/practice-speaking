package io.winstonhoyle.practicespeaking.ui.session

import io.winstonhoyle.practicespeaking.TestApp
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.await
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionDetailViewModelTest {
    private lateinit var app: TestApp
    private var sessionId = 0L
    private var tutorMessageId = 0L

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        app = TestApp()
        runBlocking {
            sessionId = app.sessions.startSession(TutorLanguage.SPANISH, Provider.GEMINI, "gemini-test", CefrLevel.B1)
            app.sessions.addUserMessage(sessionId, "Hola")
            tutorMessageId = app.sessions.addAssistantMessage(sessionId, "¡Hola! ¿Qué tal?", null)
        }
    }

    @After
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SessionDetailViewModel(app.sessions, sessionId, app.settings, app.tutorModels, app.translator, app.speaker).also { vm ->
            vm.state.await { !it.loading && it.messages.size == 2 }
        }

    @Test
    fun aTranslationUsesTheSessionsProviderAndModelAndIsSaved() {
        runBlocking { app.settings.setApiKey(Provider.GEMINI, "AIza-test") }
        app.tutor.enqueue("Hi! How are you?")
        val vm = viewModel()

        vm.toggleTranslation(tutorMessageId)
        val state = vm.state.await { tutorMessageId in it.translations.shown && it.messages[1].translation != null }

        assertEquals(Triple("GEMINI", "AIza-test", "gemini-test"), app.builtModels.last())
        assertEquals(listOf(Turn(Role.USER, "¡Hola! ¿Qué tal?")), app.tutor.requests.last())
        assertEquals("Hi! How are you?", state.translations.visible(tutorMessageId, state.messages[1].translation))
    }

    @Test
    fun withoutAKeyForTheSessionsProviderTheTranslationAsksForOne() {
        val vm = viewModel()
        vm.toggleTranslation(tutorMessageId)
        assertEquals(TutorError.InvalidKey, vm.state.await { it.translationError != null }.translationError)
        vm.dismissTranslationError()
        assertNull(vm.state.await { it.translationError == null }.translationError)
    }

    @Test
    fun hearItSpeaksInTheSessionsLanguageAtTheSavedSpeed() {
        runBlocking { app.settings.setVoiceSpeed(VoiceSpeed.Relaxed) }
        val vm = viewModel()
        vm.replay("¡Hola!")
        runBlocking { withTimeout(5_000) { while (app.speaker.spoken.isEmpty()) delay(10) } }
        assertEquals("es-MX", app.speaker.localeTag)
        assertEquals(VoiceSpeed.Relaxed.rate, app.speaker.currentRate)
        assertEquals(listOf("¡Hola!"), app.speaker.spoken)
    }
}
