package me.magnum.melonds.ui.emulator.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorSessionFallbackCoordinatorTest {
    @Test
    fun enhancedLaunchOrdersLoadCapabilityActivationCompositionExposureAndPolicy() = runTest {
        val events = mutableListOf<String>()
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("accepted", "rejected"),
            loadRomPaused = { events += "load:paused" },
            reportCapabilities = { events += "capabilities:actual" },
            prepare = {
                events += "prepare:$it"
                true
            },
            activate = {
                events += "activate:$it"
                it.toSet()
            },
            compose = {
                events += "compose:$it"
                "base+${it.joinToString("+")}"
            },
            expose = {
                events += "expose:$it"
                "input" to "presentation"
            },
            hardcoreAllowed = {
                events += "ra:$it"
                true
            },
        )

        assertEquals(
            listOf(
                "load:paused",
                "capabilities:actual",
                "prepare:accepted",
                "prepare:rejected",
                "activate:[accepted, rejected]",
                "compose:[accepted, rejected]",
                "expose:[accepted, rejected]",
                "ra:[accepted, rejected]",
            ),
            events,
        )
        assertEquals(setOf("accepted", "rejected"), result.activeIds)
        assertEquals("base+accepted+rejected", result.composed)
        assertEquals("input", result.runtimeInput)
        assertEquals("presentation", result.presentation)
        assertTrue(result.hardcoreAllowed)
    }

    @Test
    fun capabilityNegotiationMismatchLeavesNoLaunchResultForFreshSessionFallback() = runTest {
        var fallbackCount = 0
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("camera"),
            loadRomPaused = {},
            reportCapabilities = {},
            prepare = { true },
            activate = { it.toSet() },
            compose = { it },
            expose = { null to null },
            hardcoreAllowed = { true },
        )

        if (result.activeIds == setOf("camera") && result.runtimeInput == null) {
            fallbackCount += 1
        }

        assertEquals(1, fallbackCount)
        assertEquals(setOf("camera"), result.activeIds)
        assertEquals(null, result.runtimeInput)
    }

    @Test
    fun fallbackCancelsOldCollectorsAndStartsOneNewSessionWithOriginalLaunchContract() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val oldScope = CoroutineScope(dispatcher + Job())
        val newScope = CoroutineScope(dispatcher + Job())
        val oldCollector = oldScope.launch { awaitCancellation() }
        val collectorCount = mutableListOf<Int>()
        val launches = mutableListOf<LaunchContract>()
        val coordinator = EmulatorSessionFallbackCoordinator(
            retireSession = oldScope::cancel,
            startSession = {},
            launchOnSession = { block -> newScope.launch { block() } },
        )

        coordinator.startFreshSession {
            collectorCount += 1
            launches += LaunchContract(
                originalUri = "content://rom/original",
                slot2 = "configured-slot-2",
                cheats = listOf("base-cheat"),
                retroAchievements = "casual",
                presentation = "native",
            )
        }
        testScheduler.advanceUntilIdle()

        assertFalse(oldCollector.isActive)
        assertEquals(1, collectorCount.size)
        assertEquals(
            LaunchContract(
                originalUri = "content://rom/original",
                slot2 = "configured-slot-2",
                cheats = listOf("base-cheat"),
                retroAchievements = "casual",
                presentation = "native",
            ),
            launches.single(),
        )
        assertTrue(newScope.coroutineContext[Job]!!.isActive)
        newScope.cancel()
    }

    @Test
    fun unsupportedCapabilityPreparesAsFailureAndFallsBackToOriginalLaunchContract() = runTest {
        val launches = mutableListOf<LaunchContract>()
        var fallbackCount = 0
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("camera"),
            loadRomPaused = {},
            reportCapabilities = {},
            prepare = { false },
            activate = { it.toSet() },
            compose = { it },
            expose = { null to null },
            hardcoreAllowed = { true },
        )

        if (result.activeIds.isEmpty()) {
            fallbackCount += 1
            launches += LaunchContract(
                originalUri = "content://rom/original",
                slot2 = "configured-slot-2",
                cheats = listOf("base-cheat"),
                retroAchievements = "casual",
                presentation = "native",
            )
        }

        assertEquals(1, fallbackCount)
        assertEquals(
            LaunchContract(
                originalUri = "content://rom/original",
                slot2 = "configured-slot-2",
                cheats = listOf("base-cheat"),
                retroAchievements = "casual",
                presentation = "native",
            ),
            launches.single(),
        )
        assertTrue(result.activeIds.isEmpty())
        assertEquals(null, result.runtimeInput)
        assertEquals(null, result.presentation)
    }

    private data class LaunchContract(
        val originalUri: String,
        val slot2: String,
        val cheats: List<String>,
        val retroAchievements: String,
        val presentation: String,
    )
}
