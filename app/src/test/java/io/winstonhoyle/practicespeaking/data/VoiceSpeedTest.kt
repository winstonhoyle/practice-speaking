package io.winstonhoyle.practicespeaking.data

import io.winstonhoyle.practicespeaking.data.settings.VoiceSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSpeedTest {
    @Test
    fun slowerStepsDownAndStopsAtSlow() {
        assertEquals(VoiceSpeed.Relaxed, VoiceSpeed.Normal.slower())
        assertEquals(VoiceSpeed.Slow, VoiceSpeed.Relaxed.slower())
        assertEquals(VoiceSpeed.Slow, VoiceSpeed.Slow.slower())
    }
}
