package me.magnum.enhancements

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class EnhancementManifestValidationTest {
    @Test
    fun allExampleManifestsParseWithTheProductionParser() {
        val root = listOf(Path.of("enhancements"), Path.of("."))
            .map { it.resolve("sm64ds-eu-60fps") }
            .first { Files.isDirectory(it) }
            .parent
        val examples = Files.walk(root).use { paths ->
            paths.filter { it.fileName.toString() == "manifest.example.json" }.toList()
        }

        assertEquals(3, examples.size)
        examples.forEach { path ->
            EnhancementManifestParser.parse(Files.readString(path))
        }
    }
    @Test
    fun sourceOnlyManifestMayDeclareMissingEvidence() {
        EnhancementManifest(
            id = "source.only",
            name = "Source only",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
        ).also(EnhancementManifestParser::validate)
    }

    @Test
    fun legacyManifestWithoutStatusRemainsInstallable() {
        val legacy = EnhancementManifest(
            id = "legacy.addon",
            name = "Legacy",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
        )

        val parsed = EnhancementManifestParser.parse(
            """{"id":"legacy.addon","name":"Legacy","version":"1.0.0",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"}}""".trimIndent(),
        )
        assertTrue(parsed.status == null)
    }

    @Test
    fun schemaV2ParsesWhileSchemaV1RemainsCompatible() {
        val parsed = EnhancementManifestParser.parse(
            """{"schemaVersion":2,"id":"v2.addon","name":"V2","version":"1",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                "status":"SOURCE_ONLY"}""".trimIndent(),
        )
        assertTrue(parsed.schemaVersion == 2)
        assertTrue(EnhancementManifestParser.parse(
            """{"schemaVersion":1,"id":"v1.addon","name":"V1","version":"1",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"}}""",
        ).schemaVersion == 1)
    }

    @Test
    fun verifiedManifestRequiresEveryClaimAndPayload() {
        val incomplete = EnhancementManifest(
            id = "verified.addon",
            name = "Verified",
            version = "1.0.0",
            status = EnhancementStatus.VERIFIED,
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            verification = EnhancementVerification(
                guardedPayload = EnhancementClaim.VERIFIED,
                payloadInput = EnhancementPayloadInput.VERIFIED,
            ),
        )
        try {
            EnhancementManifestParser.validate(incomplete)
            fail("Expected incomplete verification to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("every timing contract claim"))
        }
    }

    @Test
    fun acceptsSupportedPatchApplyPairs() {
        listOf(
            EnhancementPatchType.ACTION_REPLAY to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.RUNTIME_OVERLAY to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.IPS to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.BPS to EnhancementPatchApply.TEMPORARY_COPY,
        ).forEach { (type, apply) ->
            EnhancementManifestParser.validate(manifest(type, apply))
        }
    }

    @Test
    fun rejectsInversePatchApplyPairs() {
        listOf(
            EnhancementPatchType.ACTION_REPLAY to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.RUNTIME_OVERLAY to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.IPS to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.BPS to EnhancementPatchApply.RUNTIME,
        ).forEach { (type, apply) ->
            try {
                EnhancementManifestParser.validate(manifest(type, apply))
                throw AssertionError("Expected ${type}/${apply} to be rejected")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains(type.name))
            }
        }
    }

    @Test
    fun rejectsMalformedRomIdentity() {
        listOf(
            EnhancementMatch("asmP", "12345678"),
            EnhancementMatch("ASM", "12345678"),
            EnhancementMatch("ASM!", "12345678"),
            EnhancementMatch("ASMP", "1234567"),
            EnhancementMatch("ASMP", "1234567G"),
        ).forEach { match ->
            try {
                EnhancementManifest(
                    id = "test.addon",
                    name = "Test",
                    version = "1.0.0",
                    match = match,
                ).also(EnhancementManifestParser::validate)
                fail("Expected malformed identity to be rejected: $match")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains("Game code") ||
                    error.message.orEmpty().contains("checksum"))
            }
        }
    }

    @Test
    fun acceptsRevisionAndRetroAchievementsIdentity() {
        EnhancementManifest(
            id = "ra.addon",
            name = "RA",
            version = "1.0.0",
            match = EnhancementMatch(
                gameCode = "ASMP",
                revision = 0,
                raHashes = setOf("ba3c4052e00c5cc31df5d5534c39de1b"),
            ),
        ).also(EnhancementManifestParser::validate)
    }

    @Test
    fun rejectsMalformedRetroAchievementsHash() {
        try {
            EnhancementManifest(
                id = "bad-ra.addon",
                name = "Bad RA",
                version = "1.0.0",
                match = EnhancementMatch("ASMP", raHashes = setOf("not-a-hash")),
            ).also(EnhancementManifestParser::validate)
            fail("Expected malformed RA hash to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("RetroAchievements"))
        }
    }

    private fun manifest(type: EnhancementPatchType, apply: EnhancementPatchApply) =
        EnhancementManifest(
            id = "test.addon",
            name = "Test",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            patches = listOf(
                EnhancementPatch(
                    type = type,
                    file = "payload/patch",
                    apply = apply,
                    provenance = if (type == EnhancementPatchType.ACTION_REPLAY ||
                        type == EnhancementPatchType.RUNTIME_OVERLAY
                    ) "test" else "",
                ),
            ),
        )
}
