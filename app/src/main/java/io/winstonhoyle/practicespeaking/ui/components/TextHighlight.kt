package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString

/** Applies [style] to each range in [ranges], ignoring ranges outside the text. */
fun highlight(text: String, ranges: List<TextRange>, style: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        append(text)
        ranges
            .filter { it.min >= 0 && it.max <= text.length && !it.collapsed }
            .forEach { addStyle(style, it.min, it.max) }
    }

/** Case-insensitive ranges of every occurrence of [query] in [text]. */
fun findOccurrences(text: String, query: String): List<TextRange> {
    if (query.isBlank()) return emptyList()
    val ranges = mutableListOf<TextRange>()
    var start = text.indexOf(query, ignoreCase = true)
    while (start >= 0) {
        ranges += TextRange(start, start + query.length)
        start = text.indexOf(query, start + query.length, ignoreCase = true)
    }
    return ranges
}

/**
 * Where [phrase] appears in [text], ignoring case, punctuation, extra spaces, and stress marks.
 * Speech-to-text and the tutor rarely punctuate a phrase the same way, and only one of them may
 * mark stress. Null when it isn't there.
 */
fun findPhrase(text: String, phrase: String): TextRange? {
    val (haystack, positions) = normalize(text)
    val needle = normalize(phrase).first
    if (needle.isBlank()) return null
    var start = haystack.indexOf(needle)
    while (start >= 0) {
        val end = start + needle.length
        val wholeWords = (start == 0 || haystack[start - 1] == ' ') && (end == haystack.length || haystack[end] == ' ')
        if (wholeWords) return TextRange(positions[start], afterMarks(text, positions[end - 1] + 1))
        start = haystack.indexOf(needle, start + 1)
    }
    return null
}

/** Moves [index] past any combining marks, so a stress mark on the last letter stays inside the range. */
private fun afterMarks(text: String, index: Int): Int {
    var i = index
    while (i < text.length && isMark(text[i])) i++
    return i
}

private fun isMark(c: Char) = Character.getType(c) == Character.NON_SPACING_MARK.toInt()

/** Lowercase letters and digits with single spaces between words, plus each char's index in [text]. Marks are dropped. */
private fun normalize(text: String): Pair<String, List<Int>> {
    val out = StringBuilder()
    val positions = mutableListOf<Int>()
    text.forEachIndexed { i, c ->
        if (isMark(c)) {
            return@forEachIndexed
        } else if (c.isLetterOrDigit()) {
            out.append(c.lowercaseChar())
            positions += i
        } else if (out.isNotEmpty() && out.last() != ' ') {
            out.append(' ')
            positions += i
        }
    }
    if (out.endsWith(' ')) {
        out.setLength(out.length - 1)
        positions.removeAt(positions.lastIndex)
    }
    return out.toString() to positions
}
