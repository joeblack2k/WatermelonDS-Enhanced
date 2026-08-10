package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementRuntimeTest {
    private val identity = EnhancementRomIdentity("ASMP", "12345678", "")

    @Test
    fun rejectsMatchingDuplicateRuntimeProtocolOwners() {
        val ownerA = manifest("owner.a")
        val ownerB = manifest("owner.b")
        val catalog = EnhancementCatalog(listOf(ownerA, ownerB))

        try {
            catalog.createSession(identity, setOf(ownerA.id, ownerB.id))
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("more than one runtime protocol owner"))
            return
        }
        throw AssertionError("Expected duplicate runtime protocol owners to be rejected")
    }

    @Test
    fun preservesSingleRuntimeProtocolOwnerInput() {
        val owner = manifest(
            id = "owner",
            protocol = "camera-v1",
            axisX = 17,
            axisY = 19,
            invertX = true,
            invertY = false,
            deadzone = 0.23f,
            sensitivity = 1.7f,
        )

        val session = EnhancementCatalog(listOf(owner)).createSession(identity, setOf(owner.id))

        assertEquals(
            EnhancementRuntimeInput(
                protocol = "camera-v1",
                axisXCode = 17,
                axisYCode = 19,
                invertX = true,
                invertY = false,
                deadzone = 0.23f,
                sensitivity = 1.7f,
            ),
            session.runtimeInput,
        )
        assertFalse(session.runtimeInput == null)
    }

    private fun manifest(
        id: String,
        protocol: String = "camera-v1",
        axisX: Int = 1,
        axisY: Int = 2,
        invertX: Boolean = false,
        invertY: Boolean = false,
        deadzone: Float = 0.12f,
        sensitivity: Float = 1f,
    ) = EnhancementManifest(
        id = id,
        name = id,
        version = "1.0.0",
        match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
        capabilities = setOf(
            EnhancementCapability.CONTROLLER_AXIS_OWNER,
            EnhancementCapability.RUNTIME_INPUT_PROTOCOL,
        ),
        runtimeProtocol = protocol,
        runtimeAxisXCode = axisX,
        runtimeAxisYCode = axisY,
        runtimeInvertX = invertX,
        runtimeInvertY = invertY,
        runtimeDeadzone = deadzone,
        runtimeSensitivity = sensitivity,
    )
}
