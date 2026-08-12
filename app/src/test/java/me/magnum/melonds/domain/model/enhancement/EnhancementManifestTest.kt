package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import me.magnum.enhancements.EnhancementCapability
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementManifest
import me.magnum.enhancements.EnhancementManifestParser
import me.magnum.enhancements.EnhancementMatch
import me.magnum.enhancements.EnhancementRomIdentity
import me.magnum.enhancements.createSession
import me.magnum.melonds.impl.dtos.rom.RomConfigDto
import me.magnum.melonds.domain.model.rom.config.RuntimeConsoleType
import me.magnum.melonds.domain.model.rom.config.RuntimeMicSource

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
    fun sha256GuardCannotMatchAnotherGameCode() {
        val manifest = EnhancementManifest(
            id = "mario-camera",
            name = "Mario camera",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", sha256 = setOf(hash)),
        )

        assertTrue(!manifest.matches(EnhancementRomIdentity("XXXX", null, hash)))
        assertTrue(manifest.matches(EnhancementRomIdentity("ASMP", null, hash)))
        assertTrue(!manifest.matches(EnhancementRomIdentity("ASMP", null, hash.replaceFirst('0', 'f'))))
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

    @Test
    fun sessionReportsHardcoreIncompatibility() {
        val manifest = EnhancementManifest(
            id = "camera",
            name = "Camera",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            hardcoreCompatible = false,
        )

        val session = EnhancementCatalog(listOf(manifest)).createSession(
            EnhancementRomIdentity("ASMP", "12345678", ""),
            setOf("camera"),
        )

        assertTrue(!session.hardcoreCompatible)
    }

    @Test
    fun sessionRejectsConflictingAddOns() {
        val first = EnhancementManifest(
            id = "first",
            name = "First",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            conflictsWith = setOf("second"),
        )
        val second = first.copy(id = "second", name = "Second", conflictsWith = emptySet())

        var rejected = false
        try {
            EnhancementCatalog(listOf(first, second)).createSession(
                EnhancementRomIdentity("ASMP", "12345678", ""),
                setOf("first", "second"),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oldRomConfigDefaultsToNoEnabledEnhancements() {
        val config = RomConfigDto(
            runtimeConsoleType = RuntimeConsoleType.DEFAULT,
            runtimeMicSource = RuntimeMicSource.DEFAULT,
            layoutId = null,
            gbaSlotConfig = me.magnum.melonds.impl.dtos.rom.RomGbaSlotConfigDto.fromModel(
                me.magnum.melonds.domain.model.rom.config.RomGbaSlotConfig.None,
            ),
        ).toModel()

        assertTrue(config.enabledEnhancements.isEmpty())
    }
}
