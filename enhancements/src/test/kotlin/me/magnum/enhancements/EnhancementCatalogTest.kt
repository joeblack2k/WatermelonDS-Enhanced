package me.magnum.enhancements

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class EnhancementCatalogTest {
    @Test
    fun installableMatchesExcludeSourceOnlyManifests() {
        val sourceOnly = EnhancementManifestParser.parse(manifest("source.only"))
            .copy(schemaVersion = 3, distributionStatus = EnhancementDistributionStatus.SOURCE_ONLY)
        val installable = EnhancementManifestParser.parse(manifest("installable"))
            .copy(schemaVersion = 3, distributionStatus = EnhancementDistributionStatus.INSTALLABLE)
        val catalog = EnhancementCatalog(listOf(sourceOnly, installable))

        assertEquals(
            listOf(installable),
            catalog.installableMatching(EnhancementRomIdentity("ASMP", "12345678", "")),
        )
    }

    @Test
    fun directoryCatalogRejectsTamperedHashedPatch() {
        val root = Files.createTempDirectory("enhancements-hash").toFile()
        val packageRoot = File(root, "hashed.addon").also { it.mkdirs() }
        File(packageRoot, "patch.ards").writeText("00 00000000")
        val hash = MessageDigest.getInstance("SHA-256").digest("different".toByteArray())
            .joinToString("") { "%02x".format(it) }
        File(packageRoot, "manifest.json").writeText(
            """{"schemaVersion":3,"id":"hashed.addon","name":"Hashed","version":"1",
                "distributionStatus":"SOURCE_ONLY",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                "capabilities":["RUNTIME_CODE_PATCH"],
                "patches":[{"type":"ACTION_REPLAY","file":"patch.ards","provenance":"test","sha256":"$hash"}]}""",
        )
        try {
            EnhancementCatalog.loadFromRoots(listOf(root))
            throw AssertionError("Expected tampered patch rejection")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("SHA-256 mismatch"))
        }
    }
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

    @Test
    fun matchingRejectsRevisionOrRetroAchievementsMismatch() {
        val catalog = EnhancementCatalog(
            listOf(
                EnhancementManifest(
                    id = "exact.addon",
                    name = "Exact",
                    version = "1.0.0",
                    match = EnhancementMatch(
                        "ASMP",
                        revision = 0,
                        raHashes = setOf("ba3c4052e00c5cc31df5d5534c39de1b"),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("exact.addon"),
            catalog.matching(
                EnhancementRomIdentity(
                    "ASMP",
                    null,
                    "",
                    revision = 0,
                    raHash = "BA3C4052E00C5CC31DF5D5534C39DE1B",
                ),
            ).map { it.id },
        )
        assertTrue(
            catalog.matching(
                EnhancementRomIdentity("ASMP", null, "", revision = 1, raHash = "ba3c4052e00c5cc31df5d5534c39de1b"),
            ).isEmpty(),
        )
        assertTrue(
            catalog.matching(
                EnhancementRomIdentity("ASMP", null, "", revision = 0, raHash = "00000000000000000000000000000000"),
            ).isEmpty(),
        )
    }

    @Test
    fun checkedInPackagesHaveCanonicalIdsAndRemainSourceOnly() {
        val root = listOf(File("enhancements"), File("."))
            .map { File(it, "sm64ds.eu.60fps") }
            .first { it.isDirectory }
            .parentFile
        val catalog = EnhancementCatalog.loadFromRoots(listOf(root))
        assertEquals(
            listOf("sm64ds.eu.60fps", "sm64ds.eu.right-stick-camera", "sm64ds.eu.widescreen"),
            catalog.manifests.map { it.id },
        )
        catalog.manifests.forEach {
            assertEquals(EnhancementDistributionStatus.SOURCE_ONLY, it.distributionStatus)
            assertTrue(!it.isInstallable())
        }
    }

    @Test
    fun revisionMismatchFailsClosedEvenWhenGameCodeMatches() {
        val manifest = EnhancementManifest(
            schemaVersion = 3,
            id = "exact.addon",
            name = "Exact",
            version = "1.0.0",
            distributionStatus = EnhancementDistributionStatus.INSTALLABLE,
            match = EnhancementMatch("ASMP", revision = 0, raHashes = setOf("ba3c4052e00c5cc31df5d5534c39de1b")),
        )
        val catalog = EnhancementCatalog(listOf(manifest))
        assertTrue(
            catalog.installableMatching(
                EnhancementRomIdentity("ASMP", null, "", revision = 1, raHash = "ba3c4052e00c5cc31df5d5534c39de1b"),
            ).isEmpty(),
        )
    }

    private fun writeManifest(root: File, id: String, contents: String = manifest(id)) {
        val packageRoot = File(root, id)
        packageRoot.mkdirs()
        File(packageRoot, "manifest.json").writeText(contents)
    }

    private fun manifest(id: String) = """
        {
          "schemaVersion": 3,
          "id": "$id",
          "name": "Test",
          "version": "1.0.0",
          "distributionStatus": "SOURCE_ONLY",
          "match": {"gameCode": "ASMP", "headerChecksum": "12345678"}
        }
    """.trimIndent()
}
