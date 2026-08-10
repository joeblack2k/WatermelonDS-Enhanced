package me.magnum.melonds.domain.model.enhancement

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
}
