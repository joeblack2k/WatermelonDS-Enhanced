package me.magnum.melonds.impl

import me.magnum.enhancements.EnhancementOverlayParser
import me.magnum.enhancements.EnhancementOverlayWord
import me.magnum.enhancements.EnhancementPatchType
import me.magnum.enhancements.EnhancementSession
import javax.inject.Inject

class EnhancedOverlayLoader @Inject constructor(
    private val catalogLoader: EnhancementCatalogLoader,
) {
    fun load(session: EnhancementSession): List<EnhancementOverlayWord> {
        return session.addOns.filter { it.status != me.magnum.enhancements.EnhancementStatus.SOURCE_ONLY }.flatMap { addOn ->
            addOn.patches
                .filter { it.type == EnhancementPatchType.RUNTIME_OVERLAY }
                .flatMap { patch ->
                    val text = catalogLoader.readFiles(addOn, setOf(patch.file))[patch.file]
                        ?.decodeToString()
                        ?: error("Missing runtime overlay: ${patch.file}")
                    EnhancementOverlayParser.parse(text, patch.expectedOriginalWords)
                }
        }
    }
}
