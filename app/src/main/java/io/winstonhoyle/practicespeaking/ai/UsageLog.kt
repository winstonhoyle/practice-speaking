package io.winstonhoyle.practicespeaking.ai

import android.util.Log
import io.winstonhoyle.practicespeaking.BuildConfig

/** Debug builds log token counts so prompt caching can be checked. Never logs keys or text. */
internal fun logUsage(provider: Provider, input: Long, output: Long, cached: Long) {
    if (BuildConfig.DEBUG) {
        Log.d("TutorUsage", "${provider.name} input=$input output=$output cached=$cached")
    }
}
