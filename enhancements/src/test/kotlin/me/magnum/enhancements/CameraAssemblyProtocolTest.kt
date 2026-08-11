package me.magnum.enhancements

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAssemblyProtocolTest {
    @Test
    fun assemblyDoesNotClaimAnUnverifiedRecenterImplementation() {
        val source = File(".").walkTopDown()
            .first { it.path.endsWith("sm64ds-eu-right-stick-camera/runtime/sm64ds_eu_smooth_camera.s") }
            .readText()
        assertTrue(source.contains("last-processed recenterSequence"))
        assertFalse(source.contains("ldrh	r2, [r0, #0x06]"))
        assertFalse(source.contains("strh	r3, [r8, #0x17c]"))
    }
}
