package io.winstonhoyle.practicespeaking.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** One past session: badges, title (first message), date, message count. */
@Composable
fun SessionListItem(
    language: TutorLanguage,
    providerLabel: String,
    title: String,
    dateText: String,
    messageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes providerLogo: Int? = null,
) {
    ListRow(onClick, modifier) {
        MetaRow {
            LanguageBadge(language)
            ProviderBadge(providerLabel, logo = providerLogo)
            SecondaryText(dateText)
        }
        Text(title, style = PsTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        SecondaryText(pluralStringResource(R.plurals.message_count, messageCount, messageCount))
    }
}

@PreviewPsThemes
@Composable
private fun SessionListItemPreview() =
    PreviewSurface {
        SessionListItem(
            TutorLanguage.RUSSIAN,
            "Claude",
            "Привет, как дела?",
            "Oct 13, 2026",
            14,
            {},
            providerLogo = R.drawable.ic_logo_claude,
        )
    }
