package me.magnum.enhancements

import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class EnhancementPackageInstaller(
    private val root: File,
) {
    fun install(input: InputStream): EnhancementManifest {
        root.mkdirs()
        val staging = File(root, ".installing-${UUID.randomUUID()}")
        staging.mkdirs()
        try {
            extractSafely(input, staging)
            val manifestFile = findManifest(staging)
            val manifest = EnhancementManifestParser.parse(manifestFile.readText())
            val packageDirectory = File(root, manifest.id)
            require(!packageDirectory.exists()) { "Enhancement is already installed" }
            val packageContents = manifestFile.parentFile ?: staging
            manifest.patches.forEach { patch ->
                require(File(packageContents, patch.file).isFile) {
                    "Missing enhancement patch file: ${patch.file}"
                }
            }
            require(packageContents.renameTo(packageDirectory)) {
                "Unable to install enhancement package"
            }
            return manifest
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun extractSafely(input: InputStream, staging: File) {
        ZipInputStream(input.buffered()).use { zip ->
            var entries = 0
            var extractedBytes = 0L
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entries <= MAX_ENTRIES) { "Enhancement package has too many files" }
                require(entry.compressedSize < MAX_ENTRY_BYTES || entry.compressedSize < 0) {
                    "Enhancement package entry is too large"
                }
                val relativePath = entry.name.replace('\\', '/')
                require(isSafeRelativePath(relativePath)) { "Unsafe enhancement package path" }
                val destination = File(staging, relativePath)
                val stagingPath = staging.canonicalPath + File.separator
                require(destination.canonicalPath.startsWith(stagingPath)) {
                    "Enhancement package escapes its staging directory"
                }
                if (entry.isDirectory) {
                    require(destination.mkdirs() || destination.isDirectory) {
                        "Unable to create enhancement package directory"
                    }
                    continue
                }
                destination.parentFile?.mkdirs()
                destination.outputStream().use { output ->
                    val copied = zip.copyTo(output, BUFFER_SIZE)
                    extractedBytes += copied
                    require(extractedBytes <= MAX_TOTAL_BYTES) { "Enhancement package is too large" }
                }
            }
        }
    }

    private fun findManifest(staging: File): File {
        val manifests = staging.walkTopDown()
            .filter { it.isFile && it.name == "manifest.json" }
            .toList()
        require(manifests.size == 1) { "Enhancement package must contain one manifest.json" }
        return manifests.single()
    }

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_ENTRIES = 256
        private const val MAX_ENTRY_BYTES = 16L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 64L * 1024 * 1024

        private fun isSafeRelativePath(path: String): Boolean {
            return path.isNotBlank() &&
                !path.startsWith("/") &&
                !path.contains(':') &&
                ".." !in path.split('/')
        }
    }
}
