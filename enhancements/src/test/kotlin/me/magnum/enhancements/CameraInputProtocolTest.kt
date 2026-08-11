package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraInputProtocolTest {
    @Test
    fun deadzoneAndRecenterAreDeterministic() {
        val protocol = RuntimeTransientInputAdapter(deadzone = 0.1f)

        assertEquals(0, protocol.update(0.05f, 0f).axisXQ12.toInt())
        assertEquals(4096, protocol.update(1f, 0f).axisXQ12.toInt())
        assertEquals(1, protocol.action().actionSequence.toInt())
        assertEquals(0, protocol.neutral().flags.toInt())
        assertEquals(1, protocol.neutral().actionSequence.toInt())
    }
}
