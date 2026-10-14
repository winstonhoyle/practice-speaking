package io.winstonhoyle.practicespeaking.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.winstonhoyle.practicespeaking.R

/**
 * Masked API-key field with a show/hide toggle. [placeholder] shows the expected key prefix.
 * While the key is shown, the window blocks screenshots and the recent-apps preview.
 */
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
    if (revealed) SecureWindow()
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

/** Sets FLAG_SECURE on the hosting window for as long as this is in the composition. */
@Composable
private fun SecureWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@PreviewPsThemes
@Composable
private fun SecretTextFieldPreview() =
    PreviewSurface {
        SecretTextField("sk-ant-example", {}, "API key", "sk-ant-…")
    }
