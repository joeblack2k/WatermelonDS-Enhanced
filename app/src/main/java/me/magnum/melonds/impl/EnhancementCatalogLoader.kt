package me.magnum.melonds.impl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementManifestParser
import java.io.File
import javax.inject.Inject

class EnhancementCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun load(): EnhancementCatalog {
        val roots = listOfNotNull(
            File(context.filesDir, "Enhancements"),
            context.getExternalFilesDir(null)?.let { File(it, "Enhancements") },
        )
        val manifests = roots.asSequence()
            .filter(File::isDirectory)
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.name == "manifest.json" }
                    .asSequence()
            }
            .map { EnhancementManifestParser.parse(it.readText()) }
            .toList()
        return EnhancementCatalog(manifests)
    }
}
