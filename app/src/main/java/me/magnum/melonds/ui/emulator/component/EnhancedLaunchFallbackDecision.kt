package me.magnum.melonds.ui.emulator.component

internal data class EnhancedLaunchFallbackDecision(
    val enhancementOverride: Set<String>,
    val allowEnhancedFallback: Boolean,
)

internal fun decideEnhancedLaunchFallback(
    failure: Throwable,
    allowEnhancedFallback: Boolean,
): EnhancedLaunchFallbackDecision {
    if (!allowEnhancedFallback) throw failure
    return EnhancedLaunchFallbackDecision(
        enhancementOverride = emptySet(),
        allowEnhancedFallback = false,
    )
}
