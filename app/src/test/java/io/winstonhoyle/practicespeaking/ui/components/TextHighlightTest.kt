package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Test

class TextHighlightTest {
    private val underline = SpanStyle(textDecoration = TextDecoration.Underline)

    @Test
    fun findOccurrences_isCaseInsensitiveAndFindsAll() {
        assertEquals(
            listOf(TextRange(0, 7), TextRange(12, 19)),
            findOccurrences("Магазин и в магазин", "магазин"),
        )
    }

    @Test
    fun findOccurrences_blankQueryFindsNothing() {
        assertEquals(emptyList<TextRange>(), findOccurrences("text", " "))
    }

    @Test
    fun highlight_appliesValidRangesAndSkipsOutOfBounds() {
        val result = highlight("Я иду в магазине", listOf(TextRange(6, 16), TextRange(10, 99)), underline)
        assertEquals("Я иду в магазине", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(6, result.spanStyles.single().start)
        assertEquals(16, result.spanStyles.single().end)
    }
}
