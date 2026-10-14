package io.winstonhoyle.practicespeaking

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.winstonhoyle.practicespeaking.ai.CountingTutorModel
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.ai.TutorModel
import io.winstonhoyle.practicespeaking.ai.TutorModelFactory
import io.winstonhoyle.practicespeaking.data.MessageTranslator
import io.winstonhoyle.practicespeaking.data.db.AppDatabase
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.settings.KeyStoreCipher
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.speech.Speaker
import io.winstonhoyle.practicespeaking.speech.SpeechOutput

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Builds a [TutorModel] from a provider, key, and model ID. */
typealias TutorModelBuilder = (provider: Provider, apiKey: String, model: String) -> TutorModel

/** Manual dependency injection: one instance of each app-wide object. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val settings = SettingsRepository(app.settingsStore, KeyStoreCipher())
    val sessions = SessionRepository(AppDatabase.create(app).sessionDao())
    val prompts: PromptRenderer by lazy { PromptRenderer.fromAssets(app) }
    val speaker: Speaker by lazy { SpeechOutput(app) }

    /** Every model the app builds counts its tokens into [SettingsRepository.usage]. */
    val tutorModels: TutorModelBuilder = { provider, key, model ->
        CountingTutorModel(TutorModelFactory.create(provider, key, model), settings::addUsage)
    }
    val translator: MessageTranslator by lazy { MessageTranslator(sessions, prompts) }
}
