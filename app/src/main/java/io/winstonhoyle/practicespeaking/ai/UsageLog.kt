package io.winstonhoyle.practicespeaking.ai

import android.util.Log
import io.winstonhoyle.practicespeaking.BuildConfig

/**
 * The token counts of one reply. Debug builds also log them so prompt caching can be checked.
 * Never logs keys or text.
 */
internal fun usageOf(provider: Provider, input: Long, output: Long, cached: Long): TokenUsage {
    if (BuildConfig.DEBUG) {
        Log.d("TutorUsage", "${provider.name} input=$input output=$output cached=$cached")
    }
    return TokenUsage(input, output, cached)
}
