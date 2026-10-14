package io.winstonhoyle.practicespeaking.speech

import kotlinx.coroutines.flow.StateFlow

enum class VoiceStatus {
    /** The TTS engine is still starting. */
    Loading,
    Ready,

    /** The engine works but has no voice data for this language. */
    MissingData,

    /** No TTS engine at all. */
    Unavailable,
}

/** Reads tutor replies aloud. The ViewModel talks to this, not to TextToSpeech. */
interface Speaker {
    val isSpeaking: StateFlow<Boolean>
    val voiceStatus: StateFlow<VoiceStatus>

    fun setLanguage(localeTag: String)

    /** Speech rate for everything spoken after this call; 1.0 is the engine's normal speed. */
    fun setRate(rate: Float)

    fun speak(text: String)

    fun stop()
}
