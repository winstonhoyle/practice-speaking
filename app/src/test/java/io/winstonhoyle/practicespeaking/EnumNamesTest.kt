package io.winstonhoyle.practicespeaking

import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.db.MessageEntity
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.cefrLevel
import io.winstonhoyle.practicespeaking.data.db.displayText
import io.winstonhoyle.practicespeaking.data.db.isUser
import io.winstonhoyle.practicespeaking.data.db.providerOrNull
import io.winstonhoyle.practicespeaking.data.db.summary
import io.winstonhoyle.practicespeaking.data.db.tutorLanguage
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EnumNamesTest {
    @Test
    fun knownNamesResolveAndUnknownOnesAreNull() {
        assertEquals(Provider.GEMINI, enumByName<Provider>("GEMINI"))
        assertNull(enumByName<Provider>("MISTRAL"))
        assertNull(enumByName<Provider>(null))
    }

    @Test
    fun unknownSessionValuesGetSafeDefaults() {
        val odd = SessionEntity(language = "KLINGON", provider = "MISTRAL", model = "m", startedAt = 0, level = null)
        assertEquals(TutorLanguage.RUSSIAN, odd.tutorLanguage)
        assertNull(odd.providerOrNull)
        assertEquals(CefrLevel.DEFAULT, odd.cefrLevel)
        assertEquals(CefrLevel.C1, odd.copy(level = "C1").cefrLevel)
    }

    @Test
    fun summaryIsNullUntilTheTutorGivesOne() {
        val session = SessionEntity(language = "RUSSIAN", provider = "GEMINI", model = "m", startedAt = 0)
        assertNull(session.summary)
        assertNull(session.copy(summaryTip = " ").summary)
        assertEquals(Summary(null, null, "Read aloud"), session.copy(summaryTip = "Read aloud").summary)
    }

    @Test
    fun messagesShowTheStressedCopyWhenThereIsOne() {
        val tutor = MessageEntity(sessionId = 1, role = MessageEntity.ROLE_ASSISTANT, text = "мама", createdAt = 0)
        assertFalse(tutor.isUser)
        assertEquals("мама", tutor.displayText)
        assertEquals("ма́ма", tutor.copy(stressed = "ма́ма").displayText)
    }
}
