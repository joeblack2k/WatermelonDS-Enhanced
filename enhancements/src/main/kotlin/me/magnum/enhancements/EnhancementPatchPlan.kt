package me.magnum.enhancements

data class EnhancementPatchPlan(
    val temporaryCopyPatches: List<EnhancementPatch>,
    val runtimePatches: List<EnhancementPatch>,
) {
    fun applyTemporaryCopy(source: ByteArray, files: Map<String, ByteArray>): ByteArray {
        return temporaryCopyPatches.fold(source) { current, patch ->
            val payload = files[patch.file] ?: error("Missing enhancement patch file: ${patch.file}")
            when (patch.type) {
                EnhancementPatchType.IPS -> EnhancementPatchApplier.applyIps(current, payload)
                EnhancementPatchType.BPS -> EnhancementPatchApplier.applyBps(current, payload)
                else -> error("Patch type ${patch.type} cannot modify a temporary ROM copy")
            }
        }
    }
}

fun EnhancementSession.createPatchPlan(): EnhancementPatchPlan {
    val patches = addOns.flatMap { it.patches }
    return EnhancementPatchPlan(
        temporaryCopyPatches = patches.filter { it.apply == EnhancementPatchApply.TEMPORARY_COPY },
        runtimePatches = patches.filter { it.apply == EnhancementPatchApply.RUNTIME },
    )
}
