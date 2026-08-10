package me.magnum.enhancements

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementPackageInstallerTest {
    @Test
    fun installsValidatedPackageUnderManifestId() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val manifest = """{
            "id": "test.addon",
            "name": "Test",
            "version": "1.0.0",
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
            "patches": [{"type": "IPS", "file": "payload/test.ips", "apply": "TEMPORARY_COPY"}]
        }""".trimIndent()

        val installed = EnhancementPackageInstaller(root).install(
            zipOf(
                "bundle/manifest.json" to manifest,
                "bundle/payload/test.ips" to "patch",
            ),
        )

        assertEquals("test.addon", installed.id)
        assertTrue(File(root, "test.addon/manifest.json").isFile)
        assertTrue(File(root, "test.addon/payload/test.ips").isFile)
    }

    @Test
    fun rejectsMissingNestedPatchWithoutLeavingInstalledOrStagingFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val manifest = """{
            "id": "test.addon",
            "name": "Test",
            "version": "1.0.0",
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
            "patches": [{"type": "IPS", "file": "payload/missing.ips", "apply": "TEMPORARY_COPY"}]
        }""".trimIndent()

        var rejected = false
        try {
            EnhancementPackageInstaller(root).install(zipOf("bundle/manifest.json" to manifest))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertFalse(File(root, "test.addon").exists())
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun rejectsTraversalWithoutLeavingInstalledFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()
        var rejected = false
        try {
            EnhancementPackageInstaller(root).install(zipOf("../manifest.json" to "{}"))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun rejectsOversizedEntryWithoutLeavingStagingFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val oversized = ByteArray(16 * 1024 * 1024 + 1)

        var rejected = false
        try {
            EnhancementPackageInstaller(root).install(zipOfBytes("payload.bin" to oversized))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun rejectsAliasedDuplicateWithoutLeavingStagingFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()

        var rejected = false
        try {
            EnhancementPackageInstaller(root).install(
                zipOf(
                    "bundle/manifest.json" to "{}",
                    "bundle\\manifest.json" to "{}",
                ),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun rejectsDotAndSlashAliasedDuplicateWithoutLeavingStagingFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()

        var rejected = false
        try {
            EnhancementPackageInstaller(root).install(
                zipOf(
                    "bundle/manifest.json" to "{}",
                    "bundle/./manifest.json" to "{}",
                ),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun promotesNestedPackageWithoutLeavingStagingDirectory() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val manifest = """{
            "id": "test.addon",
            "name": "Test",
            "version": "1.0.0",
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
            "patches": [{"type": "IPS", "file": "payload/test.ips", "apply": "TEMPORARY_COPY"}]
        }""".trimIndent()

        EnhancementPackageInstaller(root).install(
            zipOf(
                "bundle/manifest.json" to manifest,
                "bundle/payload/test.ips" to "patch",
            ),
        )

        assertEquals(listOf("test.addon"), root.list()?.sorted()?.toList())
        assertEquals(
            listOf("manifest.json", "payload"),
            File(root, "test.addon").list()?.sorted()?.toList(),
        )
        assertTrue(File(root, "test.addon/payload/test.ips").isFile)
        assertFalse(root.listFiles()?.any { it.name.startsWith(".installing-") } == true)
        root.deleteRecursively()
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private fun zipOfBytes(vararg entries: Pair<String, ByteArray>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }
}
