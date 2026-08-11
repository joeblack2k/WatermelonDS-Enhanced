package me.magnum.melonds.ui.emulator.component

import kotlinx.coroutines.Job

/**
 * Keeps enhanced-launch fallback at one explicit session boundary.
 */
internal class EmulatorSessionFallbackCoordinator(
    private val retireSession: () -> Unit,
    private val startSession: () -> Unit,
    private val launchOnSession: (suspend () -> Unit) -> Job,
) {
    fun resetSession() {
        retireSession()
        startSession()
    }

    fun startFreshSession(block: suspend () -> Unit) {
        resetSession()
        launchOnSession(block)
    }

    fun launchOnFreshSession(block: suspend () -> Unit) {
        launchOnSession(block)
    }
}
