package me.magnum.enhancements

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EnhancementManifest(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val version: String,
    val author: String = "",
    // null is the legacy installable-manifest format.
    val status: EnhancementStatus? = null,
    val match: EnhancementMatch,
    val capabilities: Set<EnhancementCapability> = emptySet(),
    val patches: List<EnhancementPatch> = emptyList(),
    val runtimeProtocol: String? = null,
    val runtimeAxisXCode: Int? = null,
    val runtimeAxisYCode: Int? = null,
    val runtimeInvertX: Boolean = false,
    val runtimeInvertY: Boolean = false,
    val runtimeDeadzone: Float = 0.12f,
    val runtimeSensitivity: Float = 1f,
    val requiresCapabilities: Set<EnhancementCapability> = emptySet(),
    val conflictsWith: Set<String> = emptySet(),
    val hardcoreCompatible: Boolean = false,
    val verification: EnhancementVerification = EnhancementVerification(),
)

@Serializable
enum class EnhancementStatus {
    SOURCE_ONLY,
    VERIFIED,
}

@Serializable
enum class EnhancementClaim {
    VERIFIED,
    UNVERIFIED,
}

@Serializable
enum class EnhancementPayloadInput {
    VERIFIED,
    MISSING,
}

@Serializable
data class EnhancementVerification(
    val cadence: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val gameplayPhysics: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val timers: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val animation: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val particles: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val audio: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val saveState: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val guardedPayload: EnhancementClaim = EnhancementClaim.UNVERIFIED,
    val payloadInput: EnhancementPayloadInput = EnhancementPayloadInput.MISSING,
)

@Serializable
data class EnhancementMatch(
    val gameCode: String,
    val headerChecksum: String? = null,
    val sha256: Set<String> = emptySet(),
)

@Serializable
enum class EnhancementCapability {
    CONTROLLER_AXIS_OWNER,
    RUNTIME_INPUT_PROTOCOL,
    RUNTIME_CODE_PATCH,
    NATIVE_EMULATOR_CAPABILITY,
    SLOT2_ANALOG,
    LAYER_AWARE_PRESENTATION,
}

@Serializable
data class EnhancementPatch(
    val type: EnhancementPatchType,
    val file: String,
    val apply: EnhancementPatchApply = EnhancementPatchApply.RUNTIME,
    val provenance: String = "",
    val expectedOriginalWords: Map<String, String> = emptyMap(),
    val sha256: String? = null,
)

@Serializable
enum class EnhancementPatchType {
    ACTION_REPLAY,
    IPS,
    BPS,
    RUNTIME_OVERLAY,
}

@Serializable
enum class EnhancementPatchApply {
    RUNTIME,
    TEMPORARY_COPY,
}

object EnhancementManifestParser {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(serialized: String): EnhancementManifest {
        val manifest = json.decodeFromString<EnhancementManifest>(serialized).let {
            // Legacy manifests had no status field and were installable.
            it
        }
        validate(manifest)
        return manifest
    }

