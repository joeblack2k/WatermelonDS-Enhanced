package me.magnum.enhancements

import java.io.File
import java.security.MessageDigest

class EnhancementCatalog(manifests: List<EnhancementManifest>) {
    val manifests: List<EnhancementManifest> = manifests.sortedBy { it.id }

    init {
        require(this.manifests.map { it.id }.distinct().size == this.manifests.size) {
            "Duplicate enhancement id"
        }
        this.manifests.forEach(EnhancementManifestParser::validate)
    }

    fun matching(identity: EnhancementRomIdentity): List<EnhancementManifest> {
        return manifests.filter { it.matches(identity) }
    }

    fun installableMatching(identity: EnhancementRomIdentity): List<EnhancementManifest> {
        return matching(identity).filter(EnhancementManifest::isInstallable)
    }

    fun hasShaGuard(gameCode: String, headerChecksum: String?): Boolean {
        return manifests.any {
            it.match.gameCode == gameCode &&
                (it.match.headerChecksum == null ||
                    it.match.headerChecksum.equals(headerChecksum, ignoreCase = true)) &&
                it.match.sha256.isNotEmpty()
        }
    }

    fun find(id: String): EnhancementManifest? = manifests.firstOrNull { it.id == id }

    companion object {
        fun loadFromRoots(roots: Iterable<File>): EnhancementCatalog {
            val manifests = roots.asSequence()
                .filter(File::isDirectory)
                .flatMap { root ->
                    root.listFiles()
                        .orEmpty()
                        .asSequence()
                        .filter { it.isDirectory && !it.name.startsWith(".") }
                        .filter { File(it, "manifest.json").isFile }
                        .map { packageRoot ->
                            val manifestFile = File(packageRoot, "manifest.json")
                            val manifest = EnhancementManifestParser.parse(manifestFile.readText())
                            require(packageRoot.name == manifest.id) {
                                "Enhancement directory does not match manifest id: ${packageRoot.name}"
                            }
                            manifest.patches.forEach { patch ->
                                patch.sha256?.let { expected ->
                                    val file = File(packageRoot, patch.file)
                                    require(file.isFile) { "Missing enhancement patch file: ${patch.file}" }
                                    val actual = MessageDigest.getInstance("SHA-256")
                                        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
                                    require(actual.equals(expected, ignoreCase = true)) {
                                        "Patch SHA-256 mismatch: ${patch.file}"
                                    }
                                }
                            }
                            manifest
                        }
                }
                .toList()
            return EnhancementCatalog(manifests)
        }
    }
}
