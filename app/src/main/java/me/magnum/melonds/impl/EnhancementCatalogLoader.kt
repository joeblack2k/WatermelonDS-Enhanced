package me.magnum.melonds.impl

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementManifest
import me.magnum.enhancements.EnhancementManifestParser
import me.magnum.enhancements.EnhancementPackageInstaller
import java.io.File
import java.security.MessageDigest
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
        val installed = loadInstalled(roots)
        val bundled = context.assets.list("enhancements").orEmpty().mapNotNull { id ->
            context.assets.open("enhancements/$id/manifest.json").use {
                EnhancementManifestParser.parse(it.bufferedReader().readText())
            }
        }
        return installed.mergeBundled(EnhancementCatalog(bundled))
    }

    internal companion object {
        fun loadInstalled(roots: List<File>): EnhancementCatalog {
            return EnhancementCatalog(
                roots.asSequence()
                    .filter(File::isDirectory)
                    .flatMap { root ->
                        root.listFiles().orEmpty().asSequence()
                            .filter { it.isDirectory && !it.name.startsWith(".") }
                    }
                    .mapNotNull { packageRoot ->
                        runCatching {
                            val manifestFile = File(packageRoot, "manifest.json")
                            require(manifestFile.isFile)
                            val manifest = EnhancementManifestParser.parse(manifestFile.readText())
                            require(packageRoot.name == manifest.id) {
                                "Enhancement directory does not match manifest id: ${packageRoot.name}"
                            }
                            manifest.patches.forEach { patch ->
                                patch.sha256?.let { expected ->
                                    val file = File(packageRoot, patch.file)
                                    require(file.isFile) {
                                        "Missing enhancement patch file: ${patch.file}"
                                    }
                                    val actual = MessageDigest.getInstance("SHA-256")
                                        .digest(file.readBytes())
                                        .joinToString("") { "%02x".format(it) }
                                    require(actual.equals(expected, ignoreCase = true)) {
                                        "Patch SHA-256 mismatch: ${patch.file}"
                                    }
                                }
                            }
                            manifest
                        }.getOrNull()
                    }
                    .toList(),
            )
        }

        fun acceptedPackageRoot(
            roots: List<File>,
            manifest: EnhancementManifest,
        ): File? {
            return roots.asSequence()
                .map { File(it, manifest.id) }
                .firstOrNull { it.isAcceptedPackage(manifest) }
        }
    }

    fun readFiles(manifest: me.magnum.enhancements.EnhancementManifest, paths: Set<String>): Map<String, ByteArray> {
        val roots = listOfNotNull(
            File(context.filesDir, "Enhancements"),
            context.getExternalFilesDir(null)?.let { File(it, "Enhancements") },
        )
        val packageRoot = acceptedPackageRoot(roots, manifest)
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

private fun File.isAcceptedPackage(manifest: EnhancementManifest): Boolean {
    if (!isDirectory || name != manifest.id) return false
    return runCatching {
        val loaded = EnhancementManifestParser.parse(File(this, "manifest.json").readText())
        require(loaded.id == manifest.id)
        manifest.patches.forEach { patch ->
            patch.sha256?.let { expected ->
                val file = File(this, patch.file)
                require(file.isFile)
                val actual = MessageDigest.getInstance("SHA-256")
                    .digest(file.readBytes())
                    .joinToString("") { "%02x".format(it) }
                require(actual.equals(expected, ignoreCase = true))
            }
        }
    }.isSuccess
}
