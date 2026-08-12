package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun neutralizationLeavesTheNextFrameDeliverable() {
        val protocol = RuntimeTransientInputAdapter()

        protocol.update(0.8f, -0.6f)
        val neutral = protocol.neutral()
        val next = protocol.update(-0.8f, 0.6f)

        assertEquals(0, neutral.axisXQ12.toInt())
        assertEquals(0, neutral.axisYQ12.toInt())
        assertEquals(0, neutral.scalar.toInt())
        assertEquals(0, neutral.flags.toInt())
        assertNotEquals(neutral.axisXQ12, next.axisXQ12)
        assertNotEquals(neutral.axisYQ12, next.axisYQ12)
        assertEquals(1, next.flags.toInt())
    }

    @Test
    fun everyDeclaredLifecycleEventProducesANeutralFrame() {
        val protocol = RuntimeTransientInputAdapter()
        val neutralizationByEvent = listOf(
            EnhancementLifecycleEvent.PAUSE to { protocol.neutral() },
            EnhancementLifecycleEvent.RESET to { protocol.neutral() },
            EnhancementLifecycleEvent.SAVE_STATE_LOAD to { protocol.neutral() },
            EnhancementLifecycleEvent.CONTROLLER_DISCONNECT to { protocol.neutral() },
            EnhancementLifecycleEvent.ACTIVITY_REPLACEMENT to { protocol.neutral() },
            EnhancementLifecycleEvent.EMULATOR_STOP to { protocol.neutral() },
            EnhancementLifecycleEvent.SESSION_TEARDOWN to { protocol.neutral() },
        )

        protocol.update(0.8f, -0.6f)
        assertEquals(EnhancementLifecycleEvent.entries.size, neutralizationByEvent.size)
        assertEquals(
            EnhancementLifecycleEvent.entries.toSet(),
            neutralizationByEvent.map { it.first }.toSet(),
        )
        neutralizationByEvent.forEach { (_, neutralize) ->
            val neutral = neutralize()
            assertEquals(0, neutral.axisXQ12.toInt())
            assertEquals(0, neutral.axisYQ12.toInt())
            assertEquals(0, neutral.scalar.toInt())
            assertEquals(0, neutral.flags.toInt())
        }
    }

    @Test
    fun repeatedNeutralizationDoesNotDuplicateRecenterSequence() {
        val protocol = RuntimeTransientInputAdapter()

        val recenter = protocol.action()
        val firstNeutral = protocol.neutral()
        val secondNeutral = protocol.neutral()
        val nextRecenter = protocol.action()

        assertEquals(1, recenter.actionSequence.toInt())
        assertEquals(1, firstNeutral.actionSequence.toInt())
        assertEquals(firstNeutral, secondNeutral)
        assertEquals(2, nextRecenter.actionSequence.toInt())
    }
}
