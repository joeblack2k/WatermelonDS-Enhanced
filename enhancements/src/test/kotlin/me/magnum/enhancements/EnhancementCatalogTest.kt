package me.magnum.enhancements

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementCatalogTest {
    @Test
    fun loadsInternalAndExternalRootsWithOnlyCanonicalDirectPackages() {
        val internal = Files.createTempDirectory("enhancements-internal").toFile()
        val external = Files.createTempDirectory("enhancements-external").toFile()
        writeManifest(internal, "internal.addon")
        writeManifest(external, "external.addon")
        writeManifest(File(internal, ".staging"), "ignored.addon")
        File(internal, "nested/stray").mkdirs()
        File(internal, "nested/stray/manifest.json").writeText(manifest("stray.addon"))

        val catalog = EnhancementCatalog.loadFromRoots(listOf(internal, external))

        assertEquals(listOf("external.addon", "internal.addon"), catalog.manifests.map { it.id })
    }

    @Test
    fun directoryAndManifestMismatchFailsBeforeSession() {
        val root = Files.createTempDirectory("enhancements-mismatch").toFile()
        writeManifest(root, "wrong-directory", manifest("canonical.addon"))

        val error = try {
            EnhancementCatalog.loadFromRoots(listOf(root))
            throw AssertionError("Expected directory mismatch")
        } catch (error: IllegalArgumentException) {
            error
        }

        assertTrue(error.message.orEmpty().contains("does not match"))
    }

    @Test
    fun duplicateCanonicalIdsAcrossRootsFail() {
        val first = Files.createTempDirectory("enhancements-first").toFile()
        val second = Files.createTempDirectory("enhancements-second").toFile()
        writeManifest(first, "same.addon")
        writeManifest(second, "same.addon")

        val error = try {
            EnhancementCatalog.loadFromRoots(listOf(first, second))
            throw AssertionError("Expected duplicate id")
        } catch (error: IllegalArgumentException) {
            error
        }

        assertTrue(error.message.orEmpty().contains("Duplicate enhancement id"))
    }

    @Test
    fun matchingUsesGameCodeAndHeaderChecksum() {
        val catalog = EnhancementCatalog(
            listOf(
                EnhancementManifest(
                    id = "us.addon",
                    name = "US",
                    version = "1.0.0",
                    match = EnhancementMatch("ASMP", "1234ABCD"),
                ),
                EnhancementManifest(
                    id = "other-revision.addon",
                    name = "Other revision",
                    version = "1.0.0",
                    match = EnhancementMatch("ASMP", "87654321"),
                ),
                EnhancementManifest(
                    id = "other-game.addon",
                    name = "Other game",
                    version = "1.0.0",
                    match = EnhancementMatch("BEEE", "1234ABCD"),
                ),
            ),
        )

        assertEquals(
            listOf("us.addon"),
            catalog.matching(EnhancementRomIdentity("ASMP", "1234abcd", "")).map { it.id },
        )
    }

    private fun writeManifest(root: File, id: String, contents: String = manifest(id)) {
        val packageRoot = File(root, id)
        packageRoot.mkdirs()
        File(packageRoot, "manifest.json").writeText(contents)
    }

    private fun manifest(id: String) = """
        {
          "schemaVersion": 1,
          "id": "$id",
          "name": "Test",
          "version": "1.0.0",
          "match": {"gameCode": "ASMP", "headerChecksum": "12345678"}
        }
    """.trimIndent()
}
