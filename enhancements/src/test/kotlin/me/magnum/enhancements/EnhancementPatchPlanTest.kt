package me.magnum.enhancements

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EnhancementPatchPlanTest {
    @Test
    fun appliesOnlyTemporaryCopyPatchesInDeclaredOrder() {
        val plan = EnhancementPatchPlan(
            temporaryCopyPatches = listOf(
                EnhancementPatch(EnhancementPatchType.IPS, "first.ips", EnhancementPatchApply.TEMPORARY_COPY),
            ),
            runtimePatches = emptyList(),
        )
        val ips = byteArrayOf(
            *"PATCH".encodeToByteArray(),
            0, 0, 0, 0, 1, 9,
            *"EOF".encodeToByteArray(),
        )

        assertArrayEquals(byteArrayOf(9), plan.applyTemporaryCopy(ByteArray(1), mapOf("first.ips" to ips)))
    }
}
