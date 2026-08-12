package me.magnum.enhancements

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EnhancementManifestInstallabilityTest {
    private val match = EnhancementMatch(gameCode = "ABCD", headerChecksum = "12345678")

    @Test
    fun v3PromotionUsesDistributionStatus() {
        val source = EnhancementManifest(
            schemaVersion = 3,
            id = "test",
            name = "Test",
            version = "1",
            match = match,
            distributionStatus = EnhancementDistributionStatus.SOURCE_ONLY,
        )
        val promoted = source.copy(distributionStatus = EnhancementDistributionStatus.INSTALLABLE)

        assertFalse(source.isInstallable())
        assertTrue(promoted.isInstallable())
    }

    @Test
    fun v3RejectsContradictoryLegacyStatus() {
        val contradictory = EnhancementManifest(
            schemaVersion = 3,
            id = "test",
            name = "Test",
            version = "1",
            status = EnhancementStatus.SOURCE_ONLY,
            distributionStatus = EnhancementDistributionStatus.INSTALLABLE,
            match = match,
        )

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            EnhancementManifestParser.validate(contradictory)
        }
    }

    @Test
    fun sm64dsPackagesDeclareIndependentCapabilityContracts() {
        val root = listOf(File("sm64ds.eu.60fps"), File("../enhancements/sm64ds.eu.60fps"))
            .firstOrNull(File::isDirectory)
            ?: error("SM64DS enhancement packages are missing")
        val manifests = listOf(
            "sm64ds.eu.right-stick-camera",
            "sm64ds.eu.widescreen",
            "sm64ds.eu.60fps",
        ).map { id ->
            EnhancementManifestParser.parse(
                File(root.parentFile, "$id/manifest.json").readText(),
            )
        }.associateBy { it.id }

        val camera = requireNotNull(manifests["sm64ds.eu.right-stick-camera"])
        assertEquals(
            EnhancementLifecycleEvent.entries.toSet(),
            camera.runtimeLifecycle,
        )
        assertEquals(
            EnhancementRecenter("R3", true, "recenterSequence"),
            camera.recenter,
        )

        val widescreen = requireNotNull(manifests["sm64ds.eu.widescreen"])
        val sixty = requireNotNull(manifests["sm64ds.eu.60fps"])
        assertTrue(widescreen.requiresCapabilities.isEmpty())
        assertEquals(setOf(EnhancementCapability.RUNTIME_CODE_PATCH), sixty.capabilities)
        assertEquals(setOf("sm64ds.eu.60fps"), widescreen.conflictsWith)
        assertEquals(setOf("sm64ds.eu.widescreen"), sixty.conflictsWith)
        assertEquals(EnhancementDistributionStatus.SOURCE_ONLY, sixty.distributionStatus)
        assertEquals(EnhancementClaim.UNVERIFIED, sixty.verification.cadence)
    }
}
