package io.winstonhoyle.practicespeaking.ui

import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.appError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which messages show their English translation, which are being fetched, and the ones fetched so far. */
data class TranslationState(
    val shown: Set<Long> = emptySet(),
    val loading: Set<Long> = emptySet(),
    val texts: Map<Long, String> = emptyMap(),
) {
    /** The translation to display for [messageId], or null while it is hidden or unknown. */
    fun visible(messageId: Long, saved: String? = null): String? = (texts[messageId] ?: saved)?.takeIf { messageId in shown }
}

/**
 * The translate button's behavior, shared by the conversation and saved-session screens: show or
 * hide a known translation, or fetch it once. Failures go to [onError].
 */
class TranslationToggles(private val scope: CoroutineScope, private val onError: (AppError) -> Unit) {
    private val _state = MutableStateFlow(TranslationState())
    val state: StateFlow<TranslationState> = _state

    /** Translations already saved on messages, e.g. when a session is continued. They start hidden. */
    fun remember(texts: Map<Long, String>) = _state.update { it.copy(texts = it.texts + texts) }

    /** [saved] is a translation already stored on the message; [fetch] asks for one when there is none. */
    fun toggle(messageId: Long, saved: String?, fetch: suspend () -> Result<String>) {
        val current = _state.value
        if (messageId in current.loading) return
        if (saved != null || messageId in current.texts) {
            _state.update { it.copy(shown = if (messageId in it.shown) it.shown - messageId else it.shown + messageId) }
            return
        }
        _state.update { it.copy(loading = it.loading + messageId) }
        scope.launch {
            val result = fetch()
            val text = result.getOrNull()
            _state.update {
                it.copy(
                    loading = it.loading - messageId,
                    texts = if (text != null) it.texts + (messageId to text) else it.texts,
                    shown = if (text != null) it.shown + messageId else it.shown,
                )
            }
            result.appError()?.let(onError)
        }
    }
}
