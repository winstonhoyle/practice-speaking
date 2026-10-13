package io.winstonhoyle.practicespeaking.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** The tutor's built-in commands. The trigger words sent to the model live in the prompt layer. */
enum class TutorCommand { Slower, Harder, Translate, EndSession }

/** Slower, Harder, Translate, and End session as a centered 2×2 grid of chips. */
@Composable
fun CommandChipRow(onCommand: (TutorCommand) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    // A centered 2×2 grid of equal-width chips: nothing is cut off or left alone on a line, at any font size.
    Column(
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs),
        modifier = modifier.widthIn(max = PsTheme.sizes.bubbleMaxWidth),
    ) {
        TutorCommand.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
                pair.forEach { command ->
                    AssistChip(
                        onClick = { onCommand(command) },
                        enabled = enabled,
                        shape = PsTheme.shapes.chip,
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                stringResource(command.label),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                }
            }
        }
    }
}

@get:StringRes
private val TutorCommand.label: Int
    get() =
        when (this) {
            TutorCommand.Slower -> R.string.command_slower
            TutorCommand.Harder -> R.string.command_harder
            TutorCommand.Translate -> R.string.command_translate
            TutorCommand.EndSession -> R.string.command_end_session
        }

@PreviewPsThemes
@Composable
private fun CommandChipRowPreview() = PreviewSurface { CommandChipRow(onCommand = {}) }
