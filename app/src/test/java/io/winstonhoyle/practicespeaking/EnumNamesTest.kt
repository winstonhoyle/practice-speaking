package io.winstonhoyle.practicespeaking

import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.cefrLevel
import io.winstonhoyle.practicespeaking.data.db.providerOrNull
import io.winstonhoyle.practicespeaking.data.db.tutorLanguage
import io.winstonhoyle.practicespeaking.prompt.CefrLevel
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import org.junit.Assert.assertEquals
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
}
