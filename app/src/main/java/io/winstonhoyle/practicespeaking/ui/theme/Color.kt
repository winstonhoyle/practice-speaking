package io.winstonhoyle.practicespeaking.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage

// Brand: deep teal. Fixed scheme (no dynamic color) so screenshots match on every device.
private val Teal10 = Color(0xFF00201C)
private val Teal20 = Color(0xFF003731)
private val Teal30 = Color(0xFF005048)
private val Teal40 = Color(0xFF006B60)
private val Teal80 = Color(0xFF5EDBC8)
private val Teal90 = Color(0xFF7DF8E4)
private val Teal95 = Color(0xFFC9FFF3)

private val Slate10 = Color(0xFF151D1B)
private val Slate20 = Color(0xFF2A3230)
private val Slate30 = Color(0xFF3F4946)
private val Slate50 = Color(0xFF6F7976)
private val Slate60 = Color(0xFF899390)
private val Slate80 = Color(0xFFBEC9C5)
private val Slate90 = Color(0xFFDAE5E1)
private val Slate95 = Color(0xFFE9F0ED)
private val Slate98 = Color(0xFFF5FBF8)
private val Night = Color(0xFF0F1513)
private val NightHigh = Color(0xFF1B2220)

private val Indigo30 = Color(0xFF2E3A73)
private val Indigo40 = Color(0xFF46528C)
private val Indigo80 = Color(0xFFBAC3FF)
private val Indigo90 = Color(0xFFDEE0FF)

val PsLightColorScheme =
    lightColorScheme(
        primary = Teal40,
        onPrimary = Color.White,
        primaryContainer = Teal90,
        onPrimaryContainer = Teal10,
        secondary = Indigo40,
        onSecondary = Color.White,
        secondaryContainer = Indigo90,
        onSecondaryContainer = Indigo30,
        background = Slate98,
        onBackground = Slate10,
        surface = Slate98,
        onSurface = Slate10,
        surfaceVariant = Slate90,
        onSurfaceVariant = Slate30,
        surfaceContainer = Slate95,
        surfaceContainerHigh = Slate90,
        outline = Slate50,
        outlineVariant = Slate80,
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

val PsDarkColorScheme =
    darkColorScheme(
        primary = Teal80,
        onPrimary = Teal20,
        primaryContainer = Teal30,
        onPrimaryContainer = Teal95,
        secondary = Indigo80,
        onSecondary = Indigo30,
        secondaryContainer = Indigo30,
        onSecondaryContainer = Indigo90,
        background = Night,
        onBackground = Slate90,
        surface = Night,
        onSurface = Slate90,
        surfaceVariant = Slate30,
        onSurfaceVariant = Slate80,
        surfaceContainer = NightHigh,
        surfaceContainerHigh = Slate20,
        outline = Slate60,
        outlineVariant = Slate30,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

/**
 * Semantic colors Material doesn't have. Language accents are for badges only.
 * Mistake (amber) and success (teal-green) stay apart for color-blind users; see
 * "Color and access" in docs/DESIGN.md before changing them.
 */
@Immutable
data class PsExtendedColors(
    val userBubble: Color,
    val onUserBubble: Color,
    val tutorBubble: Color,
    val onTutorBubble: Color,
    val mistake: Color,
    val mistakeContainer: Color,
    val success: Color,
    val successContainer: Color,
    val languageAccents: Map<TutorLanguage, Color>,
) {
    fun accent(language: TutorLanguage): Color = languageAccents.getValue(language)
}

val PsLightExtendedColors =
    PsExtendedColors(
        userBubble = Teal40,
        onUserBubble = Color.White,
        tutorBubble = Color(0xFFE6EBF5),
        onTutorBubble = Slate10,
        mistake = Color(0xFFA65200),
        mistakeContainer = Color(0xFFFFF2CC),
        success = Color(0xFF0E6650),
        successContainer = Color(0xFFC2F4F4),
        languageAccents =
            mapOf(
                TutorLanguage.RUSSIAN to Color(0xFFB0283B),
                TutorLanguage.FRENCH to Color(0xFF2B55B8),
                TutorLanguage.PORTUGUESE to Color(0xFF1C7A43),
                TutorLanguage.GERMAN to Color(0xFF8A5A00),
                TutorLanguage.SPANISH to Color(0xFFB4421A),
            ),
    )

val PsDarkExtendedColors =
    PsExtendedColors(
        userBubble = Teal30,
        onUserBubble = Teal95,
        tutorBubble = Color(0xFF212645),
        onTutorBubble = Slate90,
        mistake = Color(0xFFFF8833),
        mistakeContainer = Color(0xFF4E3A1D),
        success = Color(0xFF7FE0B8),
        successContainer = Color(0xFF12211A),
        languageAccents =
            mapOf(
                TutorLanguage.RUSSIAN to Color(0xFFFFB2B8),
                TutorLanguage.FRENCH to Color(0xFFB2C5FF),
                TutorLanguage.PORTUGUESE to Color(0xFF8DD9A8),
                TutorLanguage.GERMAN to Color(0xFFF2C173),
                TutorLanguage.SPANISH to Color(0xFFFFB597),
            ),
    )

val LocalPsExtendedColors = staticCompositionLocalOf { PsLightExtendedColors }
