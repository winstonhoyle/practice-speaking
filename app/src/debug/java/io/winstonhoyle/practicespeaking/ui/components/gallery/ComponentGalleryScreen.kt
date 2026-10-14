package io.winstonhoyle.practicespeaking.ui.components.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.ui.components.CommandChipRow
import io.winstonhoyle.practicespeaking.ui.components.CorrectionCard
import io.winstonhoyle.practicespeaking.ui.components.EmptyState
import io.winstonhoyle.practicespeaking.ui.components.ErrorBanner
import io.winstonhoyle.practicespeaking.ui.components.FlagImage
import io.winstonhoyle.practicespeaking.ui.components.LanguageBadge
import io.winstonhoyle.practicespeaking.ui.components.MessageBubble
import io.winstonhoyle.practicespeaking.ui.components.MicButton
import io.winstonhoyle.practicespeaking.ui.components.MicState
import io.winstonhoyle.practicespeaking.ui.components.ProviderBadge
import io.winstonhoyle.practicespeaking.ui.components.PsDropdown
import io.winstonhoyle.practicespeaking.ui.components.PsIcons
import io.winstonhoyle.practicespeaking.ui.components.PsTopBar
import io.winstonhoyle.practicespeaking.ui.components.SearchField
import io.winstonhoyle.practicespeaking.ui.components.SearchResultItem
import io.winstonhoyle.practicespeaking.ui.components.SecretTextField
import io.winstonhoyle.practicespeaking.ui.components.SessionListItem
import io.winstonhoyle.practicespeaking.ui.components.SummaryContent
import io.winstonhoyle.practicespeaking.ui.components.ThinkingIndicator
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme

class ComponentGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ComponentGalleryScreen() }
    }
}

/** Debug-only catalogue of every component in every state, with a light/dark toggle. */
@Composable
fun ComponentGalleryScreen(modifier: Modifier = Modifier) {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    PsTheme(darkTheme = dark) {
        Scaffold(
            modifier = modifier,
            topBar = {
                PsTopBar("Components", actions = {
                    IconButton(onClick = { dark = !dark }) {
                        Icon(
                            if (dark) PsIcons.LightMode else PsIcons.DarkMode,
                            contentDescription = if (dark) "Light theme" else "Dark theme",
                        )
                    }
                })
            },
        ) { padding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg),
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = PsTheme.spacing.lg),
            ) {
                GalleryContent()
            }
        }
    }
}

/** Every component in every state. Shared by the gallery screen and the docs screenshot test. */
@Composable
fun GalleryContent(modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.lg), modifier = modifier) {
        Section("MessageBubble") {
            MessageBubble("Я иду в магазине за хлебом.", isUser = true, highlights = listOf(TextRange(6, 17)))
            MessageBubble("Приве́т! Как прошёл твой день?", isUser = false, translation = "Hi! How was your day?")
            MessageBubble("Хорошо, спасибо.", isUser = true)
        }
        Section("CorrectionCard") {
            CorrectionCard("в магазине", "в магази́н", "Motion toward a place takes the accusative.", onHearRight = {})
            CorrectionCard("je suis allé", "je suis allée", null)
        }
        Section("ThinkingIndicator") { ThinkingIndicator() }
        Section("MicButton") {
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.md)) {
                MicState.entries.take(3).forEach { MicButton(it, {}, {}) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.md)) {
                MicState.entries.drop(3).forEach { MicButton(it, {}, {}) }
            }
        }
        Section("CommandChipRow") {
            CommandChipRow(onCommand = {})
            CommandChipRow(onCommand = {}, enabled = false)
        }
        Section("SummaryContent (SummarySheet body)") {
            SummaryContent(
                mistakes = "в магазине → в магазин",
                newWords = "хлеб — bread, сходить — to go (and come back)",
                tip = "Practise the accusative after в and на.",
            )
        }
        Section("LanguageBadge / ProviderBadge") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm),
            ) {
                TutorLanguage.entries.forEach { LanguageBadge(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
                Provider.entries.forEach { ProviderBadge(it) }
                ProviderBadge("Text only")
            }
        }
        Section("PsDropdown / SecretTextField") {
            var language by remember { mutableStateOf(TutorLanguage.RUSSIAN) }
            PsDropdown("Language", TutorLanguage.entries, language, { it.displayName }, { language = it }, optionIcon = {
                FlagImage(it, width = PsTheme.sizes.icon)
            })
            var key by remember { mutableStateOf("") }
            SecretTextField(key, { key = it }, "API key", "sk-…", supportingText = { Text("Stored encrypted on this device") })
        }
        Section("SessionListItem") {
            SessionListItem(
                TutorLanguage.RUSSIAN,
                "Claude",
                "Привет, как дела?",
                "Oct 13, 2026",
                14,
                {},
                providerLogo = Provider.ANTHROPIC.logo,
            )
            SessionListItem(TutorLanguage.SPANISH, "Gemini", "¿Qué hiciste el fin de semana?", "Oct 12, 2026", 1, {
            }, providerLogo = Provider.GEMINI.logo)
        }
        Section("SearchField / SearchResultItem") {
            SearchField("магазин", {})
            SearchField("", {})
            SearchResultItem("Я иду в магазине за хлебом.", "магазин", TutorLanguage.RUSSIAN, "Привет, как дела?", "Oct 13", {})
        }
        Section("EmptyState") {
            EmptyState(PsIcons.Conversation, "No conversations yet.", actionLabel = "Start one", onAction = {})
        }
        Section("ErrorBanner") {
            ErrorBanner("Check your API key", actionLabel = "Open Settings", onAction = {})
            ErrorBanner("You're offline.", actionLabel = "Retry", onAction = {})
            ErrorBanner("The tutor couldn't answer that.")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PsTheme.spacing.sm)) {
        Text(title, style = PsTheme.typography.titleMedium, color = PsTheme.colors.primary)
        content()
        HorizontalDivider(Modifier.padding(top = PsTheme.spacing.sm))
    }
}
