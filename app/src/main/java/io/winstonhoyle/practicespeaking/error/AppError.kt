package io.winstonhoyle.practicespeaking.error

/**
 * Every failure the learner can see. Each case has a stable [code] for bug reports and logs;
 * `ui/Errors.kt` turns it into a message and one action. The hierarchy is sealed, so adding a
 * case doesn't compile until it has a message.
 */
sealed interface AppError {
    val code: String
}

/** Failures talking to the AI provider. Every `TutorModel` maps its SDK's exceptions to these. */
sealed interface TutorError : AppError {
    /** 401/403: the key is wrong, revoked, or lacks access. */
    data object InvalidKey : TutorError {
        override val code = "TUTOR_INVALID_KEY"
    }

    /** 404 on the model: the model ID in Settings doesn't exist for this key. */
    data object ModelNotFound : TutorError {
        override val code = "TUTOR_MODEL_NOT_FOUND"
    }

    /** 429. */
    data object RateLimited : TutorError {
        override val code = "TUTOR_RATE_LIMITED"
    }

    /** 5xx, including overload. */
    data object ProviderDown : TutorError {
        override val code = "TUTOR_PROVIDER_DOWN"
    }

    /** The request never reached the provider. */
    data object Offline : TutorError {
        override val code = "TUTOR_OFFLINE"
    }

    /** A safety refusal or block. */
    data object Refused : TutorError {
        override val code = "TUTOR_REFUSED"
    }

    /** The conversation no longer fits the model's context window. Only a new session helps. */
    data object ContextTooLong : TutorError {
        override val code = "TUTOR_CONTEXT_TOO_LONG"
    }

    /** The provider answered with no text. */
    data object EmptyReply : TutorError {
        override val code = "TUTOR_EMPTY_REPLY"
    }

    /** Anything the provider classes couldn't classify. [message] is shown with the code for bug reports. */
    data class Unknown(val message: String) : TutorError {
        override val code = "TUTOR_UNKNOWN"
    }
}

/** Failures from on-device speech recognition and text-to-speech. */
sealed interface SpeechError : AppError {
    /** Recognition ended without words: silence, or nothing it could match. */
    data object NothingHeard : SpeechError {
        override val code = "SPEECH_NOTHING_HEARD"
    }

    /** The microphone permission was denied. */
    data object NoPermission : SpeechError {
        override val code = "SPEECH_NO_PERMISSION"
    }

    /** The recognizer has no model for this language. */
    data object LanguageUnavailable : SpeechError {
        override val code = "SPEECH_LANGUAGE_UNAVAILABLE"
    }

    /** The recognizer needs a network connection or its server failed. */
    data object ServiceUnavailable : SpeechError {
        override val code = "SPEECH_SERVICE_UNAVAILABLE"
    }

    /** Another app (or a previous press) is still using the recognizer. */
    data object Busy : SpeechError {
        override val code = "SPEECH_BUSY"
    }

    /** No voice data is installed for this language, so replies stay silent. */
    data object VoiceMissing : SpeechError {
        override val code = "SPEECH_VOICE_MISSING"
    }

    /** Any other recognizer error. [reason] is Android's `SpeechRecognizer.ERROR_*` value. */
    data class Failed(val reason: Int) : SpeechError {
        override val code = "SPEECH_FAILED_$reason"
    }
}

/** Failures reading or writing data on the device. */
sealed interface StorageError : AppError {
    /** A key is saved but can't be decrypted, usually after a backup restore or a Keystore reset. */
    data object KeyUnreadable : StorageError {
        override val code = "STORAGE_KEY_UNREADABLE"
    }

    /** The database rejected a read or write. */
    data class DatabaseFailed(val message: String) : StorageError {
        override val code = "STORAGE_DATABASE_FAILED"
    }
}

/** Carries an [AppError] through [Result] and coroutines. */
class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.code, cause)

/** The [AppError] behind a failed [Result], or null when it succeeded. Unexpected exceptions become [TutorError.Unknown]. */
fun Result<*>.appError(): AppError? = exceptionOrNull()?.let { (it as? AppException)?.error ?: TutorError.Unknown(it.message.orEmpty()) }
