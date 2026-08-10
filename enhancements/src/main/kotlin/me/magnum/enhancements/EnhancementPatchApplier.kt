package me.magnum.enhancements

import java.util.zip.CRC32

object EnhancementPatchApplier {
    fun applyIps(source: ByteArray, patch: ByteArray): ByteArray {
        require(patch.copyOfRange(0, 5).contentEquals("PATCH".encodeToByteArray())) {
            "Invalid IPS header"
        }
        var cursor = 5
        var output = source.copyOf()
        while (cursor + 3 <= patch.size) {
            if (patch.copyOfRange(cursor, cursor + 3).contentEquals("EOF".encodeToByteArray())) {
                return output
            }
            val offset = readU24(patch, cursor)
            val size = readU16(patch, cursor + 3)
            cursor += 5
            if (size == 0) {
                require(cursor + 3 <= patch.size) { "Truncated IPS RLE record" }
                val count = readU16(patch, cursor)
                val value = patch[cursor + 2]
                cursor += 3
                require(count > 0) { "Invalid IPS RLE record" }
                output = output.ensureSize(offset + count)
                output.fill(value, offset, offset + count)
            } else {
                require(cursor + size <= patch.size) { "Truncated IPS record" }
                output = output.ensureSize(offset + size)
                patch.copyInto(output, offset, cursor, cursor + size)
                cursor += size
            }
        }
        error("Missing IPS EOF marker")
    }

    fun applyBps(source: ByteArray, patch: ByteArray): ByteArray {
        require(patch.copyOfRange(0, 4).contentEquals("BPS1".encodeToByteArray())) {
            "Invalid BPS header"
        }
        require(patch.size >= 16) { "Truncated BPS patch" }
        var cursor = 4
        val sourceNumber = readBpsNumber(patch, cursor)
        val sourceSize = sourceNumber.first
        cursor = sourceNumber.second
        val targetNumber = readBpsNumber(patch, cursor)
        val targetSize = targetNumber.first
        cursor = targetNumber.second
        val metadataNumber = readBpsNumber(patch, cursor)
        val metadata = metadataNumber.first
        cursor = metadataNumber.second
        require(source.size == sourceSize) { "BPS source size mismatch" }
        cursor += metadata
        require(cursor <= patch.size - 12) { "Truncated BPS metadata" }

        val output = ByteArray(targetSize)
        var outputCursor = 0
        var sourceRelative = 0
        var targetRelative = 0
        val actionEnd = patch.size - 12
        while (cursor < actionEnd) {
            val actionNumber = readBpsNumber(patch, cursor)
            val action = actionNumber.first
            cursor = actionNumber.second
            val length = (action ushr 2) + 1
            when (action and 3) {
                0 -> {
                    require(outputCursor + length <= output.size) { "BPS target overflow" }
                    source.copyInto(output, outputCursor, outputCursor, outputCursor + length)
                    outputCursor += length
                }
                1 -> {
                    require(cursor + length <= actionEnd) { "Truncated BPS target read" }
                    patch.copyInto(output, outputCursor, cursor, cursor + length)
                    cursor += length
                    outputCursor += length
                }
                2 -> {
                    val sourceOffset = readBpsSigned(patch, cursor)
                    sourceRelative += sourceOffset.first
                    cursor = sourceOffset.second
                    require(sourceRelative >= 0 && sourceRelative + length <= source.size) { "BPS source copy overflow" }
                    source.copyInto(output, outputCursor, sourceRelative, sourceRelative + length)
                    sourceRelative += length
                    outputCursor += length
                }
                3 -> {
                    val targetOffset = readBpsSigned(patch, cursor)
                    targetRelative += targetOffset.first
                    cursor = targetOffset.second
                    require(targetRelative >= 0 && targetRelative + length <= outputCursor) { "BPS target copy overflow" }
                    repeat(length) {
                        output[outputCursor] = output[targetRelative]
                        outputCursor++
                        targetRelative++
                    }
                }
            }
        }
        require(outputCursor == output.size) { "BPS target size mismatch" }
        require(readLe32(patch, patch.size - 12) == crc32(source)) { "BPS source CRC mismatch" }
        require(readLe32(patch, patch.size - 8) == crc32(output)) { "BPS target CRC mismatch" }
        require(readLe32(patch, patch.size - 4) == crc32(patch, 0, patch.size - 4)) { "BPS patch CRC mismatch" }
        return output
    }

    private fun readBpsNumber(bytes: ByteArray, start: Int): Pair<Int, Int> {
        var cursor = start
        var shift = 1
        var result = 0
        while (true) {
            require(cursor < bytes.size) { "Truncated BPS number" }
            val value = bytes[cursor++].toInt() and 0xff
            result += (value and 0x7f) * shift
            if (value and 0x80 != 0) {
                return result to cursor
            }
            shift = shift shl 7
            result += shift
        }
    }

    private fun readBpsSigned(bytes: ByteArray, start: Int): Pair<Int, Int> {
        val value = readBpsNumber(bytes, start)
        val signed = if (value.first and 1 == 0) value.first ushr 1 else -(value.first ushr 1) - 1
        return signed to value.second
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int {
        require(offset + 2 <= bytes.size) { "Truncated patch" }
        return (bytes[offset].toInt() and 0xff) shl 8 or (bytes[offset + 1].toInt() and 0xff)
    }

    private fun readU24(bytes: ByteArray, offset: Int): Int {
        return readU16(bytes, offset) shl 8 or (bytes[offset + 2].toInt() and 0xff)
    }

    private fun readLe32(bytes: ByteArray, offset: Int): Long {
        return (bytes[offset].toLong() and 0xff) or
            ((bytes[offset + 1].toLong() and 0xff) shl 8) or
            ((bytes[offset + 2].toLong() and 0xff) shl 16) or
            ((bytes[offset + 3].toLong() and 0xff) shl 24)
    }

    private fun crc32(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Long {
        return CRC32().apply { update(bytes, offset, length) }.value
    }

    private fun ByteArray.ensureSize(size: Int): ByteArray {
        return if (size <= this.size) this else copyOf(size)
    }
}
