package io.winstonhoyle.practicespeaking.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorResponseParserTest {
    @Test
    fun stressedLineIsSplitOffAndMarksAreKeptOutOfSpeech() {
        val parsed =
            TutorResponseParser.parse(
                "Приве́т! Ты идёшь в магази́н?\nSTRESSED: Я иду́ в магази́не.\nCORRECTION: в магазине → в магази́н | Accusative after a verb of motion.",
            )
        assertEquals("Приве́т! Ты идёшь в магази́н?", parsed.spokenText)
        assertEquals("Привет! Ты идёшь в магазин?", parsed.speechText)
        assertEquals("Я иду́ в магази́не.", parsed.stressed)
        assertEquals("в магази́н", parsed.correction?.right)
    }

    @Test
    fun stressedLineWorksAfterTheCorrectionToo() {
        val parsed = TutorResponseParser.parse("Хорошо́.\nCORRECTION: a → b | x\n**STRESSED:** Я до́ма.")
        assertEquals("Хорошо́.", parsed.spokenText)
        assertEquals("Я до́ма.", parsed.stressed)
    }

    @Test
    fun noCorrection() {
        val parsed = TutorResponseParser.parse("Привет! Как дела?")
        assertEquals("Привет! Как дела?", parsed.spokenText)
        assertNull(parsed.correction)
        assertNull(parsed.summary)
    }

    @Test
    fun correctionIsSplitOffAndNotSpoken() {
        val parsed =
            TutorResponseParser.parse(
                "Ты идёшь в магазин?\nCORRECTION: в магазине → в магазин | Accusative after a verb of motion.",
            )
        assertEquals("Ты идёшь в магазин?", parsed.spokenText)
        assertEquals("Ты идёшь в магазин?", parsed.speechText)
        assertEquals(Correction("в магазине", "в магазин", "Accusative after a verb of motion."), parsed.correction)
    }

    @Test
    fun asciiArrowAndExtraSpaces() {
        val parsed =
            TutorResponseParser.parse(
                "Bien.\n  CORRECTION:   je suis allé au magasin->je suis allée au magasin|Agreement with être.  ",
            )
        assertEquals(Correction("je suis allé au magasin", "je suis allée au magasin", "Agreement with être."), parsed.correction)
    }

    @Test
    fun markdownNoiseAroundTheLabel() {
        val parsed =
            TutorResponseParser.parse(
                "**Gut!** Und dann?\n**CORRECTION:** ich habe gegangen → ich bin gegangen | gehen takes sein.",
            )
        assertEquals(Correction("ich habe gegangen", "ich bin gegangen", "gehen takes sein."), parsed.correction)
        assertEquals("Gut! Und dann?", parsed.speechText)
    }

    @Test
    fun correctionWithoutExplanation() {
        assertEquals(Correction("a", "b", null), TutorResponseParser.parse("x\nCORRECTION: a → b").correction)
    }

    @Test
    fun malformedCorrectionKeepsTheRawText() {
        val parsed = TutorResponseParser.parse("Hola.\nCORRECTION: something odd")
        assertEquals("Hola.", parsed.spokenText)
        assertNull(parsed.correction)
        assertEquals("something odd", parsed.rawCorrection)
    }

    @Test
    fun summaryIsExtracted() {
        val parsed =
            TutorResponseParser.parse(
                "Пока! До встречи.\nSUMMARY\nMistakes: в магазине → в магазин\nNew words: хлеб — bread\nTip: Practise the accusative.",
            )
        assertEquals("Пока! До встречи.", parsed.spokenText)
        assertEquals(Summary("в магазине → в магазин", "хлеб — bread", "Practise the accusative."), parsed.summary)
    }

    @Test
    fun summaryWithMarkdownAndListItems() {
        val parsed =
            TutorResponseParser.parse(
                "Tchau!\n**SUMMARY**\n**Mistakes:**\n- eu sou cansado → eu estou cansado\n- no Brasil → em o Brasil\n**New words:** ônibus — bus\n**Tip:** Review ser vs. estar.",
            )
        assertEquals("eu sou cansado → eu estou cansado; no Brasil → em o Brasil", parsed.summary?.mistakes)
        assertEquals("ônibus — bus", parsed.summary?.newWords)
        assertEquals("Review ser vs. estar.", parsed.summary?.tip)
    }

    @Test
    fun malformedSummaryShowsTextAsIs() {
        val parsed = TutorResponseParser.parse("Adiós.\nSUMMARY\nno fields here")
        assertEquals("Adiós.", parsed.spokenText)
        assertNull(parsed.summary)
    }

    @Test
    fun emojiAndMarkdownAreStrippedForSpeech() {
        assertEquals("Super! Was machst du heute?", TutorResponseParser.forSpeech("**Super!** 🎉 Was machst du _heute_?"))
    }

    @Test
    fun storedCorrectionReadsAsOneLine() {
        val stored = "в магазине → в магазин | Motion toward a place takes the accusative."
        assertEquals(
            "в магазине → в магазин (Motion toward a place takes the accusative.)",
            TutorResponseParser.parseCorrection(stored)?.display(),
        )
        assertEquals("ich bin → ich habe", TutorResponseParser.parseCorrection("ich bin -> ich habe")?.display())
    }

    @Test
    fun emptyReplyDoesNotCrash() {
        assertEquals("", TutorResponseParser.parse("").spokenText)
    }
}
