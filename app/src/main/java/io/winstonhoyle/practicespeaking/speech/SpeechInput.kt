package io.winstonhoyle.practicespeaking.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import io.winstonhoyle.practicespeaking.error.SpeechError

/**
 * Hold-to-talk on Android SpeechRecognizer. Call [start] on press and [stop] on release;
 * results arrive through [Listener]. Create it on the main thread and [destroy] it with the screen.
 */
class SpeechInput(context: Context, private val listener: Listener) {
    interface Listener {
        fun onPartial(text: String)

        fun onFinal(text: String)

        /** Recognition ended without text: silence, no permission, a missing language, or an engine error. */
        fun onFailed(error: SpeechError)
    }

    private val recognizer: SpeechRecognizer? =
        if (isAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    init {
        recognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults.firstResult()?.let(listener::onPartial)
                }

                override fun onResults(results: Bundle?) {
                    val text = results.firstResult()
                    if (text.isNullOrBlank()) listener.onFailed(SpeechError.NothingHeard) else listener.onFinal(text)
                }

                override fun onError(error: Int) = listener.onFailed(speechErrorFor(error))

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
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        recognizer?.startListening(intent)
    }

    /** Stops listening; the final result still arrives afterwards. */
    fun stop() = recognizer?.stopListening()

    fun cancel() = recognizer?.cancel()

    fun destroy() = recognizer?.destroy()

    private fun Bundle?.firstResult(): String? = this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    companion object {
        fun isAvailable(context: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    }
}
