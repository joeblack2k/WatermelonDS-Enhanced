package me.magnum.enhancements

data class EnhancementPatchPlan(
    val temporaryCopyPatches: List<EnhancementPatchResource>,
    val runtimePatches: List<EnhancementPatchResource>,
) {
    fun applyTemporaryCopy(source: ByteArray, files: Map<String, ByteArray>): ByteArray {
        return temporaryCopyPatches.fold(source) { current, patch ->
            val payload = files[patch.key] ?: error("Missing enhancement patch file: ${patch.key}")
            when (patch.patch.type) {
                EnhancementPatchType.IPS -> EnhancementPatchApplier.applyIps(current, payload)
                EnhancementPatchType.BPS -> EnhancementPatchApplier.applyBps(current, payload)
                else -> error("Patch type ${patch.patch.type} cannot modify a temporary ROM copy")
            }
        }
    }
}

data class EnhancementPatchResource(
    val addOnId: String,
    val patch: EnhancementPatch,
) {
    val key: String = "$addOnId/${patch.file}"
}

fun EnhancementSession.createPatchPlan(): EnhancementPatchPlan {
    val patches = addOns.flatMap { addOn ->
        addOn.patches.map { EnhancementPatchResource(addOn.id, it) }
    }
    return EnhancementPatchPlan(
        temporaryCopyPatches = patches.filter { it.patch.apply == EnhancementPatchApply.TEMPORARY_COPY },
        runtimePatches = patches.filter { it.patch.apply == EnhancementPatchApply.RUNTIME },
    )
}
