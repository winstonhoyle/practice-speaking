package io.winstonhoyle.practicespeaking.ui.components

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecretTextFieldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val secure: Boolean
        get() = compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun theWindowBlocksScreenshotsOnlyWhileTheKeyIsShown() {
        compose.setContent { PsTheme { SecretTextField("sk-ant-secret", {}, "API key", "sk-ant-…") } }
        assertEquals(false, secure)

        compose.onNodeWithContentDescription("Show key").performClick()
        compose.waitForIdle()
        assertEquals(true, secure)

        compose.onNodeWithContentDescription("Hide key").performClick()
        compose.waitForIdle()
        assertEquals(false, secure)
    }
}
