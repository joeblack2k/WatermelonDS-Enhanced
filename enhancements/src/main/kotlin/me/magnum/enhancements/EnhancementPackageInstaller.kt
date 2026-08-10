package me.magnum.enhancements

import java.io.File
import java.io.InputStream
import java.util.HashSet
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
            staging.deleteRecursively()
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
            val paths = HashSet<String>()
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entries <= MAX_ENTRIES) { "Enhancement package has too many files" }
                val relativePath = canonicalRelativePath(entry.name)
                require(paths.add(relativePath)) { "Enhancement package contains duplicate paths" }
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
                    var entryBytes = 0L
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        entryBytes += read
                        extractedBytes += read
                        require(entryBytes <= MAX_ENTRY_BYTES) {
                            "Enhancement package entry is too large"
                        }
                        require(extractedBytes <= MAX_TOTAL_BYTES) {
                            "Enhancement package is too large"
                        }
                        output.write(buffer, 0, read)
                    }
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

        private fun canonicalRelativePath(rawPath: String): String {
            val path = rawPath.replace('\\', '/')
            require(path.isNotBlank() && !path.startsWith('/') && !path.contains(':')) {
                "Unsafe enhancement package path"
            }
            val segments = path.split('/').filter { it.isNotEmpty() && it != "." }
            require(segments.isNotEmpty() && ".." !in segments) {
                "Unsafe enhancement package path"
            }
            return segments.joinToString("/")
        }
    }
}
