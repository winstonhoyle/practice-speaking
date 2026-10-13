package io.winstonhoyle.practicespeaking.ui.components.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import io.winstonhoyle.practicespeaking.ui.theme.PsDarkColorScheme
import io.winstonhoyle.practicespeaking.ui.theme.PsDarkExtendedColors
import io.winstonhoyle.practicespeaking.ui.theme.PsExtendedColors
import io.winstonhoyle.practicespeaking.ui.theme.PsLightColorScheme
import io.winstonhoyle.practicespeaking.ui.theme.PsLightExtendedColors
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

/** Writes the images in docs/images used by docs/DESIGN.md. Runs only with -Pscreenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h4000dp-xhdpi")
class GalleryScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir get() = File(System.getProperty("screenshotDir")!!).apply { mkdirs() }

    @Before
    fun gate() {
        assumeTrue(System.getProperty("screenshots") == "true")
        // Static frames: "Remove animations" stops the endless pulse/dots so Compose can go idle.
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun galleryLight() = capture("gallery-light.png", dark = false)

    @Test
    fun galleryDark() = capture("gallery-dark.png", dark = true)

    /** Palette swatches as SVG, generated from the theme tokens so the docs can't drift. */
    @Test
    fun palette() {
        val rows =
            listOf(
                "Light" to swatches(PsLightColorScheme, PsLightExtendedColors),
                "Dark" to swatches(PsDarkColorScheme, PsDarkExtendedColors),
            )
        val cell = 96
        val width = 16 + rows.first().second.size * (cell + 8)
        val svg =
            buildString {
                append(
                    """<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="${16 + rows.size * (cell + 40)}" font-family="sans-serif" font-size="11">""",
                )
                rows.forEachIndexed { r, (theme, colors) ->
                    val y = 8 + r * (cell + 40)
                    append("""<text x="8" y="${y + 12}" font-weight="bold" fill="#888">$theme</text>""")
                    colors.forEachIndexed { i, (name, color) ->
                        val x = 8 + i * (cell + 8)
                        append(
                            """<rect x="$x" y="${y + 20}" width="$cell" height="${cell - 30}" rx="12" fill="${hex(
                                color,
                            )}" stroke="#8884"/>""",
                        )
                        append("""<text x="$x" y="${y + cell + 4}" fill="#888">$name</text>""")
                        append("""<text x="$x" y="${y + cell + 18}" fill="#888">${hex(color)}</text>""")
                    }
                }
                append("</svg>\n")
            }
        File(outDir, "palette.svg").writeText(svg)
    }

    private fun swatches(scheme: ColorScheme, ext: PsExtendedColors) =
        listOf(
            "primary" to scheme.primary,
            "secondary" to scheme.secondary,
            "background" to scheme.background,
            "userBubble" to ext.userBubble,
            "tutorBubble" to ext.tutorBubble,
            "mistake" to ext.mistake,
            "success" to ext.success,
        ) + ext.languageAccents.map { (language, color) -> language.code to color }

    private fun hex(color: Color) = "#%06X".format(color.toArgb() and 0xFFFFFF)

    private fun capture(name: String, dark: Boolean) {
        compose.setContent { Framed(dark) { GalleryContent() } }
        compose.waitForIdle()
        // Draw the content view directly; captureToImage's PixelCopy path hangs under Robolectric.
        val view = compose.activity.findViewById<View>(android.R.id.content)
        val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(full))
        val cropped = Bitmap.createBitmap(full, 0, 0, full.width, contentHeight(full))
        File(outDir, name).outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Height down to the last row that differs from the background, plus a small margin. */
    private fun contentHeight(bitmap: Bitmap): Int {
        val background = bitmap.getPixel(0, bitmap.height - 1)
        val row = IntArray(bitmap.width)
        for (y in bitmap.height - 1 downTo 0) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { it != background }) return minOf(bitmap.height, y + 48)
        }
        return bitmap.height
    }

    @Composable
    private fun Framed(dark: Boolean, content: @Composable () -> Unit) {
        PsTheme(darkTheme = dark) {
            Surface { Box(Modifier.padding(PsTheme.spacing.lg)) { content() } }
        }
    }
}
