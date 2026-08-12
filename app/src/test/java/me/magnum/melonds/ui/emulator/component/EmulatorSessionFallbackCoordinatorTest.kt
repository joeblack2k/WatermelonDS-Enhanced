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
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class EmulatorSessionFallbackCoordinatorTest {
    @Test
    fun emptyEnhancedLaunchIsRejectedBeforeAnyActivationWork() = runTest {
        val events = mutableListOf<String>()

        try {
            orchestrateEnhancedLaunch(
                addOnIds = emptyList(),
                loadRomPaused = { events += "load" },
                reportCapabilities = { events += "capabilities"; emptySet() },
                prepare = { _, _ -> events += "prepare"; true },
                activate = { events += "activate"; emptySet() },
                compose = { events += "compose"; it },
                expose = { _, _ -> events += "expose"; null to null },
                hardcoreAllowed = { events += "hardcore"; true },
            )
            fail("empty enhanced launch must be rejected")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("at least one add-on"))
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun reconciledEmptyPathSkipsEnhancementCatalogAndSessionWork() {
        val source = File("src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt").readText()
        val launch = source.substringAfter("private suspend fun launchRom(")
            .substringBefore("private fun isRetroAchievementsEnabledForLaunch")

        assertTrue(launch.contains("if (enabledEnhancements.isEmpty())"))
        assertTrue(launch.contains("if (reconciled.isEmpty())"))
        assertTrue(launch.contains("catalog.createSession"))
        assertTrue(launch.indexOf("if (reconciled.isEmpty())") < launch.indexOf("catalog.createSession"))
        assertTrue(launch.indexOf("if (enabledEnhancements.isEmpty())") < launch.indexOf("enhancementCatalogLoader.load()"))
    }

    @Test
    fun preflightFailureFallsBackBeforeMaterializationOrActivationAndDoesNotPersistEarly() {
        val source = File("src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt").readText()
        val launch = source.substringAfter("private suspend fun launchRom(")
            .substringBefore("private fun isRetroAchievementsEnabledForLaunch")
        val preflightStart = launch.indexOf("activeEnhancementSession = try")
        val preflight = launch.substring(preflightStart).substringBefore("reconciledConfig?.let")

        assertTrue(preflight.contains("enhancementCatalogLoader.load()"))
        assertTrue(preflight.contains("enhancementRomIdentityResolver.resolve(rom, catalog)"))
        assertTrue(preflight.contains("catalog.createSession"))
        assertTrue(preflight.contains("decideEnhancedLaunchFallback(exception, allowEnhancedFallback)"))
        assertTrue(preflight.contains("enhancementOverride = fallback.enhancementOverride"))
        assertTrue(preflight.contains("allowEnhancedFallback = fallback.allowEnhancedFallback"))
        assertFalse(preflight.contains("romsRepository.updateRomConfig"))
        assertTrue(preflight.contains("catch (exception: Exception)"))
        assertTrue(preflight.contains("if (exception is CancellationException)"))
        assertFalse(preflight.contains("launchRom(rom, enhancementOverride = emptySet()"))
        assertFalse(preflight.contains("activateEnhancedAddOns"))
        assertFalse(preflight.contains("enhancedOverlayLoader.load"))
    }

    @Test
    fun enhancedLaunchOrdersLoadCapabilityActivationCompositionExposureAndPolicy() = runTest {
        val events = mutableListOf<String>()
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("accepted", "rejected"),
            loadRomPaused = { events += "load:paused" },
            reportCapabilities = { events += "capabilities:actual"; emptySet() },
            prepare = { id, _ ->
                events += "prepare:$id"
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
            expose = { ids, _ ->
                events += "expose:$ids"
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
    fun incompatibleActiveAddOnBlocksHardcoreBeforePolicyIsPublished() = runTest {
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("incompatible"),
            loadRomPaused = {},
            reportCapabilities = { emptySet() },
            prepare = { _, _ -> true },
            activate = { it.toSet() },
            compose = { it },
            expose = { _, _ -> null to null },
            hardcoreAllowed = { false },
        )

        assertEquals(setOf("incompatible"), result.activeIds)
        assertFalse(result.hardcoreAllowed)
        assertEquals(emptySet<String>(), result.capabilities)
    }

    @Test
    fun compatibleActiveAddOnPublishesPolicyToAllSessionGates() = runTest {
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("compatible"),
            loadRomPaused = {},
            reportCapabilities = { emptySet() },
            prepare = { _, _ -> true },
            activate = { it.toSet() },
            compose = { it },
            expose = { _, _ -> null to null },
            hardcoreAllowed = { true },
        )

        assertEquals(emptySet<String>(), result.capabilities)
    }

    @Test
    fun capabilityNegotiationMismatchLeavesNoLaunchResultForFreshSessionFallback() = runTest {
        var fallbackCount = 0
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("camera"),
            loadRomPaused = {},
            reportCapabilities = { emptySet() },
            prepare = { _, _ -> true },
            activate = { it.toSet() },
            compose = { it },
            expose = { _, _ -> null to null },
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
    fun catalogSessionFailureFallsBackToOriginalOnceAndSecondFailureIsTerminal() = runTest {
        val catalogSessionFailure = IllegalStateException("catalog/session failure")
        val launches = mutableListOf<LaunchContract>()
        var allowEnhancedFallback = true

        repeat(2) {
            try {
                val decision = decideEnhancedLaunchFallback(
                    catalogSessionFailure,
                    allowEnhancedFallback,
                )
                assertEquals(emptySet<String>(), decision.enhancementOverride)
                assertFalse(decision.allowEnhancedFallback)
                launches += originalLaunchContract()
                allowEnhancedFallback = decision.allowEnhancedFallback
            } catch (failure: IllegalStateException) {
                assertEquals(catalogSessionFailure, failure)
            }
        }

        assertEquals(1, launches.size)
    }

    @Test
    fun unsupportedCapabilityPreparesAsFailureAndFallsBackToOriginalLaunchContract() = runTest {
        val launches = mutableListOf<LaunchContract>()
        var fallbackCount = 0
        val result = orchestrateEnhancedLaunch(
            addOnIds = listOf("camera"),
            loadRomPaused = {},
            reportCapabilities = { emptySet() },
            prepare = { _, _ -> false },
            activate = { it.toSet() },
            compose = { it },
            expose = { _, _ -> null to null },
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

    private fun originalLaunchContract() = LaunchContract(
        originalUri = "content://rom/original",
        slot2 = "configured-slot-2",
        cheats = listOf("base-cheat"),
        retroAchievements = "casual",
        presentation = "native",
    )
}
