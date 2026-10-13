package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.winstonhoyle.practicespeaking.R

/** The app's icon set: Material SVGs bundled as vector drawables in res/drawable (no icon library). */
object PsIcons {
    val Back: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_back)
    val Replay: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_volume_up)
    val Show: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_visibility)
    val Hide: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_visibility_off)
    val Search: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_search)
    val Clear: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_clear)
    val Error: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_error_outline)
    val Conversation: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_forum)
    val Mic: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mic)
    val MicOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mic_off)
    val Thinking: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_more_horiz)
    val Speaking: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_graphic_eq)
    val DarkMode: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_dark_mode)
    val LightMode: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_light_mode)
    val Settings: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_settings)
    val History: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_history)
    val Send: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_send)
    val Delete: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_delete)
    val Success: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_check_circle)
    val Failure: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_cancel)
    val Info: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_info)
}
