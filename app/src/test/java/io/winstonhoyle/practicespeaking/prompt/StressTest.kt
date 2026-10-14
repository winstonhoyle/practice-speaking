package io.winstonhoyle.practicespeaking.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StressTest {
    @Test
    fun stripRemovesOnlyTheMarks() {
        assertEquals("Привет, магазин!", Stress.strip("Приве́т, магази́н!"))
    }

    @Test
    fun aStressedCopyOfTheSameWordsIsAccepted() {
        assertEquals("Я иду́ в магази́н.", Stress.matchOrNull("я иду в магазин", "«Я иду́ в магази́н.»"))
    }

    @Test
    fun yoAndPunctuationMayDiffer() {
        assertEquals("Всё хорошо́!", Stress.matchOrNull("все хорошо", "Всё хорошо́!"))
    }

    @Test
    fun rewrittenWordsAreRejected() {
        assertNull(Stress.matchOrNull("Я иду в магазине", "Я иду́ в магази́н."))
    }

    @Test
    fun aCopyWithoutMarksIsRejected() {
        assertNull(Stress.matchOrNull("Я дома", "Я дома"))
    }
}
