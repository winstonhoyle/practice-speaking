package io.winstonhoyle.practicespeaking

import android.app.Application

class PracticeSpeakingApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
