package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

// Building blocks shared by the public components. Keep layout decisions here, once.

/** Test tag on a [PsPill]'s leading icon. */
internal const val PILL_ICON_TAG = "pill-icon"

/** Rounded label: language and provider badges. [leading] is an optional flag or logo before the text. */
@Composable
internal fun PsPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(shape = PsTheme.shapes.chip, color = containerColor, contentColor = contentColor, border = border, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PsTheme.spacing.sm, vertical = PsTheme.spacing.xs),
        ) {
            if (leading != null) Box(Modifier.testTag(PILL_ICON_TAG)) { leading() }
            Text(text, style = PsTheme.typography.labelSmall)
        }
    }
}

/** A chat bubble aligned to the speaker's side, with the speaker's colors and tail shape. */
@Composable
internal fun BubbleSurface(isUser: Boolean, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val extended = PsTheme.extendedColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) extended.userBubble else extended.tutorBubble,
            contentColor = if (isUser) extended.onUserBubble else extended.onTutorBubble,
            shape = if (isUser) PsTheme.shapes.bubbleUser else PsTheme.shapes.bubbleTutor,
            modifier = Modifier.widthIn(max = PsTheme.sizes.bubbleMaxWidth),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = PsTheme.spacing.lg, vertical = PsTheme.spacing.md),
                content = content,
            )
        }
    }
}

/** Tinted, rounded card: corrections, the summary tip, error banners. */
@Composable
internal fun PsCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = PsTheme.colors.onSurface,
    contentPadding: PaddingValues = PaddingValues(PsTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = containerColor, contentColor = contentColor, shape = PsTheme.shapes.card, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Full-width tappable list row with a 48dp minimum height: sessions and search hits. */
@Composable
internal fun ListRow(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.xs),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = PsTheme.sizes.minTouchTarget)
                .clickable(onClick = onClick)
                .padding(horizontal = PsTheme.spacing.lg, vertical = PsTheme.spacing.md),
        content = content,
    )
}

/** Small muted text: dates, counts, hints. */
@Composable
internal fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = PsTheme.typography.bodySmall,
        color = PsTheme.colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A horizontal row of badges/metadata with standard spacing. */
@Composable
internal fun MetaRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
        modifier = modifier,
        content = content,
    )
}
