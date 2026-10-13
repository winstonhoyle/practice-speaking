package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** End-of-session summary in a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySheet(mistakes: String?, newWords: String?, tip: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        SummaryContent(mistakes, newWords, tip, Modifier.padding(bottom = PsTheme.spacing.xxl))
    }
}

/** The sheet's body; also used inline as the summary card in session detail. */
@Composable
fun SummaryContent(
    mistakes: String?,
    newWords: String?,
    tip: String?,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = PsTheme.spacing.xl,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg),
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
    ) {
        Text(stringResource(R.string.summary_title), style = PsTheme.typography.titleLarge)
        SummarySection(stringResource(R.string.summary_mistakes), mistakes, PsTheme.extendedColors.mistake)
        SummarySection(stringResource(R.string.summary_new_words), newWords, PsTheme.colors.primary)
        if (!tip.isNullOrBlank()) {
            PsCard(containerColor = PsTheme.extendedColors.successContainer) {
                SummarySection(stringResource(R.string.summary_tip), tip, PsTheme.extendedColors.success)
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, body: String?, titleColor: Color, modifier: Modifier = Modifier) {
    if (body.isNullOrBlank()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs)) {
        Text(title, style = PsTheme.typography.titleSmall, color = titleColor)
        Text(body, style = PsTheme.typography.bodyMedium)
    }
}

@PreviewPsThemes
@Composable
private fun SummaryContentPreview() =
    PreviewSurface {
        SummaryContent(
            mistakes = "в магазине → в магазин; я хотел пойти → я хотел сходить",
            newWords = "хлеб — bread, сходить — to go (and come back)",
            tip = "Practise the accusative after в and на with verbs of motion.",
        )
    }

/** The summary as a card inside a transcript (session detail). */
@Composable
fun SummaryCard(mistakes: String?, newWords: String?, tip: String?, modifier: Modifier = Modifier) {
    PsCard(
        containerColor = PsTheme.colors.surfaceContainer,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = PsTheme.spacing.lg),
    ) {
        // Same inset as the chat bubbles above it, so the left edges line up.
        SummaryContent(mistakes, newWords, tip, horizontalPadding = PsTheme.spacing.lg)
    }
}
