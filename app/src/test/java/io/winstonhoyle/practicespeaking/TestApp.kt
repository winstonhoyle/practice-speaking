package io.winstonhoyle.practicespeaking

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.ai.FakeTutorModel
import io.winstonhoyle.practicespeaking.data.FakeCipher
import io.winstonhoyle.practicespeaking.data.db.AppDatabase
import io.winstonhoyle.practicespeaking.data.db.SessionRepository
import io.winstonhoyle.practicespeaking.data.settings.SettingsRepository
import io.winstonhoyle.practicespeaking.prompt.PromptRenderer
import io.winstonhoyle.practicespeaking.speech.Speaker
import io.winstonhoyle.practicespeaking.speech.VoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files

/** Records what would have been spoken. */
class FakeSpeaker : Speaker {
    override val isSpeaking = MutableStateFlow(false)
    override val voiceStatus = MutableStateFlow(VoiceStatus.Ready)
    val spoken = mutableListOf<String>()
    var localeTag: String? = null
    var stops = 0

    override fun setLanguage(localeTag: String) {
        this.localeTag = localeTag
    }

    override fun speak(text: String) {
        spoken += text
    }

    override fun stop() {
        stops++
    }
}

/**
 * Real repositories on an in-memory database and a temp DataStore, with a fake tutor and speaker.
 * Needs Robolectric for the Room context.
 */
class TestApp {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir: File = Files.createTempDirectory("ps-test").toFile()
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    val cipher = FakeCipher()
    val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }, cipher)
    val sessions = SessionRepository(db.sessionDao())
    val prompts = PromptRenderer("You are {{TUTOR_NAME}}, a {{LANGUAGE_NAME}} tutor.")
    val speaker = FakeSpeaker()
    val tutor = FakeTutorModel()
    val builtModels = mutableListOf<Triple<String, String, String>>()
    val tutorModels: TutorModelBuilder = { provider, key, model ->
        builtModels += Triple(provider.name, key, model)
        tutor
    }

    fun close() {
        db.close()
        scope.cancel()
        dir.deleteRecursively()
    }
}

/**
 * Waits (up to 5 s) for a flow to emit a value matching [predicate]. It polls with a fresh read
 * each time: a DataStore collector that starts while a write is in flight can miss that write.
 */
fun <T> Flow<T>.await(predicate: (T) -> Boolean): T =
    runBlocking {
        withTimeout(5_000) {
            var value = first()
            while (!predicate(value)) {
                delay(10)
                value = first()
            }
            value
        }
    }
