package io.winstonhoyle.practicespeaking.ui

import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationTogglesTest {
    private val errors = mutableListOf<AppError>()
    private val toggles = TranslationToggles(CoroutineScope(Dispatchers.Unconfined)) { errors += it }
    private var fetches = 0

    private fun fetch(result: Result<String>): suspend () -> Result<String> =
        {
            fetches++
            result
        }

    @Test
    fun theFirstTapFetchesAndShowsLaterTapsOnlyToggle() {
        toggles.toggle(1, saved = null, fetch(Result.success("Hello")))
        assertEquals("Hello", toggles.state.value.visible(1))
        toggles.toggle(1, saved = null, fetch(Result.success("again?")))
        assertNull(toggles.state.value.visible(1))
        toggles.toggle(1, saved = null, fetch(Result.success("again?")))
        assertEquals("Hello", toggles.state.value.visible(1))
        assertEquals(1, fetches)
    }

    @Test
    fun aSavedTranslationIsShownWithoutFetching() {
        toggles.toggle(2, saved = "Saved", fetch(Result.success("new")))
        assertEquals("Saved", toggles.state.value.visible(2, saved = "Saved"))
        assertEquals(0, fetches)
    }

    @Test
    fun aFailedFetchStaysHiddenAndReportsTheError() {
        toggles.toggle(3, saved = null, fetch(Result.failure(AppException(TutorError.Offline))))
        assertNull(toggles.state.value.visible(3))
        assertEquals(emptySet<Long>(), toggles.state.value.loading)
        assertEquals(listOf<AppError>(TutorError.Offline), errors)
    }

    @Test
    fun rememberedTranslationsStartHidden() {
        toggles.remember(mapOf(4L to "Remembered"))
        assertNull(toggles.state.value.visible(4))
        toggles.toggle(4, saved = null, fetch(Result.success("x")))
        assertEquals("Remembered", toggles.state.value.visible(4))
        assertEquals(0, fetches)
    }
}
