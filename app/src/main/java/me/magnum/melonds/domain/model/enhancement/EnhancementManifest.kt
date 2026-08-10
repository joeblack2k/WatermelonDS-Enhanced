package me.magnum.melonds.domain.model.enhancement

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EnhancementManifest(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val version: String,
    val author: String = "",
    val match: EnhancementMatch,
    val capabilities: Set<EnhancementCapability> = emptySet(),
    val patches: List<EnhancementPatch> = emptyList(),
    val hardcoreCompatible: Boolean = false,
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
}

@Serializable
data class EnhancementPatch(
    val type: EnhancementPatchType,
    val file: String,
    val apply: EnhancementPatchApply = EnhancementPatchApply.RUNTIME,
)

@Serializable
enum class EnhancementPatchType {
    ACTION_REPLAY,
    IPS,
    BPS,
    RUNTIME_OVERLAY,
)

@Serializable
enum class EnhancementPatchApply {
    RUNTIME,
    TEMPORARY_COPY,
)

object EnhancementManifestParser {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(serialized: String): EnhancementManifest {
        val manifest = json.decodeFromString<EnhancementManifest>(serialized)
        validate(manifest)
        return manifest
    }

    fun validate(manifest: EnhancementManifest) {
        require(manifest.schemaVersion == 1) { "Unsupported enhancement schema" }
        require(manifest.id.matches(Regex("[a-z0-9][a-z0-9._-]*"))) { "Invalid enhancement id" }
        require(manifest.name.isNotBlank() && manifest.version.isNotBlank()) { "Missing enhancement metadata" }
        require(manifest.match.gameCode.length == 4) { "Game code must contain four characters" }
        require(manifest.match.sha256.isNotEmpty()) { "Enhancement must declare an exact ROM SHA-256" }
        require(manifest.match.sha256.all { it.matches(Regex("[0-9a-fA-F]{64}")) }) { "Invalid ROM SHA-256" }
        require(manifest.patches.map { it.file }.distinct().size == manifest.patches.size) {
            "Duplicate enhancement patch file"
        }
        manifest.patches.forEach {
            require(!it.file.startsWith("/") && ".." !in it.file.split('/')) {
                "Patch file must stay inside the enhancement package"
            }
        }
        if (manifest.capabilities.contains(EnhancementCapability.RUNTIME_INPUT_PROTOCOL)) {
            require(EnhancementCapability.CONTROLLER_AXIS_OWNER in manifest.capabilities) {
                "Runtime input protocols must own their controller axes"
            }
        }
    }
}

data class EnhancementRomIdentity(
    val gameCode: String,
    val headerChecksum: String?,
    val sha256: String,
)

fun EnhancementManifest.matches(identity: EnhancementRomIdentity): Boolean {
    return match.gameCode == identity.gameCode &&
        (match.headerChecksum == null || match.headerChecksum.equals(identity.headerChecksum, ignoreCase = true)) &&
        identity.sha256.lowercase() in match.sha256.map(String::lowercase)
}
