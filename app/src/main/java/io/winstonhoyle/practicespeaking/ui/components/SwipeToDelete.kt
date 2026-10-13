package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * Swipe a row left to ask for deletion. The row springs back and [onDelete] runs, so the caller
 * can confirm before deleting anything. The row sits on an opaque surface, so the delete
 * background only shows while swiping.
 */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val currentOnDelete by rememberUpdatedState(onDelete)
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            currentOnDelete()
            state.reset()
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
        modifier = modifier,
    ) {
        Surface(color = PsTheme.colors.surface) { content() }
    }
}

@Composable
private fun DeleteBackground(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier.fillMaxSize().background(PsTheme.colors.errorContainer).padding(horizontal = PsTheme.spacing.xl),
    ) {
        Icon(PsIcons.Delete, contentDescription = stringResource(R.string.action_delete), tint = PsTheme.colors.onErrorContainer)
    }
}
