package io.winstonhoyle.practicespeaking.speech

import android.speech.SpeechRecognizer
import io.winstonhoyle.practicespeaking.error.SpeechError

/** Maps Android's `SpeechRecognizer.ERROR_*` codes to the app's [SpeechError]s. */
fun speechErrorFor(reason: Int): SpeechError =
    when (reason) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NothingHeard

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.NoPermission

        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.LanguageUnavailable

        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
        -> SpeechError.ServiceUnavailable

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> SpeechError.Busy

        else -> SpeechError.Failed(reason)
    }
