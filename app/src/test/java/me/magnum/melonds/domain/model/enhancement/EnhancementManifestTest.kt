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
            match = EnhancementMatch("ASMP", sha256 = setOf(hash)),
            capabilities = setOf(
                EnhancementCapability.CONTROLLER_AXIS_OWNER,
                EnhancementCapability.RUNTIME_INPUT_PROTOCOL,
            ),
        )

        assertTrue(manifest.matches(EnhancementRomIdentity("ASMP", null, hash)))
        assertTrue(!manifest.matches(EnhancementRomIdentity("ASMP", null, hash.dropLast(1) + "0")))
        assertTrue(!manifest.matches(EnhancementRomIdentity("XXXX", null, hash)))
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
                "sha256": ["$hash"]
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
            match = EnhancementMatch("ASMP", sha256 = setOf(hash)),
        )
        val catalog = EnhancementCatalog(listOf(manifest))

        assertEquals(listOf(manifest), catalog.matching(EnhancementRomIdentity("ASMP", null, hash)))
        assertTrue(catalog.matching(EnhancementRomIdentity("ASMP", null, hash.reversed())).isEmpty())
    }
}
