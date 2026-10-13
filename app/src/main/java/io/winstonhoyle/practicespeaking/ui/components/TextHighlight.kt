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
 * Where [phrase] appears in [text], ignoring case, punctuation, and extra spaces. Speech-to-text
 * and the tutor rarely punctuate a phrase the same way. Null when it isn't there.
 */
fun findPhrase(text: String, phrase: String): TextRange? {
    val (haystack, positions) = normalize(text)
    val needle = normalize(phrase).first
    if (needle.isBlank()) return null
    var start = haystack.indexOf(needle)
    while (start >= 0) {
        val end = start + needle.length
        val wholeWords = (start == 0 || haystack[start - 1] == ' ') && (end == haystack.length || haystack[end] == ' ')
        if (wholeWords) return TextRange(positions[start], positions[end - 1] + 1)
        start = haystack.indexOf(needle, start + 1)
    }
    return null
}

/** Lowercase letters and digits with single spaces between words, plus each char's index in [text]. */
private fun normalize(text: String): Pair<String, List<Int>> {
    val out = StringBuilder()
    val positions = mutableListOf<Int>()
    text.forEachIndexed { i, c ->
        if (c.isLetterOrDigit()) {
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
