package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancementActivationTest {
    @Test
    fun goodAddonSurvivesFailingAddon() {
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

        assertEquals(setOf("good"), result.activeAddOnIds)
        assertEquals(setOf("bad"), result.failedAddOnIds)
    }
}
