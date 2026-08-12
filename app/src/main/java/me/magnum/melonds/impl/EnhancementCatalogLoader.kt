package me.magnum.melonds.impl

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementManifest
import me.magnum.enhancements.EnhancementManifestParser
import me.magnum.enhancements.EnhancementPackageInstaller
import java.io.File
import javax.inject.Inject

class EnhancementCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun stage(packageUri: Uri): EnhancementPackageInstaller.Staged {
        val input = requireNotNull(context.contentResolver.openInputStream(packageUri)) {
            "Unable to open enhancement package"
        }
        return input.use {
            EnhancementPackageInstaller(File(context.filesDir, "Enhancements")).stage(it)
        }
    }

    fun install(
        staged: EnhancementPackageInstaller.Staged,
        replace: Boolean = false,
    ): EnhancementManifest {
        return EnhancementPackageInstaller(File(context.filesDir, "Enhancements")).install(staged, replace)
    }

    fun load(): EnhancementCatalog {
        val roots = listOfNotNull(
            File(context.filesDir, "Enhancements"),
            context.getExternalFilesDir(null)?.let { File(it, "Enhancements") },
        )
        val installed = EnhancementCatalog.loadFromRoots(roots)
        val bundled = context.assets.list("enhancements").orEmpty().mapNotNull { id ->
            context.assets.open("enhancements/$id/manifest.json").use {
                EnhancementManifestParser.parse(it.bufferedReader().readText())
            }
        }
        return installed.mergeBundled(EnhancementCatalog(bundled))
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
