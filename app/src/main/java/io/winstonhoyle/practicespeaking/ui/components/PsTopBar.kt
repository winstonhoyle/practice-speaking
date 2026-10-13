package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/** The app's top bar: a title, a back arrow when [onBack] is set, and optional [actions] on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = PsTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(PsIcons.Back, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PsTheme.colors.surface),
    )
}

@PreviewPsThemes
@Composable
private fun PsTopBarPreview() =
    PreviewSurface {
        PsTopBar(title = "Conversation", onBack = {}, actions = {
            IconButton(onClick = {}) { Icon(PsIcons.Search, contentDescription = "Search") }
        })
    }
