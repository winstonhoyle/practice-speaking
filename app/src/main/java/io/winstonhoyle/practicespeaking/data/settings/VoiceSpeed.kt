package io.winstonhoyle.practicespeaking.data.settings

/** How fast the tutor's voice speaks. [rate] is TextToSpeech's speech rate (1.0 is the engine's normal). */
enum class VoiceSpeed(val rate: Float) {
    Slow(0.7f),
    Relaxed(0.85f),
    Normal(1.0f),
    ;

    /** One step slower, stopping at [Slow]. The Slower command uses it. */
    fun slower(): VoiceSpeed = entries[(ordinal - 1).coerceAtLeast(0)]

    companion object {
        val DEFAULT = Normal
    }
}
