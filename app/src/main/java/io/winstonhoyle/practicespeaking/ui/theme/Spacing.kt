package io.winstonhoyle.practicespeaking.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PsSpacing(val xs: Dp = 4.dp, val sm: Dp = 8.dp, val md: Dp = 12.dp, val lg: Dp = 16.dp, val xl: Dp = 24.dp, val xxl: Dp = 32.dp)

/** Fixed sizes components share (touch targets, icons, the mic button). */
@Immutable
data class PsSizes(
    val minTouchTarget: Dp = 48.dp,
    val icon: Dp = 24.dp,
    val iconSmall: Dp = 18.dp,
    val badgeIcon: Dp = 14.dp,
    val flagWidth: Dp = 16.dp,
    val flagCorner: Dp = 2.dp,
    val micButton: Dp = 96.dp,
    val bubbleMaxWidth: Dp = 320.dp,
    val dot: Dp = 8.dp,
    val divider: Dp = 1.dp,
)

val LocalPsSpacing = staticCompositionLocalOf { PsSpacing() }
val LocalPsSizes = staticCompositionLocalOf { PsSizes() }
