package io.winstonhoyle.practicespeaking.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import io.winstonhoyle.practicespeaking.error.SpeechError

/**
 * Hold-to-talk on Android SpeechRecognizer. Call [start] on press and [stop] on release; the
 * whole utterance arrives through [Listener.onFinal] after release.
 *
 * The recognizer ends a segment by itself after a short silence. While the button is held, a
 * finished or silent segment starts the next one, and the pieces are joined, so the learner can
 * pause to think. Create it on the main thread and [destroy] it with the screen.
 */
class SpeechInput(context: Context, private val listener: Listener) {
    interface Listener {
        /** Everything heard so far in this hold, including the segment still in progress. */
        fun onPartial(text: String)

        fun onFinal(text: String)

        /** Recognition ended without text: silence, no permission, a missing language, or an engine error. */
        fun onFailed(error: SpeechError)
    }

    private val recognizer: SpeechRecognizer? =
        if (isAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    private val handler = Handler(Looper.getMainLooper())
    private val utterance = HeldUtterance()
    private var localeTag = ""

    /** The button is down. */
    private var holding = false

    /** A segment is running and its result or error is still to come. */
    private var listening = false

    init {
        recognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults.firstResult()?.let { listener.onPartial(utterance.withPartial(it)) }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    Log.d(TAG, "segment result, holding=$holding")
                    results.firstResult()?.let(utterance::add)
                    if (holding) restartSoon() else finish()
                }

                override fun onError(error: Int) {
                    listening = false
                    Log.d(TAG, "segment error $error, holding=$holding")
                    when {
                        holding && error in KEEP_LISTENING -> restartSoon()

                        // Released: keep what was heard, even if the last segment failed.
                        !holding && (error in END_OF_HOLD || utterance.text().isNotEmpty()) -> finish()

                        else -> fail(speechErrorFor(error))
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
    }

    val available: Boolean get() = recognizer != null

    fun start(localeTag: String) {
        this.localeTag = localeTag
        utterance.clear()
        holding = true
        listen()
    }

    /** The button is released: finish the current segment, then deliver everything heard. */
    fun stop() {
        if (!holding) return
        holding = false
        handler.removeCallbacksAndMessages(null)
        if (listening) recognizer?.stopListening() else finish()
    }

    fun cancel() {
        holding = false
        listening = false
        handler.removeCallbacksAndMessages(null)
        utterance.clear()
        recognizer?.cancel()
    }

    fun destroy() {
        cancel()
        recognizer?.destroy()
    }

    private fun listen() {
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Hints only: engines may ignore them, which is why held segments restart.
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, PAUSE_MILLIS)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, PAUSE_MILLIS)
            }
        listening = true
        recognizer?.startListening(intent)
    }

    /** The engine needs a moment between segments, or it reports itself busy. */
    private fun restartSoon() {
        handler.postDelayed({ if (holding) listen() }, RESTART_DELAY_MILLIS)
    }

    private fun finish() {
        val text = utterance.text()
        utterance.clear()
        if (text.isBlank()) listener.onFailed(SpeechError.NothingHeard) else listener.onFinal(text)
    }

    private fun fail(error: SpeechError) {
        holding = false
        utterance.clear()
        listener.onFailed(error)
    }

    private fun Bundle?.firstResult(): String? =
        this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "SpeechInput"
        private const val PAUSE_MILLIS = 4_000L
        private const val RESTART_DELAY_MILLIS = 100L

        /** Silence or a busy engine while the button is held: start the next segment. */
        private val KEEP_LISTENING =
            setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

        /** What engines report when a segment is stopped on release with nothing new in it. */
        private val END_OF_HOLD =
            setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT)

        fun isAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    }
}

/** The segments of one hold-to-talk utterance, joined with single spaces. */
internal class HeldUtterance {
    private val segments = mutableListOf<String>()

    fun add(segment: String) {
        segment.trim().takeIf { it.isNotEmpty() }?.let(segments::add)
    }

    fun withPartial(partial: String): String = join(segments + partial.trim())

    fun text(): String = join(segments)

    fun clear() = segments.clear()

    private fun join(parts: List<String>) = parts.filter { it.isNotEmpty() }.joinToString(" ")
}
