package io.winstonhoyle.practicespeaking.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.error.AppError
import io.winstonhoyle.practicespeaking.error.SpeechError
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.error.TutorError
import io.winstonhoyle.practicespeaking.ui.components.BannerTone
import io.winstonhoyle.practicespeaking.ui.components.PsBanner

/** What the learner can do about an error. Each error offers exactly one. */
enum class ErrorAction(
    @StringRes val label: Int,
) {
    OpenSettings(R.string.action_open_settings),
    Retry(R.string.action_retry),
    EndSession(R.string.command_end_session),
    OpenAppSettings(R.string.action_app_settings),
    InstallVoice(R.string.action_install_voice),
    Dismiss(R.string.action_dismiss),
}

/** How an error is shown: its message, its one action, and whether it's a failure or a hint. */
data class ErrorPresentation(
    @StringRes val message: Int,
    val action: ErrorAction,
    val tone: BannerTone = BannerTone.Error,
)

/** The one table from error to UI. Sealed types make this `when` exhaustive. */
fun AppError.presentation(): ErrorPresentation =
    when (this) {
        TutorError.InvalidKey -> ErrorPresentation(R.string.error_invalid_key, ErrorAction.OpenSettings)
        TutorError.ModelNotFound -> ErrorPresentation(R.string.error_model_not_found, ErrorAction.OpenSettings)
        TutorError.RateLimited -> ErrorPresentation(R.string.error_rate_limited, ErrorAction.Retry)
        TutorError.ProviderDown -> ErrorPresentation(R.string.error_provider_down, ErrorAction.Retry)
        TutorError.Offline -> ErrorPresentation(R.string.error_offline, ErrorAction.Retry)
        TutorError.Refused -> ErrorPresentation(R.string.error_refused, ErrorAction.Dismiss)
        TutorError.ContextTooLong -> ErrorPresentation(R.string.error_context_too_long, ErrorAction.EndSession)
        TutorError.EmptyReply -> ErrorPresentation(R.string.error_empty_reply, ErrorAction.Retry)
        is TutorError.Unknown -> ErrorPresentation(R.string.error_unknown, ErrorAction.Retry)
        SpeechError.NothingHeard -> ErrorPresentation(R.string.speech_nothing_heard, ErrorAction.Dismiss, BannerTone.Info)
        SpeechError.NoPermission -> ErrorPresentation(R.string.speech_no_permission, ErrorAction.OpenAppSettings, BannerTone.Info)
        SpeechError.LanguageUnavailable -> ErrorPresentation(R.string.speech_language_unavailable, ErrorAction.Dismiss)
        SpeechError.ServiceUnavailable -> ErrorPresentation(R.string.speech_service_unavailable, ErrorAction.Dismiss)
        SpeechError.Busy -> ErrorPresentation(R.string.speech_busy, ErrorAction.Dismiss)
        SpeechError.VoiceMissing -> ErrorPresentation(R.string.speech_voice_missing, ErrorAction.InstallVoice, BannerTone.Info)
        is SpeechError.Failed -> ErrorPresentation(R.string.speech_failed, ErrorAction.Dismiss)
        StorageError.KeyUnreadable -> ErrorPresentation(R.string.storage_key_unreadable, ErrorAction.OpenSettings)
        is StorageError.DatabaseFailed -> ErrorPresentation(R.string.storage_database_failed, ErrorAction.Dismiss)
    }

/** The message for [error]. Errors without a specific cause include their code, for bug reports. */
@Composable
fun errorMessage(error: AppError): String {
    val message = error.presentation().message
    return when (error) {
        is TutorError.Unknown -> stringResource(message, error.code, error.message.take(MAX_DETAIL))
        is SpeechError.Failed, is StorageError.DatabaseFailed -> stringResource(message, error.code)
        else -> stringResource(message)
    }
}

/** Shows any [AppError] the same way everywhere: its message, tone, and one action. */
@Composable
fun AppErrorBanner(error: AppError, onAction: (ErrorAction) -> Unit, modifier: Modifier = Modifier) {
    val presentation = error.presentation()
    PsBanner(
        message = errorMessage(error),
        tone = presentation.tone,
        modifier = modifier,
        actionLabel = stringResource(presentation.action.label),
        onAction = { onAction(presentation.action) },
    )
}

private const val MAX_DETAIL = 120
