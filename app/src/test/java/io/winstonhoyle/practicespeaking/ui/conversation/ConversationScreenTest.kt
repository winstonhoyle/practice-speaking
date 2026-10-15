package io.winstonhoyle.practicespeaking.ui.conversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ConversationScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun editingBack_removesLaterMessagesAtOnce() {
        val chat =
            listOf(
                ChatMessage(1, isUser = true, text = "Привет"),
                ChatMessage(2, isUser = false, text = "Здравствуй!"),
                ChatMessage(3, isUser = true, text = "Как дела"),
                ChatMessage(4, isUser = false, text = "Хорошо, спасибо."),
            )
        var state by mutableStateOf(ConversationUiState(loading = false, messages = chat))
        compose.setContent {
            PsTheme {
                ConversationScreen(
                    state = state,
                    micAvailable = true,
                    onPressStart = {},
                    onPressEnd = {},
                    onSend = {},
                    onCommand = {},
                    onReplay = {},
                    onTranslate = {},
                    onEdit = {},
                    onDraftChange = {},
                    onErrorAction = {},
                    onEndSession = {},
                    onOpenSettings = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }
        compose.waitForIdle()

        // Hold the clock: the cut messages must be gone on the next frames, not after an animation.
        compose.mainClock.autoAdvance = false
        state = state.copy(messages = chat.take(2), draft = "Как дела")
        Snapshot.sendApplyNotifications()
        repeat(3) { compose.mainClock.advanceTimeByFrame() }

        compose.onNodeWithText("Хорошо, спасибо.").assertDoesNotExist()
        compose.onNodeWithText("Здравствуй!").assertExists()
    }
}
