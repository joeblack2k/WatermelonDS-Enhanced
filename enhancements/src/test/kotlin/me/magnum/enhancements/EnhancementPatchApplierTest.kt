package me.magnum.enhancements

import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EnhancementPatchApplierTest {
    @Test
    fun appliesIpsLiteralAndRleRecords() {
        val patch = byteArrayOf(
            *"PATCH".encodeToByteArray(),
            0, 0, 1, 0, 2, 9, 8,
            0, 0, 4, 0, 0, 0, 2, 7,
            *"EOF".encodeToByteArray(),
        )

        assertArrayEquals(byteArrayOf(0, 9, 8, 0, 7, 7), EnhancementPatchApplier.applyIps(ByteArray(2), patch))
    }

    @Test
    fun appliesBpsTargetReadAndChecksCrcs() {
        val source = byteArrayOf(1, 2, 3)
        val body = byteArrayOf(
            *"BPS1".encodeToByteArray(),
            0x83.toByte(), 0x84.toByte(), 0x80.toByte(),
            0x8D.toByte(), 4, 5, 6, 7,
        )
        val target = byteArrayOf(4, 5, 6, 7)
        val patch = body + le32(crc32(source)) + le32(crc32(target))
        val completePatch = patch + le32(crc32(patch))

        assertArrayEquals(target, EnhancementPatchApplier.applyBps(source, completePatch))
    }

    private fun crc32(bytes: ByteArray): Long {
        return CRC32().apply { update(bytes) }.value
    }

    private fun le32(value: Long): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte(),
        )
    }
}
