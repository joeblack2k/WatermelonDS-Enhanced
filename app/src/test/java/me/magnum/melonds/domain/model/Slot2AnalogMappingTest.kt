package me.magnum.melonds.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Slot2AnalogMappingTest {
    @Test
    fun defaultsRemainBackwardCompatible() {
        val mapping = Slot2AnalogMapping()

        assertNull(mapping.effectiveDeviceId())
        assertEquals(Slot2AnalogMapping.DEFAULT_AXIS_X_CODE, mapping.axisXCode)
        assertEquals(Slot2AnalogMapping.DEFAULT_AXIS_Y_CODE, mapping.axisYCode)
        assertEquals(Slot2AnalogMapping.DEFAULT_DEADZONE, mapping.normalizedDeadzone())
    }

    @Test
    fun deviceFilterOnlyAppliesWhenEnabled() {
        val mapping = Slot2AnalogMapping(deviceId = 42, useDeviceFilter = false)
        assertNull(mapping.effectiveDeviceId())

        assertEquals(42, mapping.copy(useDeviceFilter = true).effectiveDeviceId())
    }

    @Test
    fun deadzoneIsClampedToSupportedRange() {
        assertEquals(0f, Slot2AnalogMapping(deadzone = -1f).normalizedDeadzone())
        assertEquals(1f, Slot2AnalogMapping(deadzone = 2f).normalizedDeadzone())
    }
}
