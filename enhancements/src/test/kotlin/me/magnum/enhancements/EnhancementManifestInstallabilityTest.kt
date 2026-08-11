package me.magnum.enhancements

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
