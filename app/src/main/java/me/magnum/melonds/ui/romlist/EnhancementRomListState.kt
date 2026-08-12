package me.magnum.melonds.ui.romlist

import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementRomIdentity
import me.magnum.enhancements.createSession

data class EnhancementRomAvailability(
    val availableIds: Set<String>,
    val enhancedLaunchIds: Set<String>?,
)

internal fun resolveEnhancementAvailability(
    catalog: EnhancementCatalog,
    identity: EnhancementRomIdentity?,
): EnhancementRomAvailability {
    val availableIds = identity?.let {
        catalog.installableMatching(it).mapTo(linkedSetOf()) { manifest -> manifest.id }
    }.orEmpty()
    val enhancedLaunchIds = if (availableIds.isEmpty() || identity == null) {
        null
    } else {
        runCatching { catalog.createSession(identity, availableIds) }
            .getOrNull()
            ?.let { availableIds }
    }
    return EnhancementRomAvailability(availableIds, enhancedLaunchIds)
}
