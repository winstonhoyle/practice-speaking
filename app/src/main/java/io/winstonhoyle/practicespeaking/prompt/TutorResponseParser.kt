package io.winstonhoyle.practicespeaking.prompt

/** A learner mistake from a `CORRECTION:` line: `wrong → right | explanation`. */
data class Correction(val wrong: String, val right: String, val explanation: String?) {
    /** The stored form, always with a plain arrow and pipe. */
    fun format(): String = "$wrong → $right" + (explanation?.let { " | $it" } ?: "")

    /** How a correction reads in one line of UI text, such as a search result. */
    fun display(): String = "$wrong → $right" + (explanation?.let { " ($it)" } ?: "")
}

data class Summary(val mistakes: String?, val newWords: String?, val tip: String?)

/**
 * One parsed tutor reply. [spokenText] is what the bubble shows; [speechText] is the same
 * text without markdown or emoji, for TTS. [rawCorrection] is the correction line as sent when
 * it couldn't be split into parts. [stressed] is the learner's last message with stress marks, from
 * the `STRESSED:` line (Russian only); check it with [Stress.matchOrNull] before showing it.
 */
data class ParsedReply(
    val spokenText: String,
    val speechText: String,
    val correction: Correction?,
    val rawCorrection: String?,
    val summary: Summary?,
    val stressed: String? = null,
)

/**
 * Splits a tutor reply into the spoken text, an optional correction, and an optional summary.
 * Models drift from the exact format, so it accepts `->`, extra spaces, and stray `**`.
 * Malformed blocks fall back to plain text; it never throws.
 */
object TutorResponseParser {
    private val correctionLine = Regex("""^\W*CORRECTION\W*:\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val stressedLine = Regex("""^\W*STRESSED\W*:\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val summaryHeader = Regex("""^\W*SUMMARY\W*$""", RegexOption.IGNORE_CASE)
    private val summaryField = Regex("""^\W*(Mistakes|New words|Tip)\W*:\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val arrow = Regex("""\s*(→|->|=>)\s*""")

    fun parse(reply: String): ParsedReply {
        val lines = reply.trim().lines()
        val summaryStart = lines.indexOfFirst { summaryHeader.matches(it.trim()) }
        val body = if (summaryStart >= 0) lines.subList(0, summaryStart) else lines
        val summary = if (summaryStart >= 0) parseSummary(lines.drop(summaryStart + 1)) else null

        val correctionIndex = body.indexOfLast { correctionLine.matches(it.trim()) }
        val rawCorrection =
            if (correctionIndex >= 0) {
                correctionLine
                    .find(body[correctionIndex].trim())!!
                    .groupValues[1]
                    .let(::cleanMarkdown)
                    .trim()
                    .ifBlank { null }
            } else {
                null
            }
        val stressedIndex = body.indexOfLast { stressedLine.matches(it.trim()) }
        val stressed =
            if (stressedIndex >= 0) {
                stressedLine
                    .find(body[stressedIndex].trim())!!
                    .groupValues[1]
                    .let(::cleanMarkdown)
                    .trim()
                    .ifBlank { null }
            } else {
                null
            }
        val spokenLines = body.filterIndexed { i, _ -> i != correctionIndex && i != stressedIndex }
        val spoken = cleanMarkdown(spokenLines.joinToString("\n")).trim()

        return ParsedReply(
            spokenText = spoken,
            speechText = forSpeech(spoken),
            correction = rawCorrection?.let(::parseCorrection),
            rawCorrection = rawCorrection,
            summary = summary,
            stressed = stressed,
        )
    }

    /** Parses a stored or raw `wrong → right | explanation` string. Null when there is no arrow. */
    fun parseCorrection(raw: String): Correction? {
        val (pair, explanation) = raw.split("|", limit = 2).let { it[0] to it.getOrNull(1)?.trim()?.ifBlank { null } }
        val parts = pair.split(arrow, limit = 2)
        if (parts.size < 2) return null
        val wrong = parts[0].trim().trim('"', '«', '»', '“', '”')
        val right = parts[1].trim().trim('"', '«', '»', '“', '”')
        if (wrong.isEmpty() || right.isEmpty()) return null
        return Correction(wrong, right, explanation)
    }

    private fun parseSummary(lines: List<String>): Summary? {
        val fields = mutableMapOf<String, String>()
        var current: String? = null
        lines.map { cleanMarkdown(it).trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val match = summaryField.find(line)
            if (match != null) {
                current = match.groupValues[1].lowercase()
                fields[current!!] = match.groupValues[2].trim()
            } else if (current != null) {
                // A field written as a list continues on the next lines.
                val item = line.removePrefix("-").removePrefix("•").trim()
                fields[current!!] = listOf(fields[current!!], item).filter { !it.isNullOrBlank() }.joinToString("; ")
            }
        }
        if (fields.isEmpty()) return null
        return Summary(
            mistakes = fields["mistakes"]?.ifBlank { null },
            newWords = fields["new words"]?.ifBlank { null },
            tip = fields["tip"]?.ifBlank { null },
        )
    }

    private fun cleanMarkdown(text: String): String = text.replace(Regex("""\*\*|__|`"""), "")

    /** Removes markdown, emoji, and stress marks so TTS reads plain words. */
    fun forSpeech(text: String): String =
        Stress
            .strip(text)
            .let(::cleanMarkdown)
            .replace(Regex("""(?m)^\s*([#>*-]|\d+\.)\s+"""), "")
            .replace(Regex("""[*_~]"""), "")
            .replace(Regex("""[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{FE0F}\x{200D}]"""), "")
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
}
