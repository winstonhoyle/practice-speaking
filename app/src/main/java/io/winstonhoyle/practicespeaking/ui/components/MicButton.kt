package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** What the [MicButton] shows: ready, listening, waiting for the tutor, speaking, or unavailable. */
enum class MicState { Idle, Listening, Thinking, Speaking, Disabled }

/** Large hold-to-talk button. Press starts listening, release stops. Pulses while listening. */
@Composable
fun MicButton(state: MicState, onPressStart: () -> Unit, onPressEnd: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val currentStart by rememberUpdatedState(onPressStart)
    val currentEnd by rememberUpdatedState(onPressEnd)
    val enabled = state == MicState.Idle || state == MicState.Listening || state == MicState.Speaking
    val label =
        stringResource(
            when (state) {
                MicState.Idle -> R.string.mic_idle
                MicState.Listening -> R.string.mic_listening
                MicState.Thinking -> R.string.mic_thinking
                MicState.Speaking -> R.string.mic_speaking
                MicState.Disabled -> R.string.mic_disabled
            },
        )
    val pulse =
        if (state == MicState.Listening && !rememberReduceMotion()) {
            val transition = rememberInfiniteTransition(label = "mic")
            transition
                .animateFloat(
                    initialValue = 1f,
                    targetValue = PULSE_SCALE,
                    animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
                    label = "micPulse",
                ).value
        } else {
            1f
        }
    val colors = PsTheme.colors
    val (container, content) =
        when (state) {
            MicState.Listening -> colors.primary to colors.onPrimary
            MicState.Disabled -> colors.surfaceVariant to colors.onSurfaceVariant
            else -> colors.primaryContainer to colors.onPrimaryContainer
        }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(PsTheme.sizes.micButton)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(container)
                    .semantics {
                        role = Role.Button
                        contentDescription = label
                    }.pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentStart()
                            try {
                                // Only lifting the finger ends the hold; drifting past the pulsing edge must not.
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                            } finally {
                                currentEnd()
                            }
                        }
                    },
        ) {
            Icon(
                imageVector =
                    when (state) {
                        MicState.Disabled -> PsIcons.MicOff
                        MicState.Thinking -> PsIcons.Thinking
                        MicState.Speaking -> PsIcons.Speaking
                        else -> PsIcons.Mic
                    },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(PsTheme.sizes.icon * 2),
            )
        }
        Text(label, style = PsTheme.typography.labelLarge, color = PsTheme.colors.onSurfaceVariant)
    }
}

@PreviewPsThemes
@Composable
private fun MicButtonPreview() =
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg)) {
            MicButton(MicState.Idle, {}, {})
            MicButton(MicState.Listening, {}, {})
            MicButton(MicState.Disabled, {}, {})
        }
    }

private const val PULSE_SCALE = 1.08f
private const val PULSE_MILLIS = 600
