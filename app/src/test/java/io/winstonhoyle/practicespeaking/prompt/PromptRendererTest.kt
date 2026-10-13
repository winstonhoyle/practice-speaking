package io.winstonhoyle.practicespeaking.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PromptRendererTest {
    private val renderer = PromptRenderer(File("src/main/assets/${PromptRenderer.TEMPLATE_ASSET}").readText())

    @Test
    fun russianMatchesTheOriginalPromptExactly() {
        // The hand-written Russian prompt the template was generalized from, kept as a test fixture.
        val original = javaClass.getResource("/russian_tutor_system_prompt.md")!!.readText()
        assertEquals(original, renderer.render(TutorLanguage.RUSSIAN))
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
}
