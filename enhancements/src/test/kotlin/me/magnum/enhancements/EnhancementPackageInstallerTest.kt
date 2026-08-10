package me.magnum.enhancements

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
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
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"}
        }""".trimIndent()

        val installed = EnhancementPackageInstaller(root).install(zipOf("bundle/manifest.json" to manifest))

        assertEquals("test.addon", installed.id)
        assertTrue(File(root, "test.addon/manifest.json").isFile)
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
}
