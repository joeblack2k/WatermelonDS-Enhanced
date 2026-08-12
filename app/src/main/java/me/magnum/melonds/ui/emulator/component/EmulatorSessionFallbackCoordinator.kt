package me.magnum.melonds.ui.emulator.component

import kotlinx.coroutines.Job
import me.magnum.enhancements.RuntimeCapability

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

internal data class EnhancedOrchestrationResult<T>(
    val activeIds: Set<String>,
    val composed: T,
    val runtimeInput: Any?,
    val presentation: Any?,
    val hardcoreAllowed: Boolean,
    val capabilities: Set<RuntimeCapability>,
)

/**
 * Pure ordering seam for the enhanced launch contract.
 */
internal suspend fun <T> orchestrateEnhancedLaunch(
    addOnIds: List<String>,
    loadRomPaused: suspend () -> Unit,
    reportCapabilities: () -> Set<RuntimeCapability>,
    prepare: (String, Set<RuntimeCapability>) -> Boolean,
    activate: (List<String>) -> Set<String>,
    compose: (Set<String>) -> T,
    expose: (Set<String>, Set<RuntimeCapability>) -> Pair<Any?, Any?>,
    hardcoreAllowed: (Set<String>) -> Boolean,
): EnhancedOrchestrationResult<T> {
    require(addOnIds.isNotEmpty()) { "Enhanced orchestration requires at least one add-on" }
    loadRomPaused()
    val capabilities = reportCapabilities()
    val prepared = addOnIds.filter { prepare(it, capabilities) }
    if (prepared.size != addOnIds.size) {
        return EnhancedOrchestrationResult(
            activeIds = emptySet(),
            composed = compose(emptySet()),
            runtimeInput = null,
            presentation = null,
            hardcoreAllowed = false,
            capabilities = capabilities,
        )
    }
    val active = activate(prepared)
    if (active != addOnIds.toSet()) {
        return EnhancedOrchestrationResult(
            activeIds = emptySet(),
            composed = compose(emptySet()),
            runtimeInput = null,
            presentation = null,
            hardcoreAllowed = false,
            capabilities = capabilities,
        )
    }
    val composed = compose(active)
    val (runtimeInput, presentation) = expose(active, capabilities)
    return EnhancedOrchestrationResult(
        activeIds = active,
        composed = composed,
        runtimeInput = runtimeInput,
        presentation = presentation,
        hardcoreAllowed = hardcoreAllowed(active),
        capabilities = capabilities,
    )
}
