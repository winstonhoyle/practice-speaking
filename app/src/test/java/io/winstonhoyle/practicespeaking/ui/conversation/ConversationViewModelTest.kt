package io.winstonhoyle.practicespeaking.ui.conversation

import io.winstonhoyle.practicespeaking.TestApp
import io.winstonhoyle.practicespeaking.ai.FakeTutorModel
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.Role
import io.winstonhoyle.practicespeaking.ai.Turn
import io.winstonhoyle.practicespeaking.await
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.error.SpeechError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.components.TutorCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationViewModelTest {
    private lateinit var app: TestApp

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = TestApp()
    }

    @After
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(withKey: Boolean = true): ConversationViewModel {
        if (withKey) runBlocking { app.settings.setApiKey(Provider.ANTHROPIC, "sk-ant-test") }
        return ConversationViewModel(app.settings, app.sessions, app.prompts, app.tutorModels, app.speaker, app.translator).also { vm ->
            vm.state.await { !it.loading }
        }
    }

    @Test
    fun withoutAKey_asksForSettings() {
        val vm = viewModel(withKey = false)
        assertTrue(vm.state.value.needsKey)
        assertFalse(vm.state.value.canSend)
    }

    @Test
    fun usesTheSavedLanguageForPromptAndVoice() {
        runBlocking { app.settings.setLanguage(TutorLanguage.SPANISH) }
        val vm = viewModel()
        assertEquals(TutorLanguage.SPANISH, vm.state.value.language)
        assertEquals("es-MX", app.speaker.localeTag)
    }

    @Test
    fun aTurnSavesBothMessagesAttachesTheCorrectionAndSpeaksOnlyTheReply() {
        app.tutor.enqueue(FakeTutorModel.WITH_CORRECTION)
        val vm = viewModel()

        vm.send("Я иду в магазине")
        val state = vm.state.await { it.messages.size == 2 && !it.isThinking }

        val (user, tutor) = state.messages
        assertEquals("в магазине → в магазин | Use the accusative after a verb of motion.", user.correction)
        assertEquals("Ты идёшь в магазин? Что ты хочешь купить?", tutor.text)
        assertEquals(listOf("Ты идёшь в магазин? Что ты хочешь купить?"), app.speaker.spoken)

        val saved =
            runBlocking {
                app.sessions
                    .observeMessages(
                        app.sessions
                            .observeSessions()
                            .first()
                            .single()
                            .id,
                    ).first()
            }
        assertEquals(user.correction, saved[0].correction)
        assertTrue(saved[1].providerPayload!!.isNotEmpty())
    }

    @Test
    fun theNextTurnReplaysTheSavedPayload() {
        app.tutor.enqueue("Привет!")
        app.tutor.enqueue("Хорошо.")
        val vm = viewModel()

        vm.send("Привет")
        vm.state.await { it.messages.size == 2 && !it.isThinking }
        vm.send("Как дела?")
        vm.state.await { it.messages.size == 4 && !it.isThinking }

        val second = app.tutor.requests[1]
        assertEquals(listOf(Role.USER, Role.ASSISTANT, Role.USER), second.map { it.role })
        assertEquals("""{"fake":0}""", second[1].providerPayload)
    }

    @Test
    fun theSessionRecordsProviderAndModel() {
        runBlocking { app.settings.setModel(Provider.ANTHROPIC, "claude-custom") }
        app.tutor.enqueue("Привет!")
        val vm = viewModel()
        vm.send("Привет")
        vm.state.await { it.messages.size == 2 }

        assertEquals(Triple("ANTHROPIC", "sk-ant-test", "claude-custom"), app.builtModels.last())
        val row =
            runBlocking {
                app.sessions
                    .observeSessions()
                    .first()
                    .single()
            }
        assertEquals("ANTHROPIC", row.provider)
        assertEquals(app.tutor.model, row.model)
    }

    @Test
    fun anErrorShowsAndRetryResendsTheSameHistory() {
        app.tutor.enqueueError(TutorError.RateLimited)
        app.tutor.enqueue("Привет!")
        val vm = viewModel()

        vm.send("Привет")
        val failed = vm.state.await { it.error != null }
        assertEquals(TutorError.RateLimited, failed.error)
        assertEquals(1, failed.messages.size)

        vm.retry()
        val recovered = vm.state.await { it.messages.size == 2 }
        assertNull(recovered.error)
        assertEquals(app.tutor.requests[0], app.tutor.requests[1])
    }

    @Test
    fun aBlankReplyIsAnErrorAndNotSaved() {
        app.tutor.enqueue("   ")
        val vm = viewModel()
        vm.send("Привет")
        val state = vm.state.await { it.error != null }
        assertEquals(TutorError.EmptyReply, state.error)
        assertEquals(1, state.messages.size)
        assertTrue(app.speaker.spoken.isEmpty())
    }

    @Test
    fun aKeyThatNoLongerDecryptsIsReported() {
        runBlocking {
            app.settings.setApiKey(Provider.ANTHROPIC, "sk-ant-test")
            app.cipher.broken = true
        }
        val vm = viewModel(withKey = false)
        assertTrue(vm.state.value.needsKey)
        assertEquals(StorageError.KeyUnreadable, vm.state.value.error)
    }

    @Test
    fun speechFailuresShowAsErrors() {
        val vm = viewModel()
        vm.onListenStart()
        vm.onListenFailed(SpeechError.NoPermission)
        assertFalse(vm.state.value.isListening)
        assertEquals(SpeechError.NoPermission, vm.state.value.error)
    }

    @Test
    fun commandsSendTheEnglishTriggerWords() {
        app.tutor.enqueue("Привет!")
        app.tutor.enqueue("Хорошо, медленнее.")
        val vm = viewModel()
        vm.send("Привет")
        vm.state.await { it.messages.size == 2 }

        vm.onCommand(TutorCommand.Slower)
        vm.state.await { it.messages.size == 4 }
        assertEquals(
            "slower",
            app.tutor.requests[1]
                .last()
                .text,
        )
    }

    @Test
    fun endSessionSavesTheSummary() {
        app.tutor.enqueue("Привет!")
        app.tutor.enqueue(FakeTutorModel.WITH_SUMMARY)
        val vm = viewModel()
        vm.send("Привет")
        vm.state.await { it.messages.size == 2 }

        vm.endSession()
        val ended = vm.state.await { it.ended }
        assertEquals("хлеб — bread", ended.summary?.newWords)
        assertEquals(
            "end session",
            app.tutor.requests
                .last()
                .last()
                .text,
        )
        assertEquals(listOf("Привет!", "Пока! До встречи."), app.speaker.spoken)

        val session =
            runBlocking {
                app.sessions
                    .observeSession(
                        app.sessions
                            .observeSessions()
                            .first()
                            .single()
                            .id,
                    ).first()!!
            }
        assertEquals("Practise the accusative after в.", session.summaryTip)
        assertTrue(session.endedAt != null)
    }

    @Test
    fun endingBeforeSpeakingSavesNothing() {
        val vm = viewModel()
        vm.endSession()
        assertTrue(vm.state.await { it.ended }.ended)
        assertTrue(app.tutor.requests.isEmpty())
        assertTrue(runBlocking { app.sessions.observeSessions().first() }.isEmpty())
    }

    @Test
    fun listeningStopsSpeechAndAFinalTranscriptIsSent() {
        app.tutor.enqueue("Да!")
        val vm = viewModel()

        vm.onListenStart()
        assertTrue(vm.state.value.isListening)
        assertEquals(1, app.speaker.stops)
        vm.onPartialTranscript("При")
        assertEquals("При", vm.state.value.partialTranscript)

        vm.onListenResult("Привет")
        vm.state.await { it.messages.size == 2 }
        assertFalse(vm.state.value.isListening)
    }

    @Test
    fun nothingHeardSendsNothing() {
        val vm = viewModel()
        vm.onListenStart()
        vm.onListenFailed(SpeechError.NothingHeard)
        assertFalse(vm.state.value.isListening)
        assertTrue(
            vm.state.value.messages
                .isEmpty(),
        )
    }

    @Test
    fun longSessionBannerAppearsAt60Turns() {
        val vm = viewModel()
        repeat(ConversationUiState.LONG_SESSION_TURNS) { i ->
            vm.send("turn $i")
            vm.state.await { it.messages.size == (i + 1) * 2 }
            if (i == ConversationUiState.LONG_SESSION_TURNS - 2) assertFalse(vm.state.value.showLongSessionBanner)
        }
        assertTrue(vm.state.value.showLongSessionBanner)
    }

    @Test
    fun slowerSlowsTheVoiceOneStepAtATime() {
        val vm = viewModel()
        assertEquals(1.0f, app.speaker.currentRate)
        vm.onCommand(TutorCommand.Slower)
        vm.state.await { !it.isThinking }
        assertEquals(VoiceSpeed.Relaxed.rate, app.speaker.currentRate)
        vm.onCommand(TutorCommand.Slower)
        vm.state.await { !it.isThinking }
        vm.onCommand(TutorCommand.Slower)
        vm.state.await { !it.isThinking }
        assertEquals(VoiceSpeed.Slow.rate, app.speaker.currentRate)
    }

    @Test
    fun theSavedVoiceSpeedIsUsed() {
        runBlocking { app.settings.setVoiceSpeed(VoiceSpeed.Slow) }
        viewModel()
        assertEquals(VoiceSpeed.Slow.rate, app.speaker.currentRate)
    }

    @Test
    fun stressMarksAreShownButKeptOutOfSpeechAndSearch() {
        app.tutor.enqueue("Приве́т! Как дела́?\nSTRESSED: Приве́т, как дела́?")
        val vm = viewModel()
        vm.send("Привет, как дела?")
        val state = vm.state.await { it.messages.size == 2 && !it.isThinking }

        assertEquals("Приве́т, как дела́?", state.messages[0].text)
        assertEquals("Приве́т! Как дела́?", state.messages[1].text)
        assertEquals(listOf("Привет! Как дела?"), app.speaker.spoken)
        val saved =
            runBlocking {
                app.sessions
                    .observeMessages(
                        app.sessions
                            .observeSessions()
                            .first()
                            .single()
                            .id,
                    ).first()
            }
        assertEquals("Приве́т, как дела́?", saved[0].stressed)
        assertEquals("Привет! Как дела?", saved[1].text)
        assertEquals("Приве́т! Как дела́?", saved[1].stressed)
        // Both messages say "дела", stored without marks, so the plain word finds them.
        assertEquals(2, runBlocking { app.sessions.search("дела").first() }.size)
    }

    @Test
    fun aStressedLineWithDifferentWordsIsIgnored() {
        app.tutor.enqueue("Хорошо́.\nSTRESSED: Я иду́ в магази́н.")
        val vm = viewModel()
        vm.send("Я иду в магазине")
        assertEquals(
            "Я иду в магазине",
            vm.state
                .await { it.messages.size == 2 }
                .messages[0]
                .text,
        )
    }

    @Test
    fun aTranslationIsFetchedOnceThenToggled() {
        app.tutor.enqueue("Приве́т!")
        app.tutor.enqueue("Hi!")
        val vm = viewModel()
        vm.send("Привет")
        val tutorId =
            vm.state
                .await { it.messages.size == 2 && !it.isThinking }
                .messages[1]
                .id

        vm.toggleTranslation(tutorId)
        val shown = vm.state.await { tutorId in it.translations.shown }
        assertEquals("Hi!", shown.translations.visible(tutorId))
        // A request of its own: one turn, stress marks removed, not part of the conversation.
        assertEquals(listOf(Turn(Role.USER, "Привет!")), app.tutor.requests.last())

        vm.toggleTranslation(tutorId)
        assertFalse(tutorId in vm.state.value.translations.shown)
        vm.toggleTranslation(tutorId)
        assertTrue(tutorId in vm.state.value.translations.shown)
        assertEquals(2, app.tutor.requests.size)
        val saved =
            runBlocking {
                app.sessions
                    .observeMessages(
                        app.sessions
                            .observeSessions()
                            .first()
                            .single()
                            .id,
                    ).first()
            }
        assertEquals("Hi!", saved[1].translation)
    }

    @Test
    fun aSavedSessionContinuesWithItsLevelAndFullHistory() {
        runBlocking { app.settings.setLevel(CefrLevel.B2) }
        app.tutor.enqueue("Привет!")
        val first = viewModel()
        first.send("Привет")
        first.state.await { it.messages.size == 2 && !it.isThinking }
        val sessionId =
            runBlocking {
                app.sessions
                    .observeSessions()
                    .first()
                    .single()
                    .id
            }

        runBlocking { app.settings.setLevel(CefrLevel.A1) }
        val continued =
            ConversationViewModel(app.settings, app.sessions, app.prompts, app.tutorModels, app.speaker, app.translator, sessionId)
        val restored = continued.state.await { !it.loading && it.messages.size == 2 }
        assertEquals(CefrLevel.B2, restored.level)

        app.tutor.enqueue("Хорошо.")
        continued.send("Как дела?")
        continued.state.await { it.messages.size == 4 && !it.isThinking }
        val request = app.tutor.requests.last()
        assertEquals(listOf(Role.USER, Role.ASSISTANT, Role.USER), request.map { it.role })
        assertEquals("""{"fake":0}""", request[1].providerPayload)
        assertEquals(1, runBlocking { app.sessions.observeSessions().first() }.size)
    }

    @Test
    fun aNewKeyFromSettingsIsUsedInTheSameSession() {
        app.tutor.enqueue("Привет!")
        val vm = viewModel()
        vm.send("Привет")
        vm.state.await { it.messages.size == 2 && !it.isThinking }

        runBlocking { app.settings.setApiKey(Provider.ANTHROPIC, "sk-ant-new") }
        vm.reloadSettings()
        app.tutor.enqueue("Хорошо.")
        vm.send("Как дела?")
        vm.state.await { it.messages.size == 4 && !it.isThinking }

        // Same provider and the model the session recorded; only the key changes.
        val sessionModel =
            runBlocking {
                app.sessions
                    .observeSessions()
                    .first()
                    .single()
                    .model
            }
        assertEquals(Triple("ANTHROPIC", "sk-ant-new", sessionModel), app.builtModels.last())
    }
}
