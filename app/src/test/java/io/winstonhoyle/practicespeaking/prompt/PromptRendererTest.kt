package io.winstonhoyle.practicespeaking.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PromptRendererTest {
    private val renderer = PromptRenderer(File("src/main/assets/${PromptRenderer.TEMPLATE_ASSET}").readText())

    @Test
    fun russianAtTheDefaultLevelMatchesTheSnapshot() {
        // The Russian A2 prompt, byte for byte. A change here is a change to what the tutor is told: review it, then update the file.
        val snapshot = javaClass.getResource("/russian_tutor_system_prompt.md")!!.readText()
        assertEquals(snapshot, renderer.render(TutorLanguage.RUSSIAN, CefrLevel.A2))
    }

    @Test
    fun everyLevelPutsItsCodeAndRuleInThePrompt() {
        CefrLevel.entries.forEach { level ->
            val prompt = renderer.render(TutorLanguage.FRENCH, level)
            assertTrue(level.name, "CEFR level ${level.name} (${level.label})" in prompt)
            assertTrue(level.name, level.promptRule in prompt)
            assertFalse(level.name, "{{" in prompt)
        }
    }

    @Test
    fun simplificationsStopFromC1() {
        assertTrue("Avoid participles" in renderer.render(TutorLanguage.RUSSIAN, CefrLevel.B2))
        assertFalse("Avoid participles" in renderer.render(TutorLanguage.RUSSIAN, CefrLevel.C1))
    }

    @Test
    fun onlyRussianAsksForStressMarks() {
        TutorLanguage.entries.forEach { language ->
            assertEquals(language.name, language == TutorLanguage.RUSSIAN, "STRESSED:" in renderer.render(language))
        }
    }

    @Test
    fun theTranslationPromptNamesTheLanguageAndAsksForEnglishOnly() {
        val prompt = renderer.translationPrompt(TutorLanguage.GERMAN)
        assertTrue("German" in prompt && "English" in prompt && "translation only" in prompt)
    }

    @Test
    fun everyLanguageFillsEveryPlaceholder() {
        TutorLanguage.entries.forEach { language ->
            val prompt = renderer.render(language)
            assertFalse(language.name, "{{" in prompt)
            assertTrue(language.name, prompt.startsWith("You are ${language.tutorName}, a friendly ${language.displayName}"))
        }
    }

    @Test
    fun parserFormatsAreTheSameInEveryLanguage() {
        TutorLanguage.entries.forEach { language ->
            val prompt = renderer.render(language)
            assertTrue("CORRECTION: <what the learner said> → <correct form> | <short English explanation, under 15 words>" in prompt)
            assertTrue("  SUMMARY\n  Mistakes:" in prompt)
            assertTrue("New words:" in prompt && "Tip:" in prompt)
            assertTrue("1–3 sentences" in prompt)
        }
    }

    @Test
    fun renderingIsDeterministic() {
        assertEquals(renderer.render(TutorLanguage.SPANISH), renderer.render(TutorLanguage.SPANISH))
    }

    @Test
    fun spanishIsMexicanWithLucia() {
        val prompt = renderer.render(TutorLanguage.SPANISH)
        assertTrue("You are Lucía" in prompt)
        assertTrue("Mexican" in prompt)
        assertFalse("stress marks" in prompt)
    }

    @Test
    fun theRegionalVariantAppliesAtEveryLevel() {
        listOf(CefrLevel.A1, CefrLevel.C2).forEach { level ->
            assertTrue("Mexican Spanish" in renderer.render(TutorLanguage.SPANISH, level))
            assertTrue("Brazilian Portuguese" in renderer.render(TutorLanguage.PORTUGUESE, level))
            assertTrue("used in Germany (Hochdeutsch)" in renderer.render(TutorLanguage.GERMAN, level))
        }
    }

    @Test
    fun germanIsHansAndRussianIsKristina() {
        assertTrue(renderer.render(TutorLanguage.GERMAN).startsWith("You are Hans, a friendly German"))
        assertTrue(renderer.render(TutorLanguage.RUSSIAN).startsWith("You are Kristina, a friendly Russian"))
    }
}
