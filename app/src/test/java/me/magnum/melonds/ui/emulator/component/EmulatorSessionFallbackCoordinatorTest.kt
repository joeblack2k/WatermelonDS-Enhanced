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

    private data class LaunchContract(
        val originalUri: String,
        val slot2: String,
        val cheats: List<String>,
        val retroAchievements: String,
        val presentation: String,
    )
}
