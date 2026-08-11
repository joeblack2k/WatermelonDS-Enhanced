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
            .map { it.resolve("sm64ds.eu.60fps") }
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
    fun legacyManifestWithoutStatusPreservesProvenInstallableSemantics() {
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
        assertTrue(parsed.isInstallable())
    }

    @Test
    fun schemaV3RequiresDistributionStatusAndLegacySchemasRemainNonDistributable() {
        try {
            EnhancementManifestParser.parse(
                """{"schemaVersion":3,"id":"missing.status","name":"Missing","version":"1",
                    "match":{"gameCode":"ASMP","headerChecksum":"12345678"}}""".trimIndent(),
            )
            fail("Expected missing distribution status to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("distributionStatus"))
        }
        val v3 = EnhancementManifestParser.parse(
            """{"schemaVersion":3,"id":"v3.addon","name":"V3","version":"1",
                "distributionStatus":"SOURCE_ONLY",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                "verification":{"payloadInput":"VERIFIED"}}""",
        )
        assertEquals(EnhancementDistributionStatus.SOURCE_ONLY, v3.distributionStatus)
        assertTrue(!v3.isInstallable())
        val distributable = EnhancementManifestParser.parse(
            """{"schemaVersion":3,"id":"installable.addon","name":"Installable","version":"1",
                "distributionStatus":"INSTALLABLE",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                "verification":{"cadence":"UNVERIFIED"}}""",
        )
        assertTrue(distributable.isInstallable())
        assertEquals(EnhancementClaim.UNVERIFIED, distributable.verification.cadence)
        val parsed = EnhancementManifestParser.parse(
            """{"schemaVersion":2,"id":"v2.addon","name":"V2","version":"1",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                "status":"SOURCE_ONLY"}""".trimIndent(),
        )
        assertTrue(parsed.schemaVersion == 2)
        assertTrue(!parsed.isInstallable())
        assertTrue(EnhancementManifestParser.parse(
            """{"schemaVersion":1,"id":"v1.addon","name":"V1","version":"1",
                "match":{"gameCode":"ASMP","headerChecksum":"12345678"}}""",
        ).let { it.schemaVersion == 1 && it.isInstallable() })
    }

    @Test
    fun verifiedManifestRequiresEveryClaimAndPayload() {
        val incomplete = EnhancementManifest(
            id = "verified.addon",
            name = "Verified",
            version = "1.0.0",
            status = EnhancementStatus.VERIFIED,
            distributionStatus = EnhancementDistributionStatus.INSTALLABLE,
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
