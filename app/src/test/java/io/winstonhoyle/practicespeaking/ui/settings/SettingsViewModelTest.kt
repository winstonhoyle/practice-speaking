package io.winstonhoyle.practicespeaking.ui.settings

import io.winstonhoyle.practicespeaking.TestApp
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.TokenUsage
import io.winstonhoyle.practicespeaking.await
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
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

    private fun viewModel() = SettingsViewModel(app.settings, app.tutorModels).also { vm -> vm.state.await { !it.loading } }

    @Test
    fun firstLaunchHasNoKeyAndTheDefaultModel() {
        val state = viewModel().state.value
        assertEquals("", state.apiKey)
        assertEquals(Provider.ANTHROPIC.defaultModel, state.model)
        assertFalse(state.canStart)
    }

    @Test
    fun switchingProviderKeepsEachKey() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        app.settings.apiKey(Provider.ANTHROPIC).await { it == "sk-ant-1" }

        vm.selectProvider(Provider.OPENAI)
        vm.state.await { it.provider == Provider.OPENAI }
        assertEquals("", vm.state.value.apiKey)
        assertEquals(Provider.OPENAI.defaultModel, vm.state.value.model)

        vm.selectProvider(Provider.ANTHROPIC)
        assertEquals("sk-ant-1", vm.state.await { it.provider == Provider.ANTHROPIC && it.apiKey.isNotEmpty() }.apiKey)
    }

    @Test
    fun languageIsSaved() {
        val vm = viewModel()
        vm.selectLanguage(TutorLanguage.GERMAN)
        app.settings.language.await { it == TutorLanguage.GERMAN }
    }

    @Test
    fun testKeyUsesTheCurrentKeyAndModel() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.updateModel("claude-x")
        vm.testKey()
        assertEquals(KeyCheck.Valid, vm.state.await { it.keyCheck == KeyCheck.Valid }.keyCheck)
        assertEquals(Triple("ANTHROPIC", "sk-ant-1", "claude-x"), app.builtModels.last())
    }

    @Test
    fun aRejectedKeyShowsWhy() {
        app.tutor.keyResult = Result.failure(AppException(TutorError.InvalidKey))
        val vm = viewModel()
        vm.updateApiKey("bad")
        vm.testKey()
        assertEquals(KeyCheck.Invalid(TutorError.InvalidKey), vm.state.await { it.keyCheck is KeyCheck.Invalid }.keyCheck)
    }

    @Test
    fun editingTheKeyClearsTheOldResult() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.testKey()
        vm.state.await { it.keyCheck == KeyCheck.Valid }
        vm.updateApiKey("sk-ant-2")
        assertEquals(KeyCheck.NotChecked, vm.state.value.keyCheck)
        runBlocking { assertEquals("sk-ant-2", app.settings.apiKey(Provider.ANTHROPIC).await { it == "sk-ant-2" }) }
    }

    @Test
    fun aPassingCheckLoadsTheKeysModels() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.testKey()
        val state = vm.state.await { it.keyCheck == KeyCheck.Valid }
        assertEquals(listOf("fake-model", "fake-model-large"), state.models)
    }

    @Test
    fun pickingAListedModelKeepsTheCheckButTypingAnotherClearsIt() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.testKey()
        vm.state.await { it.keyCheck == KeyCheck.Valid }

        vm.updateModel("fake-model-large")
        assertEquals(KeyCheck.Valid, vm.state.value.keyCheck)
        vm.updateModel("something-else")
        assertEquals(KeyCheck.NotChecked, vm.state.value.keyCheck)
    }

    @Test
    fun aFailedModelListLeavesTheFieldFreeText() {
        app.tutor.modelsResult = Result.failure(AppException(TutorError.ProviderDown))
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.testKey()
        assertEquals(emptyList<String>(), vm.state.await { it.keyCheck == KeyCheck.Valid }.models)
    }

    @Test
    fun aRejectedKeyLoadsNoModels() {
        app.tutor.keyResult = Result.failure(AppException(TutorError.InvalidKey))
        val vm = viewModel()
        vm.updateApiKey("bad")
        vm.testKey()
        assertEquals(emptyList<String>(), vm.state.await { it.keyCheck is KeyCheck.Invalid }.models)
    }

    @Test
    fun editingTheKeyDropsTheModelList() {
        val vm = viewModel()
        vm.updateApiKey("sk-ant-1")
        vm.testKey()
        vm.state.await { it.models.isNotEmpty() }
        vm.updateApiKey("sk-ant-2")
        assertEquals(emptyList<String>(), vm.state.value.models)
    }

    @Test
    fun levelAndVoiceSpeedAreSaved() {
        val vm = viewModel()
        vm.selectLevel(CefrLevel.B2)
        vm.selectVoiceSpeed(VoiceSpeed.Slow)
        app.settings.level.await { it == CefrLevel.B2 }
        app.settings.voiceSpeed.await { it == VoiceSpeed.Slow }
        assertEquals(CefrLevel.B2, vm.state.value.level)
    }

    @Test
    fun theTokenTotalIsShownForTheSelectedProvider() {
        runBlocking {
            app.settings.addUsage(Provider.ANTHROPIC, TokenUsage(input = 1200, output = 300, cached = 900))
            app.settings.addUsage(Provider.OPENAI, TokenUsage(input = 5, output = 1, cached = 0))
        }
        val vm = viewModel()
        assertEquals(TokenUsage(1200, 300, 900), vm.state.value.usage)
        vm.selectProvider(Provider.OPENAI)
        assertEquals(TokenUsage(5, 1, 0), vm.state.await { it.provider == Provider.OPENAI }.usage)
    }
}
