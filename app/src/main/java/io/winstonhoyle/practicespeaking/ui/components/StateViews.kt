package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** An icon, a message, and an optional action, centered in the space it's given. */
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    // Centered both ways in whatever space the caller gives it.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(PsTheme.spacing.xl),
    ) {
        Icon(icon, contentDescription = null, tint = PsTheme.colors.primary, modifier = Modifier.size(PsTheme.sizes.icon * 2))
        Text(
            message,
            style = PsTheme.typography.bodyLarge,
            color = PsTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Whether a [PsBanner] reports a failure or gives a hint. */
enum class BannerTone { Error, Info }

/** Inline notice with an optional action. [BannerTone.Error] for failures, [BannerTone.Info] for hints. */
@Composable
fun PsBanner(
    message: String,
    tone: BannerTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val container = if (tone == BannerTone.Error) PsTheme.colors.errorContainer else PsTheme.colors.secondaryContainer
    val content = if (tone == BannerTone.Error) PsTheme.colors.onErrorContainer else PsTheme.colors.onSecondaryContainer
    PsCard(
        containerColor = container,
        contentColor = content,
        contentPadding =
            PaddingValues(
                start = PsTheme.spacing.lg,
                end = PsTheme.spacing.sm,
                top = PsTheme.spacing.sm,
                bottom = PsTheme.spacing.sm,
            ),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.md),
        ) {
            Icon(if (tone == BannerTone.Error) PsIcons.Error else PsIcons.Info, contentDescription = null)
            Text(message, style = PsTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = content) }
            }
        }
    }
}

/** Inline error with an optional action (retry, open Settings, end session). */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
    PsBanner(message, BannerTone.Error, modifier, actionLabel, onAction)

/** Centered spinner for screens that are still loading. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(PsTheme.spacing.xl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@PreviewPsThemes
@Composable
private fun StateViewsPreview() =
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg)) {
            EmptyState(PsIcons.Conversation, "No conversations yet. Tap the mic to start one.", actionLabel = "Start", onAction = {})
            ErrorBanner("Check your API key", actionLabel = "Open Settings", onAction = {})
            PsBanner("Long session. End it and start fresh?", BannerTone.Info, actionLabel = "End", onAction = {})
            LoadingState()
        }
    }
