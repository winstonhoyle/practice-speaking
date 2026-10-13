package io.winstonhoyle.practicespeaking.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.R
import io.winstonhoyle.practicespeaking.ai.Provider
import io.winstonhoyle.practicespeaking.data.db.MessageEntity
import io.winstonhoyle.practicespeaking.data.db.SearchHit
import io.winstonhoyle.practicespeaking.data.db.SessionEntity
import io.winstonhoyle.practicespeaking.data.db.SessionRow
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.prompt.Summary
import io.winstonhoyle.practicespeaking.prompt.TutorLanguage
import io.winstonhoyle.practicespeaking.speech.VoiceStatus
import io.winstonhoyle.practicespeaking.ui.conversation.ChatMessage
import io.winstonhoyle.practicespeaking.ui.conversation.ConversationScreen
import io.winstonhoyle.practicespeaking.ui.conversation.ConversationUiState
import io.winstonhoyle.practicespeaking.ui.history.HistoryScreen
import io.winstonhoyle.practicespeaking.ui.history.HistoryUiState
import io.winstonhoyle.practicespeaking.ui.session.SessionDetailScreen
import io.winstonhoyle.practicespeaking.ui.session.SessionDetailUiState
import io.winstonhoyle.practicespeaking.ui.settings.KeyCheck
import io.winstonhoyle.practicespeaking.ui.settings.SettingsScreen
import io.winstonhoyle.practicespeaking.ui.settings.SettingsUiState
import io.winstonhoyle.practicespeaking.ui.theme.PsDarkColorScheme
import io.winstonhoyle.practicespeaking.ui.theme.PsLightColorScheme
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real screens with sample data for the README: one image per screen and theme in
 * docs/images/screens/, plus a side-by-side montage per theme. Runs only with -Pscreenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ScreensScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val docsImages get() = File(System.getProperty("screenshotDir")!!)

    @Before
    fun gate() {
        assumeTrue(System.getProperty("screenshots") == "true")
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun light() = renderAll(dark = false)

    @Test
    fun dark() = renderAll(dark = true)

    /** The GitHub social preview (1280 x 640): the launcher icon, the app name, and one line about it. */
    @Test
    fun socialPreview() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = PsLightColorScheme
        val out = Bitmap.createBitmap(1280, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out).apply { drawColor(colors.background.toArgb()) }
        // The icon's visible 72dp square, drawn as a rounded tile like the logo in docs/images/logo.svg.
        val tile = RectF(120f, 200f, 360f, 440f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ic_launcher_background) }
        canvas.drawRoundRect(tile, 60f, 60f, paint)
        val scale = tile.width() / 72f
        context.getDrawable(R.drawable.ic_launcher_foreground)!!.apply {
            setBounds(
                (tile.left - 18 * scale).toInt(),
                (tile.top - 18 * scale).toInt(),
                (tile.right + 18 * scale).toInt(),
                (tile.bottom + 18 * scale).toInt(),
            )
            draw(canvas)
        }
        val nunito = ResourcesCompat.getFont(context, R.font.nunito)!!
        val title =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(nunito, 800, false)
                textSize = 84f
                color = colors.onBackground.toArgb()
            }
        val subtitle =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(nunito, 500, false)
                textSize = 38f
                color = colors.onSurfaceVariant.toArgb()
            }
        canvas.drawText(context.getString(R.string.app_name), 420f, 318f, title)
        canvas.drawText("Talk with an AI tutor in five languages.", 424f, 384f, subtitle)
        save(out, File(docsImages, "social-preview.png"))
    }

    private fun renderAll(dark: Boolean) {
        val theme = if (dark) "dark" else "light"
        val dir = File(docsImages, "screens").apply { mkdirs() }
        // One composition whose screen we swap, so every shot uses the same window.
        var index by mutableIntStateOf(0)
        val all = SCREENS + EMPTY_STATES
        compose.setContent { PsTheme(darkTheme = dark) { all[index].second() } }
        val shots =
            all
                .mapIndexed { i, (name, _) ->
                    index = i
                    compose.waitForIdle()
                    capture().also { bitmap -> save(bitmap, File(dir, "$name-$theme.png")) }
                }.take(SCREENS.size)
        val background = (if (dark) PsDarkColorScheme else PsLightColorScheme).surfaceContainer.toArgb()
        save(montage(shots, background), File(docsImages, "screens-$theme.png"))
    }

    /**
     * Draws the content view, then any other window on top of it (the summary sheet is a dialog).
     * captureToImage's PixelCopy path hangs under Robolectric, so the views are drawn directly.
     */
    private fun capture(): Bitmap {
        val content = compose.activity.findViewById<View>(android.R.id.content)
        val origin = IntArray(2).also { content.getLocationOnScreen(it) }
        val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        content.draw(canvas)
        for (window in windowRoots() - content.rootView) {
            val at = IntArray(2).also { window.getLocationOnScreen(it) }
            canvas.withTranslation((at[0] - origin[0]).toFloat(), (at[1] - origin[1]).toFloat()) { window.draw(this) }
        }
        return bitmap
    }

    /** Every attached window's root view, in stacking order. Robolectric has no public API for this. */
    @Suppress("UNCHECKED_CAST")
    private fun windowRoots(): List<View> {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val views = global.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<View>
        return views.filter { it.isAttachedToWindow && it.width > 0 }
    }

    /** The screens side by side at half size, each with rounded corners, on a tinted background. */
    private fun montage(shots: List<Bitmap>, background: Int): Bitmap {
        val gap = 32f
        val scale = 0.5f
        val w = shots.first().width * scale
        val h = shots.first().height * scale
        val out = Bitmap.createBitmap((shots.size * w + (shots.size + 1) * gap).toInt(), (h + 2 * gap).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out).apply { drawColor(background) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        shots.forEachIndexed { i, shot ->
            val rect = RectF(gap + i * (w + gap), gap, gap + i * (w + gap) + w, gap + h)
            val save = canvas.save()
            canvas.clipPath(android.graphics.Path().apply { addRoundRect(rect, 24f, 24f, android.graphics.Path.Direction.CW) })
            canvas.drawBitmap(shot, null, rect, paint)
            canvas.restoreToCount(save)
        }
        return out
    }

    private fun save(bitmap: Bitmap, file: File) = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private companion object {
        const val STARTED = 1_791_900_000_000L

        const val CORRECTION = "в магазине → в магазин | Motion toward a place takes the accusative."
        const val MISTAKES = "в магазине → в магазин"
        const val NEW_WORDS = "хлеб — bread, сыр — cheese"
        const val TIP = "Practise the accusative after в and на with verbs of motion."

        val chat =
            listOf(
                ChatMessage(1, isUser = true, text = "Привет! Вчера я ходил в магазине.", correction = CORRECTION),
                ChatMessage(2, isUser = false, text = "Привет! Ты ходил в магазин? Что ты купил?"),
                ChatMessage(3, isUser = true, text = "Я купил хлеб и сыр."),
                ChatMessage(4, isUser = false, text = "Отлично! А какой сыр ты любишь больше всего?"),
            )

        val SCREENS: List<Pair<String, @Composable () -> Unit>> =
            listOf(
                "conversation" to {
                    ConversationScreen(
                        state =
                            ConversationUiState(
                                loading = false,
                                language = TutorLanguage.RUSSIAN,
                                provider = Provider.ANTHROPIC,
                                messages = chat,
                                voiceStatus = VoiceStatus.Ready,
                            ),
                        micAvailable = true,
                        onPressStart = {},
                        onPressEnd = {},
                        onSend = {},
                        onCommand = {},
                        onReplay = {},
                        onErrorAction = {},
                        onEndSession = {},
                        onOpenSettings = {},
                        onFinish = {},
                        onBack = {},
                    )
                },
                "summary" to {
                    ConversationScreen(
                        state =
                            ConversationUiState(
                                loading = false,
                                language = TutorLanguage.RUSSIAN,
                                provider = Provider.ANTHROPIC,
                                messages = chat,
                                voiceStatus = VoiceStatus.Ready,
                                ended = true,
                                summary = Summary(MISTAKES, NEW_WORDS, TIP),
                            ),
                        micAvailable = true,
                        onPressStart = {},
                        onPressEnd = {},
                        onSend = {},
                        onCommand = {},
                        onReplay = {},
                        onErrorAction = {},
                        onEndSession = {},
                        onOpenSettings = {},
                        onFinish = {},
                        onBack = {},
                    )
                },
                "history" to {
                    HistoryScreen(
                        state = HistoryUiState(loading = false, sessions = sessions),
                        onQueryChange = {},
                        onDelete = {},
                        onOpenSession = { _, _ -> },
                        onNewConversation = {},
                        onOpenSettings = {},
                        onDismissError = {},
                    )
                },
                "search" to {
                    HistoryScreen(
                        state =
                            HistoryUiState(
                                loading = false,
                                sessions = sessions,
                                query = "магазин",
                                results =
                                    listOf(
                                        SearchHit(1, 1, chat[0].text, CORRECTION, "RUSSIAN", chat[0].text, STARTED),
                                        SearchHit(2, 1, chat[1].text, null, "RUSSIAN", chat[0].text, STARTED),
                                        SearchHit(
                                            21,
                                            6,
                                            "Я не люблю ходить в магазин по субботам.",
                                            null,
                                            "RUSSIAN",
                                            "Что ты делаешь в выходные?",
                                            STARTED - 6 * 86_400_000L,
                                        ),
                                    ),
                            ),
                        onQueryChange = {},
                        onDelete = {},
                        onOpenSession = { _, _ -> },
                        onNewConversation = {},
                        onOpenSettings = {},
                        onDismissError = {},
                    )
                },
                "session" to {
                    SessionDetailScreen(
                        state =
                            SessionDetailUiState(
                                loading = false,
                                session =
                                    SessionEntity(
                                        id = 1,
                                        language = "RUSSIAN",
                                        provider = "ANTHROPIC",
                                        model = Provider.ANTHROPIC.defaultModel,
                                        startedAt = STARTED,
                                        endedAt = STARTED + 900_000,
                                        title = chat.first().text,
                                        summaryMistakes = MISTAKES,
                                        summaryNewWords = NEW_WORDS,
                                        summaryTip = TIP,
                                    ),
                                messages =
                                    chat.map {
                                        MessageEntity(
                                            id = it.id,
                                            sessionId = 1,
                                            role = if (it.isUser) MessageEntity.ROLE_USER else MessageEntity.ROLE_ASSISTANT,
                                            text = it.text,
                                            correction = it.correction,
                                            createdAt = STARTED,
                                        )
                                    },
                            ),
                        scrollToMessageId = null,
                        onBack = {},
                    )
                },
                "settings" to {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                loading = false,
                                provider = Provider.ANTHROPIC,
                                language = TutorLanguage.RUSSIAN,
                                apiKey = "sk-ant-example-key-for-screenshots",
                                keyCheck = KeyCheck.Valid,
                            ),
                        onBack = {},
                        onProvider = {},
                        onApiKey = {},
                        onModel = {},
                        onLanguage = {},
                        onThemeMode = {},
                        onTestKey = {},
                        onStart = {},
                    )
                },
            )

        /** Saved one by one (not in the montage) to check that empty states sit in the middle of the screen. */
        val EMPTY_STATES: List<Pair<String, @Composable () -> Unit>> =
            listOf(
                "conversation-empty" to {
                    ConversationScreen(
                        state = ConversationUiState(loading = false, voiceStatus = VoiceStatus.Ready),
                        micAvailable = true,
                        onPressStart = {},
                        onPressEnd = {},
                        onSend = {},
                        onCommand = {},
                        onReplay = {},
                        onErrorAction = {},
                        onEndSession = {},
                        onOpenSettings = {},
                        onFinish = {},
                        onBack = {},
                    )
                },
                "history-error" to {
                    HistoryScreen(
                        state = HistoryUiState(loading = false, sessions = sessions, error = StorageError.DatabaseFailed("disk I/O error")),
                        onQueryChange = {},
                        onDelete = {},
                        onOpenSession = { _, _ -> },
                        onNewConversation = {},
                        onOpenSettings = {},
                        onDismissError = {},
                    )
                },
                "history-empty" to {
                    HistoryScreen(
                        state = HistoryUiState(loading = false),
                        onQueryChange = {},
                        onDelete = {},
                        onOpenSession = { _, _ -> },
                        onNewConversation = {},
                        onOpenSettings = {},
                        onDismissError = {},
                    )
                },
            )

        val sessions: List<SessionRow>
            get() =
                listOf(
                    row(1, TutorLanguage.RUSSIAN, Provider.ANTHROPIC, "Привет! Вчера я ходил в магазине.", 8),
                    row(2, TutorLanguage.SPANISH, Provider.OPENAI, "Hola, ¿cómo estás? Quiero practicar.", 12),
                    row(3, TutorLanguage.FRENCH, Provider.GEMINI, "Bonjour ! Je voudrais parler de mon week-end.", 6),
                    row(4, TutorLanguage.GERMAN, Provider.ANTHROPIC, "Ich habe gestern einen Film gesehen.", 10),
                    row(5, TutorLanguage.PORTUGUESE, Provider.OPENAI, "Oi! Vamos falar sobre comida brasileira?", 4),
                )

        /** Session [id] started `id - 1` days before [STARTED], so the list reads newest first. */
        fun row(id: Long, language: TutorLanguage, provider: Provider, title: String, count: Int) =
            SessionRow(id, language.name, provider.name, provider.defaultModel, STARTED - (id - 1) * 86_400_000L, null, title, count)
    }
}
