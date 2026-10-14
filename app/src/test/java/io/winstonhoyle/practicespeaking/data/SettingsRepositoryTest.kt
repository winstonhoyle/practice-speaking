package io.winstonhoyle.practicespeaking.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.TokenUsage
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository(): Pair<SettingsRepository, DataStore<Preferences>> {
        val store =
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                folder.newFile("settings.preferences_pb").also { it.delete() }
            }
        return SettingsRepository(store, FakeCipher()) to store
    }

    @Test
    fun defaults() =
        runTest {
            val (repo) = repository()
            assertEquals(Provider.ANTHROPIC, repo.provider.first())
            assertEquals(TutorLanguage.RUSSIAN, repo.language.first())
            assertNull(repo.apiKey(Provider.ANTHROPIC).first())
            assertEquals(Provider.OPENAI.defaultModel, repo.model(Provider.OPENAI).first())
        }

    @Test
    fun keysAreStoredPerProviderAndEncrypted() =
        runTest {
            val (repo, store) = repository()
            repo.setApiKey(Provider.ANTHROPIC, " sk-ant-one ")
            repo.setApiKey(Provider.GEMINI, "AIza-two")
            repo.setProvider(Provider.GEMINI)

            assertEquals("sk-ant-one", repo.apiKey(Provider.ANTHROPIC).first())
            assertEquals("AIza-two", repo.apiKey(Provider.GEMINI).first())
            assertNull(repo.apiKey(Provider.OPENAI).first())
            val raw = store.data.first()[stringPreferencesKey("api_key_ANTHROPIC")]
            assertNotEquals("sk-ant-one", raw)
        }

    @Test
    fun blankKeyRemovesIt() =
        runTest {
            val (repo) = repository()
            repo.setApiKey(Provider.OPENAI, "sk-x")
            repo.setApiKey(Provider.OPENAI, "  ")
            assertNull(repo.apiKey(Provider.OPENAI).first())
        }

    @Test
    fun modelOverrideAndReset() =
        runTest {
            val (repo) = repository()
            repo.setModel(Provider.ANTHROPIC, "claude-sonnet-5-5")
            assertEquals("claude-sonnet-5-5", repo.model(Provider.ANTHROPIC).first())
            assertEquals(Provider.GEMINI.defaultModel, repo.model(Provider.GEMINI).first())
            repo.setModel(Provider.ANTHROPIC, "")
            assertEquals(Provider.ANTHROPIC.defaultModel, repo.model(Provider.ANTHROPIC).first())
        }

    @Test
    fun language() =
        runTest {
            val (repo) = repository()
            repo.setLanguage(TutorLanguage.SPANISH)
            assertEquals(TutorLanguage.SPANISH, repo.language.first())
        }

    @Test
    fun tokenTotalsAddUpPerProvider() =
        runTest {
            val (repo) = repository()
            repo.addUsage(Provider.ANTHROPIC, TokenUsage(input = 100, output = 20, cached = 80))
            repo.addUsage(Provider.ANTHROPIC, TokenUsage(input = 50, output = 10, cached = 0))
            repo.addUsage(Provider.OPENAI, TokenUsage(input = 7, output = 3, cached = 0))
            assertEquals(TokenUsage(150, 30, 80), repo.usage(Provider.ANTHROPIC).first())
            assertEquals(TokenUsage(7, 3, 0), repo.usage(Provider.OPENAI).first())
            assertEquals(TokenUsage(), repo.usage(Provider.GEMINI).first())
        }

    @Test
    fun levelAndVoiceSpeedDefaultThenPersist() =
        runTest {
            val (repo) = repository()
            assertEquals(CefrLevel.A2, repo.level.first())
            assertEquals(VoiceSpeed.Normal, repo.voiceSpeed.first())
            repo.setLevel(CefrLevel.C1)
            repo.setVoiceSpeed(VoiceSpeed.Slow)
            assertEquals(CefrLevel.C1, repo.level.first())
            assertEquals(VoiceSpeed.Slow, repo.voiceSpeed.first())
        }
}
