package me.magnum.melonds.impl

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementManifest
import me.magnum.enhancements.EnhancementPackageInstaller
import java.io.File
import javax.inject.Inject

class EnhancementCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun install(packageUri: Uri, replace: Boolean = false): EnhancementManifest {
        val input = requireNotNull(context.contentResolver.openInputStream(packageUri)) {
            "Unable to open enhancement package"
        }
        return input.use {
            EnhancementPackageInstaller(File(context.filesDir, "Enhancements")).install(it, replace)
        }
    }

    fun load(): EnhancementCatalog {
        val roots = listOfNotNull(
            File(context.filesDir, "Enhancements"),
            context.getExternalFilesDir(null)?.let { File(it, "Enhancements") },
        )
        return EnhancementCatalog.loadFromRoots(roots)
    }

    fun readFiles(manifest: me.magnum.enhancements.EnhancementManifest, paths: Set<String>): Map<String, ByteArray> {
        val roots = listOfNotNull(
            File(context.filesDir, "Enhancements"),
            context.getExternalFilesDir(null)?.let { File(it, "Enhancements") },
        )
        val packageRoot = roots.asSequence()
            .map { File(it, manifest.id) }
            .firstOrNull { it.isDirectory }
            ?: error("Installed enhancement package not found: ${manifest.id}")
        return paths.associateWith { relativePath ->
            require(!relativePath.startsWith("/") && ".." !in relativePath.split('/')) {
                "Unsafe enhancement package path"
            }
            val file = File(packageRoot, relativePath)
            val packageRootPath = packageRoot.canonicalPath + File.separator
            require(file.canonicalPath.startsWith(packageRootPath) && file.isFile) {
                "Enhancement package file not found: $relativePath"
            }
            file.readBytes()
        }
    }
}
