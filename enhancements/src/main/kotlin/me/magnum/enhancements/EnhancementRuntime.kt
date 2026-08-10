package me.magnum.enhancements

data class EnhancementSession(
    val addOns: List<EnhancementManifest>,
) {
    private val runtimeAddOns = addOns.filter { it.status != EnhancementStatus.SOURCE_ONLY }
    val capabilities: Set<EnhancementCapability> = runtimeAddOns.flatMap { it.capabilities }.toSet()
    val hardcoreCompatible: Boolean = runtimeAddOns.all { it.hardcoreCompatible }
    val runtimeInput: EnhancementRuntimeInput? = runtimeAddOns
        .mapNotNull { addOn ->
            addOn.runtimeProtocol?.let { protocol ->
                require(EnhancementCapability.RUNTIME_INPUT_PROTOCOL in addOn.capabilities) {
                    "Runtime input protocol requires its capability"
                }
                EnhancementRuntimeInput(
                    protocol = protocol,
                    axisXCode = requireNotNull(addOn.runtimeAxisXCode),
                    axisYCode = requireNotNull(addOn.runtimeAxisYCode),
                    invertX = addOn.runtimeInvertX,
                    invertY = addOn.runtimeInvertY,
                    deadzone = addOn.runtimeDeadzone,
                    sensitivity = addOn.runtimeSensitivity,
                )
            }
        }
        .singleOrNull()
    val patchResources = runtimeAddOns.flatMap { addOn ->
        addOn.patches.map { EnhancementPatchResource(addOn.id, it) }
    }
    val patchPlan = EnhancementPatchPlan(
        temporaryCopyPatches = patchResources.filter { it.patch.apply == EnhancementPatchApply.TEMPORARY_COPY },
        runtimePatches = patchResources.filter { it.patch.apply == EnhancementPatchApply.RUNTIME },
    )
    val runtimeGuards: List<EnhancementRuntimeGuard> = runtimeAddOns.flatMap { addOn ->
        addOn.patches
            .filter { it.apply == EnhancementPatchApply.RUNTIME }
            .flatMap { patch ->
                patch.expectedOriginalWords.map { (address, word) ->
                    EnhancementRuntimeGuard(
                        addOnId = addOn.id,
                        address = address.removePrefix("0x").toLong(16),
                        expectedWord = word.removePrefix("0x").toLong(16),
                    )
                }
            }
    }

    fun hasCapability(capability: EnhancementCapability): Boolean {
        return capability in capabilities
    }

    fun presentationState(
        verifiedGamePatch: Boolean,
        availableNativeCapabilities: Set<EnhancementCapability>,
    ): EnhancementPresentationState {
        val requested = if (hasCapability(EnhancementCapability.LAYER_AWARE_PRESENTATION)) {
            EnhancementPresentationMode.LAYER_AWARE_PRESENTATION
        } else {
            EnhancementPresentationMode.NATIVE_4_3
        }
        val effective = if (
            requested == EnhancementPresentationMode.LAYER_AWARE_PRESENTATION &&
            verifiedGamePatch &&
            EnhancementCapability.NATIVE_EMULATOR_CAPABILITY in availableNativeCapabilities
        ) {
            requested
        } else {
            EnhancementPresentationMode.NATIVE_4_3
        }
        return EnhancementPresentationState(requested = requested, effective = effective)
    }

    fun close() = Unit
}

enum class EnhancementPresentationMode {
    NATIVE_4_3,
    LAYER_AWARE_PRESENTATION,
}

data class EnhancementPresentationState(
    val requested: EnhancementPresentationMode,
    val effective: EnhancementPresentationMode,
)

fun EnhancementPresentationState.requiresNative43Fallback(): Boolean {
    return requested == EnhancementPresentationMode.LAYER_AWARE_PRESENTATION &&
        effective != EnhancementPresentationMode.LAYER_AWARE_PRESENTATION
}

fun EnhancementCatalog.createSession(
    identity: EnhancementRomIdentity,
    enabledIds: Set<String>,
): EnhancementSession {
    val matching = matching(identity)
    require(enabledIds.all { id -> matching.any { it.id == id } }) {
        "Enabled enhancement does not match the ROM identity"
    }
    val selected = matching.filter { it.id in enabledIds }
    require(selected.none { addOn ->
        addOn.conflictsWith.any { conflictingId -> selected.any { it.id == conflictingId } }
    }) {
        "Enabled enhancements conflict"
    }
    require(selected.flatMap { it.requiresCapabilities }.all { it in selected.flatMap { addOn -> addOn.capabilities } }) {
        "Enabled enhancements have unsatisfied capability requirements"
    }
    require(selected.count { EnhancementCapability.CONTROLLER_AXIS_OWNER in it.capabilities } <= 1) {
        "Enabled enhancements cannot declare more than one controller axis owner"
    }
    require(selected.none {
        it.runtimeProtocol != null &&
            EnhancementCapability.RUNTIME_INPUT_PROTOCOL !in it.capabilities
    }) {
        "Runtime input protocol requires its capability"
    }
    val runtimeGuardAddresses = selected.flatMap { addOn ->
        addOn.patches
            .filter { it.apply == EnhancementPatchApply.RUNTIME }
            .flatMap { it.expectedOriginalWords.keys.map(::parseRuntimeAddress) }
    }
    require(runtimeGuardAddresses.size == runtimeGuardAddresses.toSet().size) {
        "Enabled enhancements cannot guard the same runtime address more than once"
    }
    return EnhancementSession(selected)
}

private fun parseRuntimeAddress(address: String): Long {
    return address.removePrefix("0x").removePrefix("0X").toLong(16)
}

interface EnhancementRuntime {
    fun prepare(session: EnhancementSession)
    fun onControllerInput(x: Float, y: Float)
    fun onReset()
    fun close()
}
