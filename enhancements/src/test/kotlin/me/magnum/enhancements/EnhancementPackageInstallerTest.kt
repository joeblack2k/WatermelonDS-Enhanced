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
import org.junit.Assert.assertThrows
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

    @Test
    fun preflightsRuntimePayloadsBeforePromotion() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val manifest = """{
            "id": "test.addon",
            "name": "Test",
            "version": "1.0.0",
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
            "patches": [
                {"type": "ACTION_REPLAY", "file": "payload/test.ards", "provenance": "test"},
                {"type": "RUNTIME_OVERLAY", "file": "payload/test.overlay", "provenance": "test",
                 "expectedOriginalWords": {"0x02000000": "0xE1A00000"}}
            ]
        }""".trimIndent()

        EnhancementPackageInstaller(root).install(
            zipOf(
                "bundle/manifest.json" to manifest,
                "bundle/payload/test.ards" to "02000000 00000001",
                "bundle/payload/test.overlay" to "02000000 E3A00000",
            ),
        )

        assertTrue(File(root, "test.addon/payload/test.ards").isFile)
        assertTrue(File(root, "test.addon/payload/test.overlay").isFile)
        assertFalse(root.listFiles()?.any { it.name.startsWith(".installing-") } == true)
        root.deleteRecursively()
    }

    @Test
    fun rejectsInvalidRuntimePayloadWithoutPromotionOrStagingFiles() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val manifest = """{
            "id": "test.addon",
            "name": "Test",
            "version": "1.0.0",
            "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
            "patches": [{"type": "ACTION_REPLAY", "file": "payload/test.ards", "provenance": "test"}]
        }""".trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            EnhancementPackageInstaller(root).install(
                zipOf(
                    "bundle/manifest.json" to manifest,
                    "bundle/payload/test.ards" to "not an action replay code",
                ),
            )
        }

        assertFalse(File(root, "test.addon").exists())
        assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
        root.deleteRecursively()
    }

    @Test
    fun rejectsOddActionReplayWordCountsDuringInstallerPreflight() {
        listOf("02000000", "02000000 00000001 D0000000").forEach { payload ->
            val root = Files.createTempDirectory("enhancements").toFile()
            val manifest = """{
                "id": "test.addon",
                "name": "Test",
                "version": "1.0.0",
                "match": {"gameCode": "ASMP", "headerChecksum": "12345678"},
                "patches": [{"type": "ACTION_REPLAY", "file": "payload/test.ards", "provenance": "test"}]
            }"""

            assertThrows(IllegalArgumentException::class.java) {
                EnhancementPackageInstaller(root).install(
                    zipOf(
                        "bundle/manifest.json" to manifest,
                        "bundle/payload/test.ards" to payload,
                    ),
                )
            }
            assertFalse(File(root, "test.addon").exists())
            assertEquals(emptyList<String>(), root.list()?.toList().orEmpty())
            root.deleteRecursively()
        }
    }

    @Test
    fun replacementValidatesBeforeReplacingAndKeepsOtherGamesAndLoadedSessionStable() {
        val root = Files.createTempDirectory("enhancements").toFile()
        val original = manifest("game-a.addon", "1.0.0", "ASMP")
        val replacement = manifest("game-a.addon", "2.0.0", "ASMP")
        val otherGame = manifest("game-b.addon", "1.0.0", "BEEE")

        EnhancementPackageInstaller(root).install(zipOf("bundle/manifest.json" to original))
        EnhancementPackageInstaller(root).install(zipOf("bundle/manifest.json" to otherGame))
        val existingSession = EnhancementCatalog.loadFromRoots(listOf(root))

        EnhancementPackageInstaller(root).install(
            zipOf("bundle/manifest.json" to replacement),
            replace = true,
        )

        assertEquals("1.0.0", existingSession.manifests.single { it.id == "game-a.addon" }.version)
        assertEquals("2.0.0", EnhancementCatalog.loadFromRoots(listOf(root))
            .manifests.single { it.id == "game-a.addon" }.version)
        assertEquals("1.0.0", EnhancementCatalog.loadFromRoots(listOf(root))
            .manifests.single { it.id == "game-b.addon" }.version)
        assertFalse(root.listFiles()?.any { it.name.startsWith(".backup-") || it.name.startsWith(".installing-") } == true)
        root.deleteRecursively()
    }

    @Test
    fun invalidReplacementLeavesPreviousPackageInPlace() {
        val root = Files.createTempDirectory("enhancements").toFile()
        EnhancementPackageInstaller(root).install(
            zipOf("bundle/manifest.json" to manifest("test.addon", "1.0.0", "ASMP")),
        )

        assertThrows(IllegalArgumentException::class.java) {
            EnhancementPackageInstaller(root).install(
                zipOf("bundle/manifest.json" to manifest(
                    "test.addon",
                    "2.0.0",
                    "ASMP",
                    patches = """[{"type":"ACTION_REPLAY","file":"payload/bad.ards","provenance":"test"}]""",
                ),
                    "bundle/payload/bad.ards" to "invalid"),
                replace = true,
            )
        }

        assertEquals("1.0.0", EnhancementCatalog.loadFromRoots(listOf(root))
            .manifests.single().version)
        assertFalse(root.listFiles()?.any { it.name.startsWith(".backup-") || it.name.startsWith(".installing-") } == true)
        root.deleteRecursively()
    }

    @Test
    fun keepsBackupWhenPromotionAndRestoreBothFail() {
        val root = Files.createTempDirectory("enhancements").toFile()
        EnhancementPackageInstaller(root).install(
            zipOf("bundle/manifest.json" to manifest("test.addon", "1.0.0", "ASMP")),
        )
        var moves = 0
        assertThrows(IllegalArgumentException::class.java) {
            EnhancementPackageInstaller(root, rename = { from, to ->
                moves++
                if (moves >= 2) {
                    to.mkdirs()
                    false
                } else {
                    from.renameTo(to)
                }
            }).install(
                zipOf("bundle/manifest.json" to manifest("test.addon", "2.0.0", "ASMP")),
                replace = true,
            )
        }
        assertTrue(root.listFiles()?.any { it.name.startsWith(".backup-") } == true)
        root.deleteRecursively()
    }

    private fun manifest(
        id: String,
        version: String,
        gameCode: String,
        patches: String = "[]",
    ) = """
        {"schemaVersion":1,"id":"$id","name":"Test","version":"$version",
         "match":{"gameCode":"$gameCode","headerChecksum":"12345678"},"patches":$patches}
    """.trimIndent()

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
