package io.winstonhoyle.practicespeaking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** App theme. Read tokens as `PsTheme.colors`, `PsTheme.spacing`, and so on. */
@Composable
fun PsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPsExtendedColors provides if (darkTheme) PsDarkExtendedColors else PsLightExtendedColors,
        LocalPsShapes provides PsShapes(),
        LocalPsSpacing provides PsSpacing(),
        LocalPsSizes provides PsSizes(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) PsDarkColorScheme else PsLightColorScheme,
            typography = PsTypography,
            shapes = PsMaterialShapes,
            content = content,
        )
    }
}

object PsTheme {
    val colors: ColorScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme
    val extendedColors: PsExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalPsExtendedColors.current
    val typography: Typography
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography
    val shapes: PsShapes
        @Composable @ReadOnlyComposable
        get() = LocalPsShapes.current
    val spacing: PsSpacing
        @Composable @ReadOnlyComposable
        get() = LocalPsSpacing.current
    val sizes: PsSizes
        @Composable @ReadOnlyComposable
        get() = LocalPsSizes.current
}
