package me.magnum.enhancements

data class EnhancementSession(
    val addOns: List<EnhancementManifest>,
) {
    val capabilities: Set<EnhancementCapability> = addOns.flatMap { it.capabilities }.toSet()
    val hardcoreCompatible: Boolean = addOns.all { it.hardcoreCompatible }
    val runtimeInput: EnhancementRuntimeInput? = addOns
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
    val patchPlan: EnhancementPatchPlan = createPatchPlan()
    val runtimeGuards: List<EnhancementRuntimeGuard> = addOns.flatMap { addOn ->
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

    fun close() = Unit
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
