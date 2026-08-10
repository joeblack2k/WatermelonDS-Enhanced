package me.magnum.melonds.domain.model.enhancement

data class EnhancementSession(
    val addOns: List<EnhancementManifest>,
) {
    val capabilities: Set<EnhancementCapability> = addOns.flatMap { it.capabilities }.toSet()
    val hardcoreCompatible: Boolean = addOns.all { it.hardcoreCompatible }

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
    return EnhancementSession(matching.filter { it.id in enabledIds })
}

interface EnhancementRuntime {
    fun prepare(session: EnhancementSession)
    fun onControllerInput(x: Float, y: Float)
    fun onReset()
    fun close()
}
