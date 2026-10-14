package io.winstonhoyle.practicespeaking.speech

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.error.SpeechError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSpeechRecognizer
import java.time.Duration

/** Hold-to-talk against Robolectric's recognizer: pauses while the button is held must not end the utterance. */
@RunWith(RobolectricTestRunner::class)
class SpeechInputTest {
    private val events = mutableListOf<String>()
    private lateinit var input: SpeechInput

    private val shadow: ShadowSpeechRecognizer get() = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

    @Before
    fun setUp() {
        // SpeechRecognizer is only available when a recognition service is installed.
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val service =
            ResolveInfo().apply {
                serviceInfo =
                    ServiceInfo().apply {
                        packageName = "com.example.speech"
                        name = "Recognizer"
                    }
            }
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(RecognitionService.SERVICE_INTERFACE), service)
        shadowOf(app.packageManager).addServiceIfNotPresent(ComponentName("com.example.speech", "Recognizer"))
        input =
            SpeechInput(
                app,
                object : SpeechInput.Listener {
                    override fun onPartial(text: String) {
                        events += "partial:$text"
                    }

                    override fun onFinal(text: String) {
                        events += "final:$text"
                    }

                    override fun onFailed(error: SpeechError) {
                        events += "failed:${error.code}"
                    }
                },
            )
        input.start("ru-RU")
        idle()
    }

    @Test
    fun aPauseWhileHoldingStartsTheNextSegmentAndTheTextIsJoinedOnRelease() {
        val firstIntent = shadow.lastRecognizerIntent
        shadow.triggerOnResults(results("Вчера я"))
        idle()
        assertNotSame("a new segment should start", firstIntent, shadow.lastRecognizerIntent)

        shadow.triggerOnPartialResults(results("ходил"))
        input.stop()
        shadow.triggerOnResults(results("ходил в магазин"))

        assertEquals(listOf("partial:Вчера я ходил", "final:Вчера я ходил в магазин"), events)
    }

    @Test
    fun silenceWhileHoldingIsNotAnError() {
        val firstIntent = shadow.lastRecognizerIntent
        shadow.triggerOnError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        idle()
        assertNotSame(firstIntent, shadow.lastRecognizerIntent)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun releasingWithNothingSaidReportsNothingHeard() {
        input.stop()
        shadow.triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(listOf("failed:${SpeechError.NothingHeard.code}"), events)
    }

    @Test
    fun aRealErrorEndsTheHold() {
        shadow.triggerOnError(SpeechRecognizer.ERROR_NETWORK)
        assertEquals(1, events.size)
        assertEquals(true, events.single().startsWith("failed:"))
    }

    private fun results(text: String) = Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
}
