package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

/**
 * Single-line text field with the app's defaults (model ID, typed messages). With [suggestions]
 * it also opens a menu of them, filtered by what's typed; any other text is still accepted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    suggestions: List<String> = emptyList(),
) {
    if (suggestions.isEmpty()) {
        OutlinedField(value, onValueChange, label, placeholder, supportingText, modifier)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    // A picked or empty value shows the whole list; while typing, only the matches.
    val shown =
        if (value.isBlank() || value in suggestions) suggestions else suggestions.filter { it.contains(value.trim(), ignoreCase = true) }
    ExposedDropdownMenuBox(expanded = expanded && shown.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded && shown.isNotEmpty(), onDismissRequest = { expanded = false }) {
            shown.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** The one-line outlined field behind [PsTextField], with or without its suggestion menu. */
@Composable
private fun OutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    supportingText: String?,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

/** How much weight a [PsButton] carries. */
enum class PsButtonStyle { Filled, Outlined, Text }

/** Filled for a screen's main action, outlined for secondary ones, text for links. */
@Composable
fun PsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PsButtonStyle = PsButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(PsTheme.sizes.iconSmall), strokeWidth = PsTheme.sizes.divider * 2)
        } else {
            Text(text)
        }
    }
    val sized = modifier.heightIn(min = PsTheme.sizes.minTouchTarget)
    val active = enabled && !loading
    when (style) {
        PsButtonStyle.Filled -> Button(onClick = onClick, enabled = active, modifier = sized) { content() }
        PsButtonStyle.Outlined -> OutlinedButton(onClick = onClick, enabled = active, modifier = sized) { content() }
        PsButtonStyle.Text -> TextButton(onClick = onClick, enabled = active, modifier = sized) { content() }
    }
}

/** A ✓ or ✗ line with a short message, e.g. the result of "Test key". */
@Composable
fun StatusLine(ok: Boolean, text: String, modifier: Modifier = Modifier) {
    val color = if (ok) PsTheme.extendedColors.success else PsTheme.colors.error
    MetaRow(modifier) {
        Icon(
            if (ok) PsIcons.Success else PsIcons.Failure,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(PsTheme.sizes.iconSmall),
        )
        Text(text, style = PsTheme.typography.bodyMedium, color = color)
    }
}

/** Typing fallback under the mic: a text field and a send button. */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val canSend = enabled && value.isNotBlank()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.chat_input_hint)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            shape = PsTheme.shapes.chip,
            modifier = Modifier.weight(1f),
        )
        FilledIconButton(onClick = onSend, enabled = canSend, modifier = Modifier.size(PsTheme.sizes.minTouchTarget)) {
            Icon(PsIcons.Send, contentDescription = stringResource(R.string.cd_send))
        }
    }
}

@PreviewPsThemes
@Composable
private fun InputsPreview() =
    PreviewSurface {
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.md)) {
            PsTextField("claude-haiku-5-5", {}, "Model")
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
                PsButton("Test key", {}, style = PsButtonStyle.Outlined)
                PsButton("Where do I get a key?", {}, style = PsButtonStyle.Text)
                PsButton("Start talking", {})
            }
            StatusLine(true, "Key works")
            StatusLine(false, "Check your API key")
            ChatInputBar("Привет", {}, {})
        }
    }

/**
 * Icon-only action: top bars, the actions under a message, and the correction card. [subtle]
 * draws a smaller icon in the secondary text color, for actions that sit beside content
 * instead of leading a screen. The touch target stays 48dp either way.
 */
@Composable
fun PsIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtle: Boolean = false,
) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        if (subtle) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = PsTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(PsTheme.sizes.iconSmall),
            )
        } else {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}
