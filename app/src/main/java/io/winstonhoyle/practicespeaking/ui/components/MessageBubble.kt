package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * One chat message. [highlights] mark the learner's mistake: underlined (not color alone)
 * in the `mistake` color. [onReplay] shows a replay-audio button when set.
 */
@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    highlights: List<TextRange> = emptyList(),
    onReplay: (() -> Unit)? = null,
) {
    val extended = PsTheme.extendedColors
    val mistakeStyle =
        SpanStyle(
            textDecoration = TextDecoration.Underline,
            background = extended.mistakeContainer,
            color = extended.mistake,
        )
    BubbleSurface(isUser, modifier) {
        Text(
            text = highlight(text, highlights, mistakeStyle),
            style = PsTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onReplay != null) {
            IconButton(onClick = onReplay) {
                Icon(PsIcons.Replay, contentDescription = stringResource(R.string.cd_replay))
            }
        }
    }
}

@PreviewPsThemes
@Composable
private fun MessageBubblePreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
            MessageBubble("Я иду в магазине за хлебом.", isUser = true, highlights = listOf(TextRange(6, 17)))
            MessageBubble("Ты идёшь в магазин? А что ты хочешь купить?", isUser = false, onReplay = {})
        }
    }
