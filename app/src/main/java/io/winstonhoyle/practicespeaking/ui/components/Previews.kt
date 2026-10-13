package io.winstonhoyle.practicespeaking.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** Every component is previewed in light, dark, and at 1.5× font scale. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
annotation class PreviewPsThemes

@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    PsTheme {
        Surface {
            Box(Modifier.padding(PsTheme.spacing.lg)) { content() }
        }
    }
}
