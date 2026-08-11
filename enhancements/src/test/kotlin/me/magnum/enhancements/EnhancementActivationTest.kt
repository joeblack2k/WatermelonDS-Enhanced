package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancementActivationTest {
    @Test
    fun anyPreparationFailureFallsBackAllAddOns() {
        val requests = listOf(
            EnhancementActivationRequest("good", emptyList(), emptyList()),
            EnhancementActivationRequest(
                "bad",
                listOf(EnhancementRuntimeGuard("bad", 0x10, 0x20)),
                emptyList(),
            ),
        )

        val result = activateEnhancements(
            requests = requests,
            validateGuard = { it.addOnId == "good" },
            applyOverlay = { true },
        )

        assertEquals(emptySet<String>(), result.activeAddOnIds)
        assertEquals(setOf("good", "bad"), result.failedAddOnIds)
    }

    @Test
    fun overlayIsComposedAndAppliedAsOneAtomicOperation() {
        val applied = mutableListOf<List<EnhancementOverlayWord>>()
        val requests = listOf(
            EnhancementActivationRequest("one", emptyList(), listOf(EnhancementOverlayWord(1, 2, 3))),
            EnhancementActivationRequest("two", emptyList(), listOf(EnhancementOverlayWord(4, 5, 6))),
        )

        val result = activateEnhancements(requests, { true }) {
            applied += it
            false
        }

        assertEquals(emptySet<String>(), result.activeAddOnIds)
        assertEquals(setOf("one", "two"), result.failedAddOnIds)
        assertEquals(listOf(requests.flatMap { it.overlay }), applied)
    }
}
