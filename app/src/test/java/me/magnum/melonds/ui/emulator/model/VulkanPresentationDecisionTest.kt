package me.magnum.melonds.ui.emulator.model

import me.magnum.enhancements.EnhancementPresentationMode
import me.magnum.enhancements.EnhancementPresentationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VulkanPresentationDecisionTest {
    @Test
    fun nullStateKeepsHybridPresentation() {
        assertFalse(useNative43Fallback(null))
    }

    @Test
    fun effectiveLayerAwareKeepsHybridPresentation() {
        assertFalse(
            useNative43Fallback(
                EnhancementPresentationState(
                    requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                    effective = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                ),
            ),
        )
    }

    @Test
    fun requestedButIneffectiveLayerAwareUsesNative43() {
        assertTrue(
            useNative43Fallback(
                EnhancementPresentationState(
                    requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                    effective = EnhancementPresentationMode.NATIVE_4_3,
                ),
            ),
        )
    }
}
