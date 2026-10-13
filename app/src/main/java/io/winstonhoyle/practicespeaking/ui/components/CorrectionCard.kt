package io.winstonhoyle.practicespeaking.ui.components

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
 */
@Composable
fun CorrectionCard(wrong: String, right: String?, explanation: String?, modifier: Modifier = Modifier) {
    val extended = PsTheme.extendedColors
    val label = stringResource(R.string.cd_correction)
    PsCard(
        containerColor = extended.mistakeContainer,
        modifier =
            modifier.semantics {
                contentDescription =
                    listOfNotNull(label + ":", wrong, right?.let { "→ $it." }, explanation).joinToString(" ")
            },
    ) {
        if (right == null) {
            Text(wrong, style = PsTheme.typography.bodyLarge)
        } else {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = extended.mistake, textDecoration = TextDecoration.LineThrough)) {
                        append(wrong)
                    }
                    append("  →  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(right) }
                },
                style = PsTheme.typography.bodyLarge,
            )
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
        CorrectionCard("в магазине", "в магазин", "Motion toward a place takes the accusative.")
    }
