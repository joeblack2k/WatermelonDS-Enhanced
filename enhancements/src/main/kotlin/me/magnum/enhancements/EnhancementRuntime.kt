package me.magnum.enhancements

data class EnhancementSession(
    val addOns: List<EnhancementManifest>,
) {
    val capabilities: Set<EnhancementCapability> = addOns.flatMap { it.capabilities }.toSet()
    val hardcoreCompatible: Boolean = addOns.all { it.hardcoreCompatible }
    val runtimeInput: EnhancementRuntimeInput? = addOns
        .mapNotNull { addOn ->
            addOn.runtimeProtocol?.let { protocol ->
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
    require(selected.mapNotNull { it.runtimeProtocol }.distinct().size <= 1) {
        "Enabled enhancements declare incompatible runtime protocols"
    }
    return EnhancementSession(selected)
}

interface EnhancementRuntime {
    fun prepare(session: EnhancementSession)
    fun onControllerInput(x: Float, y: Float)
    fun onReset()
    fun close()
}
