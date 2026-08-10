package me.magnum.melonds.impl

import me.magnum.enhancements.ActionReplayParser
import me.magnum.enhancements.EnhancementPatchType
import me.magnum.enhancements.EnhancementPatchApply
import me.magnum.enhancements.EnhancementSession
import me.magnum.melonds.domain.model.Cheat
import javax.inject.Inject

class EnhancedCheatLoader @Inject constructor(
    private val catalogLoader: EnhancementCatalogLoader,
) {
    fun load(session: EnhancementSession): List<Cheat> {
        return session.addOns.filter { it.status != me.magnum.enhancements.EnhancementStatus.SOURCE_ONLY }.flatMap { addOn ->
            addOn.patches
                .filter {
                    it.type == EnhancementPatchType.ACTION_REPLAY &&
                        it.apply == EnhancementPatchApply.RUNTIME
                }
                .flatMap { patch ->
                    val code = catalogLoader.readFiles(addOn, setOf(patch.file))[patch.file]
                        ?.decodeToString()
                        ?: error("Missing Action Replay patch: ${patch.file}")
                    listOf(
                        Cheat(
                            id = null,
                            cheatDatabaseId = 0,
                            name = "${addOn.name} ${addOn.version}",
                            description = patch.provenance,
                            code = ActionReplayParser.parse(code),
                            enabled = true,
                        ),
                    )
                }
        }
    }
}
