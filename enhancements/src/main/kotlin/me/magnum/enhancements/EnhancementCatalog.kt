package me.magnum.enhancements

import java.io.File

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
                            manifest
                        }
                }
                .toList()
            return EnhancementCatalog(manifests)
        }
    }
}
