package io.winstonhoyle.practicespeaking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.winstonhoyle.practicespeaking.data.settings.ThemeMode
import io.winstonhoyle.practicespeaking.ui.AppNavHost
import io.winstonhoyle.practicespeaking.ui.components.LoadingState
import io.winstonhoyle.practicespeaking.ui.theme.PsTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PracticeSpeakingApp).container
        setContent {
            val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
            PsTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                Surface {
                    // Decide once whether to open on Settings: no key saved for the chosen provider.
                    var startOnSettings by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        val provider = container.settings.provider.first()
                        startOnSettings = container.settings.apiKey(provider).first() == null
                    }
                    when (val start = startOnSettings) {
                        null -> LoadingState()
                        else -> AppNavHost(container, startOnSettings = start)
                    }
                }
            }
        }
    }
}
