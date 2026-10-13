package io.winstonhoyle.practicespeaking.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** Each language is pinned to one country's variant (pt-BR, es-MX, …), so it gets that country's flag. */
@get:DrawableRes
val TutorLanguage.flag: Int
    get() =
        when (this) {
            TutorLanguage.RUSSIAN -> R.drawable.ic_flag_ru
            TutorLanguage.FRENCH -> R.drawable.ic_flag_fr
            TutorLanguage.PORTUGUESE -> R.drawable.ic_flag_br
            TutorLanguage.GERMAN -> R.drawable.ic_flag_de
            TutorLanguage.SPANISH -> R.drawable.ic_flag_mx
        }

/** A 4:3 flag with slightly rounded corners and a hairline edge, so white stripes stay visible on white. */
@Composable
fun FlagImage(language: TutorLanguage, modifier: Modifier = Modifier, width: Dp = PsTheme.sizes.flagWidth) {
    val shape = RoundedCornerShape(PsTheme.sizes.flagCorner)
    Image(
        imageVector = ImageVector.vectorResource(language.flag),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier =
            modifier
                .size(width, width * FLAG_ASPECT)
                .clip(shape)
                .border(PsTheme.sizes.divider, PsTheme.colors.outlineVariant, shape),
    )
}

/** A provider's mark, tinted with the surrounding content color. Decorative: the name is always shown next to it. */
@Composable
fun ProviderLogo(
    @DrawableRes logo: Int,
    modifier: Modifier = Modifier,
    size: Dp = PsTheme.sizes.badgeIcon,
) {
    Icon(ImageVector.vectorResource(logo), contentDescription = null, modifier = modifier.size(size))
}

/** Flag and short language code, outlined in the language's accent color (accents are for badges only). */
@Composable
fun LanguageBadge(language: TutorLanguage, modifier: Modifier = Modifier) {
    val accent = PsTheme.extendedColors.accent(language)
    PsPill(
        text = language.code,
        containerColor = PsTheme.colors.surface,
        contentColor = accent,
        border = BorderStroke(PsTheme.sizes.divider, accent),
        leading = { FlagImage(language) },
        modifier = modifier.semantics { contentDescription = language.displayName },
    )
}

/**
 * Which AI provider a session used. Takes a plain label and an optional [logo] drawable, so the
 * UI stays vendor-agnostic; a provider without a logo shows the text alone.
 */
@Composable
fun ProviderBadge(
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes logo: Int? = null,
) {
    PsPill(
        text = label,
        containerColor = PsTheme.colors.secondaryContainer,
        contentColor = PsTheme.colors.onSecondaryContainer,
        leading = logo?.let { { ProviderLogo(it) } },
        modifier = modifier,
    )
}

/** The badge for a [Provider]: its short name and logo. */
@Composable
fun ProviderBadge(provider: Provider, modifier: Modifier = Modifier) = ProviderBadge(provider.shortName, modifier, provider.logo)

/** flag-icons draws every flag at 4:3. */
private const val FLAG_ASPECT = 3f / 4f

@PreviewPsThemes
@Composable
private fun BadgesPreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
            ) {
                TutorLanguage.entries.forEach { LanguageBadge(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
                Provider.entries.forEach { ProviderBadge(it) }
                ProviderBadge("Other")
            }
        }
    }
