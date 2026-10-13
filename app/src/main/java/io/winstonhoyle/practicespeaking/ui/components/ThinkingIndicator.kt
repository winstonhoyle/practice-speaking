package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** Three pulsing dots inside a tutor bubble. Static when animations are off. */
@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "thinking")
    val description = stringResource(R.string.cd_thinking)
    BubbleSurface(isUser = false, modifier = modifier.semantics { contentDescription = description }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs),
            modifier = Modifier.padding(vertical = PsTheme.spacing.xs),
        ) {
            repeat(DOT_COUNT) { index ->
                val alpha =
                    if (reduceMotion) {
                        STILL_ALPHA
                    } else {
                        transition
                            .animateFloat(
                                initialValue = MIN_ALPHA,
                                targetValue = 1f,
                                animationSpec =
                                    infiniteRepeatable(
                                        tween(BLINK_MILLIS),
                                        RepeatMode.Reverse,
                                        StartOffset(index * STAGGER_MILLIS),
                                    ),
                                label = "dot$index",
                            ).value
                    }
                Box(
                    Modifier
                        .size(PsTheme.sizes.dot)
                        .alpha(alpha)
                        .background(PsTheme.extendedColors.onTutorBubble, CircleShape),
                )
            }
        }
    }
}

@PreviewPsThemes
@Composable
private fun ThinkingIndicatorPreview() = PreviewSurface { ThinkingIndicator() }

private const val DOT_COUNT = 3
private const val STILL_ALPHA = 0.6f
private const val MIN_ALPHA = 0.25f
private const val BLINK_MILLIS = 500
private const val STAGGER_MILLIS = 160
