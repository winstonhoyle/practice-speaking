package io.winstonhoyle.practicespeaking.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class HeldUtteranceTest {
    @Test
    fun segmentsAreJoinedWithSingleSpaces() {
        val utterance = HeldUtterance()
        utterance.add(" Вчера я ")
        utterance.add("")
        utterance.add("ходил в магазин")
        assertEquals("Вчера я ходил в магазин", utterance.text())
    }

    @Test
    fun thePartialFollowsWhatWasAlreadyHeard() {
        val utterance = HeldUtterance()
        utterance.add("Вчера я")
        assertEquals("Вчера я ходил", utterance.withPartial("ходил"))
        assertEquals("Вчера я", utterance.withPartial(" "))
    }

    @Test
    fun clearStartsANewHold() {
        val utterance = HeldUtterance().apply { add("раз") }
        utterance.clear()
        assertEquals("", utterance.text())
    }
}
