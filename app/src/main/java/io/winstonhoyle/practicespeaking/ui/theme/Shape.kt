package io.winstonhoyle.practicespeaking.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val PsMaterialShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
    )

/** Bubbles are 20dp with a tight "tail" corner on the speaker's side; cards 16dp; chips round. */
@Immutable
data class PsShapes(
    val bubbleUser: Shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
    val bubbleTutor: Shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val chip: Shape = CircleShape,
)

val LocalPsShapes = staticCompositionLocalOf { PsShapes() }
