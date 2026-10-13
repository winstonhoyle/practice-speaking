package io.winstonhoyle.practicespeaking.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun commandChipRow_emitsTheTappedCommand() {
        val tapped = mutableListOf<TutorCommand>()
        compose.setContent { PsTheme { CommandChipRow(onCommand = { tapped += it }) } }

        compose.onNodeWithText("Translate").performClick()
        compose.onNodeWithText("End session").performClick()

        assertEquals(listOf(TutorCommand.Translate, TutorCommand.EndSession), tapped)
    }

    @Test
    fun commandChipRow_disabled() {
        compose.setContent { PsTheme { CommandChipRow(onCommand = {}, enabled = false) } }
        compose.onNodeWithText("Slower").assertIsNotEnabled()
    }

    @Test
    fun micButton_pressAndReleaseStartsThenStops() {
        val events = mutableListOf<String>()
        compose.setContent {
            PsTheme { MicButton(MicState.Idle, onPressStart = { events += "start" }, onPressEnd = { events += "end" }) }
        }

        compose.onNodeWithContentDescription("Hold to talk").performTouchInput {
            down(center)
            up()
        }

        assertEquals(listOf("start", "end"), events)
    }

    @Test
    fun micButton_ignoresPressWhileThinking() {
        val events = mutableListOf<String>()
        compose.setContent { PsTheme { MicButton(MicState.Thinking, { events += "start" }, { events += "end" }) } }

        compose.onNodeWithContentDescription("Thinking…").performTouchInput {
            down(center)
            up()
        }

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun badges_drawEveryFlagAndLogo() {
        // Inflating each vector here catches a drawable the converter got wrong (gradients, units).
        compose.setContent {
            PsTheme {
                Column {
                    TutorLanguage.entries.forEach { LanguageBadge(it) }
                    Provider.entries.forEach { ProviderBadge(it) }
                }
            }
        }
        compose.onAllNodesWithTag(PILL_ICON_TAG).assertCountEquals(TutorLanguage.entries.size + Provider.entries.size)
        compose.onNodeWithContentDescription("Spanish").assertExists()
        compose.onNodeWithText("Gemini").assertExists()
    }

    @Test
    fun providerBadge_withoutALogoIsTextOnly() {
        compose.setContent { PsTheme { ProviderBadge("Mistral") } }
        compose.onNodeWithText("Mistral").assertExists()
        compose.onAllNodesWithTag(PILL_ICON_TAG).assertCountEquals(0)
    }

    @Test
    fun everyLanguageHasItsOwnFlag() {
        val flags = TutorLanguage.entries.map { it.flag }
        assertEquals(flags.size, flags.distinct().size)
    }

    @Test
    fun errorBanner_actionFires() {
        var clicked = false
        compose.setContent { PsTheme { ErrorBanner("Check your API key", actionLabel = "Open Settings", onAction = { clicked = true }) } }

        compose.onNodeWithText("Open Settings").performClick()

        assert(clicked)
    }

    @Test
    fun correctionCard_showsBothFormsAndExplanation() {
        compose.setContent { PsTheme { CorrectionCard("в магазине", "в магазин", "Accusative for direction.") } }
        compose.onNodeWithContentDescription("Correction: в магазине → в магазин. Accusative for direction.").assertExists()
    }

    @Test
    fun secretTextField_togglesVisibility() {
        compose.setContent { PsTheme { SecretTextField("secret", {}, "API key", "sk-…") } }

        compose.onNodeWithContentDescription("Show key").performClick()

        compose.onNodeWithContentDescription("Hide key").assertExists()
    }
}
