package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.prompt.TutorResponseParser
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * One message in a transcript. On a learner message, [correction] (stored as
 * `wrong → right | explanation`) underlines the mistake in the bubble and adds a card below it.
 * If speech-to-text changed the phrase so it can't be found, only the card is shown.
 *
 * Under the bubble, [onHear] reads the message aloud (on the learner's own message too, to
 * compare pronunciation) and [onTranslate] shows or hides [translation]. [onEdit], on a learner
 * message, goes back to it to change it. The correction card's button reads the correct form
 * through [onHearPhrase].
 */
@Composable
fun ChatMessageItem(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    correction: String? = null,
    translation: String? = null,
    translating: Boolean = false,
    onHear: (() -> Unit)? = null,
    onTranslate: (() -> Unit)? = null,
    onHearPhrase: ((String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val parsed = remember(correction) { correction?.let(TutorResponseParser::parseCorrection) }
    val highlights = remember(text, parsed) { listOfNotNull(parsed?.let { findPhrase(text, it.wrong) }) }
    Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs), modifier = modifier.fillMaxWidth()) {
        MessageBubble(
            text = keepTogether(text, highlights),
            isUser = isUser,
            highlights = highlights,
            translation = if (translating) stringResource(R.string.translating) else translation,
        )
        if (onHear != null || onTranslate != null || onEdit != null) {
            MessageActions(isUser, translationShown = translation != null, translating, onHear, onTranslate, onEdit)
        }
        if (isUser && correction != null) {
            CorrectionCard(
                wrong = parsed?.wrong ?: correction,
                right = parsed?.right,
                explanation = parsed?.explanation,
                modifier = Modifier.padding(start = PsTheme.spacing.xxl),
                onHearRight = parsed?.right?.let { right -> onHearPhrase?.let { { it(right) } } },
            )
        }
    }
}

/** Hear it, translate, and edit, on the same side as the bubble they belong to. */
@Composable
private fun MessageActions(
    isUser: Boolean,
    translationShown: Boolean,
    translating: Boolean,
    onHear: (() -> Unit)?,
    onTranslate: (() -> Unit)?,
    onEdit: (() -> Unit)?,
) {
    Row(horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        // No gap between the buttons: their touch targets already space the small icons apart.
        Row {
            if (onHear != null) PsIconAction(PsIcons.Replay, stringResource(R.string.cd_hear_it), onHear, subtle = true)
            if (onTranslate != null) {
                val label = if (translationShown) R.string.cd_hide_translation else R.string.cd_translate
                PsIconAction(PsIcons.Translate, stringResource(label), onTranslate, enabled = !translating, subtle = true)
            }
            if (onEdit != null) PsIconAction(PsIcons.Edit, stringResource(R.string.cd_edit_message), onEdit, subtle = true)
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
                "Я иду́ в магази́не.",
                isUser = true,
                correction = "в магазине → в магази́н | Motion toward a place takes the accusative.",
                onHear = {},
                onTranslate = {},
                onHearPhrase = {},
                onEdit = {},
            )
            ChatMessageItem(
                "Ты идёшь в магази́н? Что хо́чешь купи́ть?",
                isUser = false,
                translation = "Are you going to the shop? What do you want to buy?",
                onHear = {},
                onTranslate = {},
            )
        }
    }
