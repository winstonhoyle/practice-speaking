package io.winstonhoyle.practicespeaking.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * [Speaker] on Android TextToSpeech. The engine starts asynchronously, so the language set
 * before it is ready is applied once it is. One instance lives for the whole app.
 */
class SpeechOutput(context: Context) : Speaker {
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _voiceStatus = MutableStateFlow(VoiceStatus.Loading)
    override val voiceStatus: StateFlow<VoiceStatus> = _voiceStatus

    private var ready = false
    private var localeTag: String? = null
    private var utteranceId = 0

    private val tts: TextToSpeech =
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                localeTag?.let(::applyLanguage)
            } else {
                _voiceStatus.value = VoiceStatus.Unavailable
            }
        }.apply {
            setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Required override on older APIs")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                },
            )
        }

    override fun setLanguage(localeTag: String) {
        this.localeTag = localeTag
        if (ready) applyLanguage(localeTag)
    }

    private fun applyLanguage(tag: String) {
        val result = tts.setLanguage(Locale.forLanguageTag(tag))
        _voiceStatus.value =
            when (result) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> VoiceStatus.MissingData
                else -> VoiceStatus.Ready
            }
    }

    override fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply-${utteranceId++}")
    }

    override fun stop() {
        if (ready) tts.stop()
        _isSpeaking.value = false
    }
}