    fun validate(manifest: EnhancementManifest) {
        require(manifest.schemaVersion == 1) { "Unsupported enhancement schema" }
        require(manifest.id.matches(Regex("[a-z0-9][a-z0-9._-]*"))) { "Invalid enhancement id" }
        require(manifest.name.isNotBlank() && manifest.version.isNotBlank()) { "Missing enhancement metadata" }
        if (manifest.status == EnhancementStatus.VERIFIED) {
            require(manifest.verification.payloadInput == EnhancementPayloadInput.VERIFIED) {
                "Verified enhancements need revision-specific payload input"
            }
            require(manifest.verification.guardedPayload == EnhancementClaim.VERIFIED) {
                "Verified enhancements need guarded payload evidence"
            }
            require(
                listOf(
                    manifest.verification.cadence,
                    manifest.verification.gameplayPhysics,
                    manifest.verification.timers,
                    manifest.verification.animation,
                    manifest.verification.particles,
                    manifest.verification.audio,
                    manifest.verification.saveState,
                ).all { it == EnhancementClaim.VERIFIED },
            ) { "Verified enhancements need every timing contract claim" }
        }
        require(manifest.match.gameCode.matches(Regex("[A-Z0-9]{4}"))) {
            "Game code must contain four uppercase ASCII letters or digits"
        }
        require(manifest.match.headerChecksum != null || manifest.match.sha256.isNotEmpty()) {
            "Enhancement must declare a ROM header checksum or SHA-256"
        }
        manifest.match.headerChecksum?.let {
            require(it.matches(Regex("[0-9A-Fa-f]{8}"))) { "Invalid ROM header checksum" }
        }
        require(manifest.match.sha256.all { it.matches(Regex("[0-9a-fA-F]{64}")) }) { "Invalid ROM SHA-256" }
        require(manifest.patches.map { it.file }.distinct().size == manifest.patches.size) {
            "Duplicate enhancement patch file"
        }
        manifest.patches.forEach {
            require(it.sha256 == null || it.sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
                "Invalid patch SHA-256"
            }
            require(!it.file.startsWith("/") && ".." !in it.file.split('/')) {
                "Patch file must stay inside the enhancement package"
            }
            when (it.type) {
                EnhancementPatchType.ACTION_REPLAY,
                EnhancementPatchType.RUNTIME_OVERLAY -> require(it.apply == EnhancementPatchApply.RUNTIME) {
                    "${it.type} patches must use RUNTIME apply mode"
                }
                EnhancementPatchType.IPS,
                EnhancementPatchType.BPS -> require(it.apply == EnhancementPatchApply.TEMPORARY_COPY) {
                    "${it.type} patches must use TEMPORARY_COPY apply mode"
                }
            }
            if (it.type == EnhancementPatchType.RUNTIME_OVERLAY || it.type == EnhancementPatchType.ACTION_REPLAY) {
                require(it.provenance.isNotBlank()) { "Runtime patches need provenance" }
                require(it.expectedOriginalWords.keys.all { address -> address.matches(Regex("0x[0-9a-fA-F]{8}")) }) {
                    "Invalid guarded patch address"
                }
                require(it.expectedOriginalWords.values.all { word -> word.matches(Regex("0x[0-9a-fA-F]{8}")) }) {
                    "Invalid guarded patch word"
                }
            }
        }
        if (manifest.capabilities.contains(EnhancementCapability.RUNTIME_INPUT_PROTOCOL)) {
            require(EnhancementCapability.CONTROLLER_AXIS_OWNER in manifest.capabilities) {
                "Runtime input protocols must own their controller axes"
            }
            require(manifest.runtimeProtocol != null) { "Runtime input capability needs a protocol" }
            require(manifest.runtimeAxisXCode != null && manifest.runtimeAxisYCode != null) {
                "Runtime input protocols must declare both axis codes"
            }
            require(manifest.runtimeDeadzone in 0f..1f) { "Invalid runtime deadzone" }
            require(manifest.runtimeSensitivity > 0f) { "Invalid runtime sensitivity" }
        }
        require(manifest.conflictsWith.none { it == manifest.id }) {
            "Enhancement cannot conflict with itself"
        }
    }
}

data class EnhancementRuntimeInput(
    val protocol: String,
    val axisXCode: Int,
    val axisYCode: Int,
    val invertX: Boolean,
    val invertY: Boolean,
    val deadzone: Float,
    val sensitivity: Float,
)

data class EnhancementRuntimeGuard(
    val addOnId: String,
    val address: Long,
    val expectedWord: Long,
)

data class EnhancementRomIdentity(
    val gameCode: String,
    val headerChecksum: String?,
    val sha256: String,
)

fun EnhancementManifest.matches(identity: EnhancementRomIdentity): Boolean {
    return match.gameCode == identity.gameCode &&
        (match.headerChecksum == null || match.headerChecksum.equals(identity.headerChecksum, ignoreCase = true)) &&
        (match.sha256.isEmpty() || identity.sha256.lowercase() in match.sha256.map(String::lowercase))
}
