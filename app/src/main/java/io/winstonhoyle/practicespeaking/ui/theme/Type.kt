package io.winstonhoyle.practicespeaking.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.winstonhoyle.practicespeaking.R

// Nunito is one variable font (Latin Extended + Cyrillic), so all five languages share a family.
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) =
    Font(
        R.font.nunito,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val Nunito =
    FontFamily(
        nunito(FontWeight.Normal),
        nunito(FontWeight.SemiBold),
        nunito(FontWeight.Bold),
    )

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** Body text is at least 16sp; all sizes are sp so they follow the system font scale. */
val PsTypography =
    Typography(
        displaySmall = style(36, 44, FontWeight.Bold),
        headlineMedium = style(28, 36, FontWeight.Bold),
        headlineSmall = style(24, 32, FontWeight.Bold),
        titleLarge = style(22, 28, FontWeight.Bold),
        titleMedium = style(18, 24, FontWeight.SemiBold),
        titleSmall = style(16, 22, FontWeight.SemiBold),
        bodyLarge = style(18, 26),
        bodyMedium = style(16, 24),
        bodySmall = style(14, 20),
        labelLarge = style(16, 20, FontWeight.SemiBold),
        labelMedium = style(14, 18, FontWeight.SemiBold),
        labelSmall = style(12, 16, FontWeight.Bold),
    )
