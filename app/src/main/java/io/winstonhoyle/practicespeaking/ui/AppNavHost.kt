package io.winstonhoyle.practicespeaking.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.winstonhoyle.practicespeaking.AppContainer
import io.winstonhoyle.practicespeaking.ui.conversation.ConversationRoute
import io.winstonhoyle.practicespeaking.ui.conversation.ConversationViewModel
import io.winstonhoyle.practicespeaking.ui.history.HistoryRoute
import io.winstonhoyle.practicespeaking.ui.history.HistoryViewModel
import io.winstonhoyle.practicespeaking.ui.session.SessionDetailRoute
import io.winstonhoyle.practicespeaking.ui.session.SessionDetailViewModel
import io.winstonhoyle.practicespeaking.ui.settings.SettingsRoute
import io.winstonhoyle.practicespeaking.ui.settings.SettingsViewModel

object Routes {
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    /** A new conversation, or with `sessionId` a saved one to continue. */
    const val CONVERSATION = "conversation?sessionId={sessionId}"
    const val SESSION = "session/{sessionId}?messageId={messageId}"

    fun session(sessionId: Long, messageId: Long?) = "session/$sessionId?messageId=${messageId ?: -1}"

    fun conversation(continueSessionId: Long? = null) = "conversation" + (continueSessionId?.let { "?sessionId=$it" } ?: "")
}

/**
 * The app's screens. History is home. On first launch, or when the chosen provider has no key,
 * the app starts on Settings instead ([startOnSettings]).
 */
@Composable
fun AppNavHost(container: AppContainer, startOnSettings: Boolean, nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = if (startOnSettings) Routes.SETTINGS else Routes.HISTORY) {
        composable(Routes.HISTORY) {
            HistoryRoute(
                viewModel = screenViewModel { HistoryViewModel(container.sessions) },
                onOpenSession = { session, message -> nav.navigate(Routes.session(session, message)) },
                onNewConversation = { nav.navigate(Routes.conversation()) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val hasBack = nav.previousBackStackEntry != null
            SettingsRoute(
                viewModel = screenViewModel { SettingsViewModel(container.settings, container.tutorModels) },
                onBack = if (hasBack) ({ nav.popBackStack() }) else null,
                onStart = {
                    if (nav.previousBackStackEntry?.destination?.route == Routes.CONVERSATION) {
                        nav.popBackStack()
                    } else {
                        nav.navigate(Routes.conversation()) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            Routes.CONVERSATION,
            arguments =
                listOf(
                    navArgument("sessionId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) { entry ->
            val continueSessionId = entry.arguments?.getLong("sessionId")?.takeIf { it >= 0 }
            ConversationRoute(
                viewModel =
                    screenViewModel {
                        ConversationViewModel(
                            container.settings,
                            container.sessions,
                            container.prompts,
                            container.tutorModels,
                            container.speaker,
                            container.translator,
                            continueSessionId,
                        )
                    },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onFinish = { nav.goHome() },
                onBack = { nav.goHome() },
            )
        }
        composable(
            Routes.SESSION,
            arguments =
                listOf(
                    navArgument("sessionId") { type = NavType.LongType },
                    navArgument("messageId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
            val messageId = entry.arguments?.getLong("messageId")?.takeIf { it >= 0 }
            SessionDetailRoute(
                viewModel =
                    screenViewModel {
                        SessionDetailViewModel(
                            container.sessions,
                            sessionId,
                            container.settings,
                            container.tutorModels,
                            container.translator,
                            container.speaker,
                        )
                    },
                scrollToMessageId = messageId,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onContinue = { nav.navigate(Routes.conversation(sessionId)) },
                onBack = { nav.popBackStack() },
            )
        }
    }
}

/** A ViewModel scoped to the current screen, built by [create] from the app's dependencies. */
@Composable
private inline fun <reified VM : ViewModel> screenViewModel(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })

/** Back to History, whatever is on the stack (the conversation may have started from Settings). */
private fun NavHostController.goHome() {
    if (!popBackStack(Routes.HISTORY, inclusive = false)) {
        navigate(Routes.HISTORY) { popUpTo(graph.id) { inclusive = true } }
    }
}
