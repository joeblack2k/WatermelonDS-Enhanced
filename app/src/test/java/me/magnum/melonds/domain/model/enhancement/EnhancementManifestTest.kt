package me.magnum.melonds.domain.model.enhancement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementManifestTest {
    private val hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun exactIdentityIsRequired() {
        val manifest = EnhancementManifest(
            id = "mario-camera",
            name = "Mario camera",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            capabilities = setOf(
                EnhancementCapability.CONTROLLER_AXIS_OWNER,
                EnhancementCapability.RUNTIME_INPUT_PROTOCOL,
            ),
        )

        assertTrue(manifest.matches(EnhancementRomIdentity("ASMP", "12345678", "")))
        assertTrue(!manifest.matches(EnhancementRomIdentity("ASMP", "87654321", "")))
        assertTrue(!manifest.matches(EnhancementRomIdentity("XXXX", "12345678", "")))
    }

    @Test
    fun parserRejectsUnsafePatchPaths() {
        val json = """
            {
              "id": "mario-camera",
              "name": "Mario camera",
              "version": "1.0.0",
              "match": {
                "gameCode": "ASMP",
                "headerChecksum": "12345678"
              },
              "patches": [
                {"type": "IPS", "file": "../camera.ips"}
              ]
            }
        """.trimIndent()

        var rejected = false
        try {
            EnhancementManifestParser.parse(json)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun catalogReturnsOnlyExactMatches() {
        val manifest = EnhancementManifest(
            id = "mario-camera",
            name = "Mario camera",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
        )
        val catalog = EnhancementCatalog(listOf(manifest))

        assertEquals(listOf(manifest), catalog.matching(EnhancementRomIdentity("ASMP", "12345678", "")))
        assertTrue(catalog.matching(EnhancementRomIdentity("ASMP", "87654321", "")).isEmpty())
    }

    @Test
    fun sessionRejectsAnEnhancementThatDoesNotMatch() {
        val manifest = EnhancementManifest(
            id = "mario-camera",
            name = "Mario camera",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", sha256 = setOf(hash)),
        )
        val catalog = EnhancementCatalog(listOf(manifest))

        var rejected = false
        try {
            catalog.createSession(
                EnhancementRomIdentity("XXXX", "12345678", ""),
                setOf("mario-camera"),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
