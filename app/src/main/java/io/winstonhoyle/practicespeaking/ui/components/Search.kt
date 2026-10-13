package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** The History search box, with a clear button once there's text. Debouncing is the caller's job. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(PsIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(PsIcons.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                }
            }
        },
        singleLine = true,
        shape = PsTheme.shapes.chip,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A search hit: session context plus a snippet with every match of [query] in bold, primary color. */
@Composable
fun SearchResultItem(
    snippet: String,
    query: String,
    language: TutorLanguage,
    sessionTitle: String,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val matchStyle = SpanStyle(fontWeight = FontWeight.Bold, color = PsTheme.colors.primary)
    ListRow(onClick, modifier) {
        Text(
            highlight(snippet, findOccurrences(snippet, query), matchStyle),
            style = PsTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MetaRow {
            LanguageBadge(language)
            SecondaryText("$sessionTitle · $dateText")
        }
    }
}

@PreviewPsThemes
@Composable
private fun SearchPreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
            SearchField("магазин", {})
            SearchResultItem("Я иду в магазине за хлебом.", "магазин", TutorLanguage.RUSSIAN, "Привет, как дела?", "Oct 13", {})
        }
    }
