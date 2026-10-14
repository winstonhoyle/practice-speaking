package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * One chat message. [highlights] mark the learner's mistake: underlined (not color alone)
 * in the `mistake` color. [translation], when set, shows under the text in the bubble's own
 * colors, a step quieter. Actions such as replay live outside the bubble, in [ChatMessageItem].
 */
@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    highlights: List<TextRange> = emptyList(),
    translation: String? = null,
) {
    val extended = PsTheme.extendedColors
    val mistakeStyle =
        SpanStyle(
            textDecoration = TextDecoration.Underline,
            background = extended.mistakeContainer,
            color = extended.mistake,
        )
    BubbleSurface(isUser, modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs), modifier = Modifier.weight(1f, fill = false)) {
            Text(text = highlight(text, highlights, mistakeStyle), style = PsTheme.typography.bodyLarge)
            if (translation != null) {
                Text(
                    translation,
                    style = PsTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = LocalContentColor.current.copy(alpha = TRANSLATION_ALPHA),
                )
            }
        }
    }
}

/** The translation reads as secondary inside a bubble of either color. */
private const val TRANSLATION_ALPHA = 0.8f

@PreviewPsThemes
@Composable
private fun MessageBubblePreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
            MessageBubble("Я иду в магазине за хлебом.", isUser = true, highlights = listOf(TextRange(6, 17)))
            MessageBubble(
                "Ты идёшь в магази́н? А что ты хо́чешь купи́ть?",
                isUser = false,
                translation = "Are you going to the shop? What do you want to buy?",
            )
        }
    }
