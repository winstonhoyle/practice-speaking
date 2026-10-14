package io.winstonhoyle.practicespeaking.prompt

/**
 * Russian stress marks: a combining acute accent after the stressed vowel (приве́т). They are
 * shown on screen, but kept out of speech and the search index, which want plain letters.
 */
object Stress {
    const val MARK = '́'

    fun strip(text: String): String = text.replace(MARK.toString(), "")

    fun hasMarks(text: String): Boolean = MARK in text

    /**
     * [stressed] when it is [original] with only stress marks added, else null. Case, punctuation,
     * spacing, and е/ё may differ, since the tutor retypes the sentence; any other letter change
     * means it rewrote the sentence, and the learner's own words are kept.
     */
    fun matchOrNull(original: String, stressed: String): String? {
        val candidate = stressed.trim().trim('"', '«', '»', '“', '”').trim()
        if (!hasMarks(candidate)) return null
        return candidate.takeIf { letters(strip(it)) == letters(original) }
    }

    private fun letters(text: String): String = text.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
}
