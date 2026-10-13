package io.winstonhoyle.practicespeaking.error

import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.speech.speechErrorFor
import io.winstonhoyle.practicespeaking.ui.ErrorAction
import io.winstonhoyle.practicespeaking.ui.presentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppErrorTest {
    /** One instance of every case. Adding a case to [AppError] should add it here. */
    private val everyError: List<AppError> =
        listOf(
            TutorError.InvalidKey,
            TutorError.ModelNotFound,
            TutorError.RateLimited,
            TutorError.ProviderDown,
            TutorError.Offline,
            TutorError.Refused,
            TutorError.ContextTooLong,
            TutorError.EmptyReply,
            TutorError.Unknown("x"),
            SpeechError.NothingHeard,
            SpeechError.NoPermission,
            SpeechError.LanguageUnavailable,
            SpeechError.ServiceUnavailable,
            SpeechError.Busy,
            SpeechError.VoiceMissing,
            SpeechError.Failed(SpeechRecognizer.ERROR_CLIENT),
            StorageError.KeyUnreadable,
            StorageError.DatabaseFailed("disk full"),
        )

    @Test
    fun listCoversEverySealedCase() {
        val cases = listOf(TutorError::class, SpeechError::class, StorageError::class).flatMap { it.sealedSubclasses }
        assertEquals(cases.toSet(), everyError.map { it::class }.toSet())
    }

    @Test
    fun codesAreUniqueAndNamespaced() {
        val codes = everyError.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.all { it.matches(Regex("(TUTOR|SPEECH|STORAGE)_[A-Z0-9_]+")) })
    }

    @Test
    fun everyErrorHasAMessageAndAnAction() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        everyError.forEach { error ->
            val presentation = error.presentation()
            assertTrue(error.code, context.getString(presentation.message).isNotBlank())
            assertTrue(error.code, context.getString(presentation.action.label).isNotBlank())
        }
    }

    @Test
    fun keyAndModelProblemsSendTheLearnerToSettings() {
        listOf(TutorError.InvalidKey, TutorError.ModelNotFound, StorageError.KeyUnreadable).forEach {
            assertEquals(ErrorAction.OpenSettings, it.presentation().action)
        }
        assertEquals(ErrorAction.EndSession, TutorError.ContextTooLong.presentation().action)
    }

    @Test
    fun speechRecognizerCodesMap() {
        assertEquals(SpeechError.NothingHeard, speechErrorFor(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(SpeechError.NothingHeard, speechErrorFor(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(SpeechError.NoPermission, speechErrorFor(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(SpeechError.LanguageUnavailable, speechErrorFor(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(SpeechError.ServiceUnavailable, speechErrorFor(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(SpeechError.Busy, speechErrorFor(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(SpeechError.Failed(SpeechRecognizer.ERROR_AUDIO), speechErrorFor(SpeechRecognizer.ERROR_AUDIO))
    }

    @Test
    fun unexpectedExceptionsBecomeUnknown() {
        val result: Result<Unit> = Result.failure(IllegalStateException("boom"))
        assertEquals(TutorError.Unknown("boom"), result.appError())
    }
}
