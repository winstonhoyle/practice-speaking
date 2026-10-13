package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.winstonhoyle.practicespeaking.R

/** Masked API-key field with a show/hide toggle. [placeholder] shows the expected key prefix. */
@Composable
fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null,
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    if (revealed) PsIcons.Hide else PsIcons.Show,
                    contentDescription = stringResource(if (revealed) R.string.cd_hide_secret else R.string.cd_show_secret),
                )
            }
        },
        supportingText = supportingText,
        modifier = modifier.fillMaxWidth(),
    )
}

@PreviewPsThemes
@Composable
private fun SecretTextFieldPreview() =
    PreviewSurface {
        SecretTextField("sk-ant-example", {}, "API key", "sk-ant-…")
    }
