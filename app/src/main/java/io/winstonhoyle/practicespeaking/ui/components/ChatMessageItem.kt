package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * One message in a transcript. On a learner message, [correction] (stored as
 * `wrong → right | explanation`) underlines the mistake in the bubble and adds a card below it.
 * If speech-to-text changed the phrase so it can't be found, only the card is shown.
 */
@Composable
fun ChatMessageItem(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    correction: String? = null,
    onReplay: (() -> Unit)? = null,
) {
    val parsed = remember(correction) { correction?.let(TutorResponseParser::parseCorrection) }
    val highlights = remember(text, parsed) { listOfNotNull(parsed?.let { findPhrase(text, it.wrong) }) }
    Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs), modifier = modifier.fillMaxWidth()) {
        MessageBubble(text = keepTogether(text, highlights), isUser = isUser, highlights = highlights, onReplay = onReplay)
        if (isUser && correction != null) {
            CorrectionCard(
                wrong = parsed?.wrong ?: correction,
                right = parsed?.right,
                explanation = parsed?.explanation,
                modifier = Modifier.padding(start = PsTheme.spacing.xxl),
            )
        }
    }
}

/** Non-breaking spaces inside each highlighted phrase, so a mistake never splits across two lines. */
private fun keepTogether(text: String, ranges: List<TextRange>): String {
    if (ranges.isEmpty()) return text
    val chars = text.toCharArray()
    for (range in ranges) {
        for (i in range.min until range.max) if (chars[i] == ' ') chars[i] = '\u00A0'
    }
    return String(chars)
}

@PreviewPsThemes
@Composable
private fun ChatMessageItemPreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md)) {
            ChatMessageItem(
                "Я иду в магазине.",
                isUser = true,
                correction = "в магазине → в магазин | Motion toward a place takes the accusative.",
            )
            ChatMessageItem("Ты идёшь в магазин? Что хочешь купить?", isUser = false, onReplay = {})
        }
    }
