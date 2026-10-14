package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * ~~wrong~~ → **right**, then the short explanation. Strikethrough + arrow, not color alone.
 * With no [right] (a correction the parser couldn't split), [wrong] is shown as plain text.
 * [onHearRight], when set, adds a button that reads the correct form aloud.
 */
@Composable
fun CorrectionCard(wrong: String, right: String?, explanation: String?, modifier: Modifier = Modifier, onHearRight: (() -> Unit)? = null) {
    val extended = PsTheme.extendedColors
    val label = stringResource(R.string.cd_correction)
    // The hear button's 48dp touch target already adds space around the first line, so the card trims its own padding there.
    val padding =
        if (onHearRight != null && right != null) {
            PaddingValues(start = PsTheme.spacing.md, end = PsTheme.spacing.xs, top = PsTheme.spacing.xs, bottom = PsTheme.spacing.md)
        } else {
            PaddingValues(PsTheme.spacing.md)
        }
    PsCard(
        containerColor = extended.mistakeContainer,
        contentPadding = padding,
        modifier =
            modifier.semantics {
                contentDescription =
                    listOfNotNull(label + ":", wrong, right?.let { "→ $it." }, explanation).joinToString(" ")
            },
    ) {
        if (right == null) {
            Text(wrong, style = PsTheme.typography.bodyLarge)
        } else {
            MetaRow {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = extended.mistake, textDecoration = TextDecoration.LineThrough)) {
                            append(wrong)
                        }
                        append("  →  ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(right) }
                    },
                    style = PsTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (onHearRight != null) {
                    PsIconAction(PsIcons.Replay, stringResource(R.string.cd_hear_correction), onHearRight, subtle = true)
                }
            }
        }
        if (!explanation.isNullOrBlank()) {
            Text(
                explanation,
                style = PsTheme.typography.bodyMedium,
                color = PsTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = PsTheme.spacing.xs),
            )
        }
    }
}

@PreviewPsThemes
@Composable
private fun CorrectionCardPreview() =
    PreviewSurface {
        CorrectionCard("в магазине", "в магази́н", "Motion toward a place takes the accusative.", onHearRight = {})
    }
