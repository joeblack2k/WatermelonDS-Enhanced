package me.magnum.enhancements

import java.util.zip.CRC32

object EnhancementPatchApplier {
    fun validateIps(patch: ByteArray) {
        require(patch.size >= 8 && patch.copyOfRange(0, 5).contentEquals("PATCH".encodeToByteArray())) {
            "Invalid IPS header"
        }
        var cursor = 5
        var records = 0
        while (cursor <= patch.size - 3) {
            require(++records <= MAX_PATCH_RECORDS) { "Too many IPS records" }
            if (patch[cursor] == 'E'.code.toByte() && patch[cursor + 1] == 'O'.code.toByte() &&
                patch[cursor + 2] == 'F'.code.toByte()) {
                require(cursor + 3 == patch.size || cursor + 6 == patch.size) { "Trailing IPS data" }
                if (cursor + 6 == patch.size) {
                    require(readU24(patch, cursor + 3) <= MAX_ROM_SIZE) { "IPS final size is too large" }
                }
                return
            }
            require(cursor <= patch.size - 5) { "Truncated IPS record" }
            val offset = readU24(patch, cursor)
            val size = readU16(patch, cursor + 3)
            cursor += 5
            if (size == 0) {
                require(cursor <= patch.size - 3) { "Truncated IPS RLE record" }
                val count = readU16(patch, cursor)
                cursor += 3
                require(count > 0) { "Invalid IPS RLE record" }
                checkedEnd(offset.toLong(), count.toLong(), MAX_ROM_SIZE)
            } else {
                require(cursor + size <= patch.size) { "Truncated IPS record" }
                checkedEnd(offset.toLong(), size.toLong(), MAX_ROM_SIZE)
                cursor += size
            }
        }
        throw IllegalArgumentException("Missing IPS EOF marker")
    }

    fun validateBps(patch: ByteArray) {
        require(patch.size >= 16 && patch.copyOfRange(0, 4).contentEquals("BPS1".encodeToByteArray())) {
            "Invalid BPS patch"
        }
        var cursor = 4
        cursor = readBpsNumber(patch, cursor).second
        val target = readBpsNumber(patch, cursor)
        cursor = target.second
        require(target.first <= MAX_ROM_SIZE) { "BPS target is too large" }
        val metadata = readBpsNumber(patch, cursor)
        cursor = metadata.second
        require(metadata.first <= actionEnd(patch) - cursor) { "Truncated BPS metadata" }
        cursor += metadata.first
        while (cursor < actionEnd(patch)) {
            val action = readBpsNumber(patch, cursor)
            cursor = action.second
            val length = checkedLength(action.first)
            when (action.first and 3) {
                0 -> Unit
                1 -> {
                    require(cursor + length <= actionEnd(patch)) { "Truncated BPS target read" }
                    cursor += length
                }
                2, 3 -> cursor = readBpsNumber(patch, cursor).second
            }
        }
        require(cursor == actionEnd(patch)) { "Invalid BPS action/footer boundary" }
        require(readLe32(patch, patch.size - 4) == crc32(patch, 0, patch.size - 4)) {
            "BPS patch CRC mismatch"
        }
    }

    fun applyIps(source: ByteArray, patch: ByteArray): ByteArray {
        require(patch.size >= 8 && patch.copyOfRange(0, 5).contentEquals("PATCH".encodeToByteArray())) {
            "Invalid IPS header"
        }
        var cursor = 5
        var output = source.copyOf()
        var records = 0
        while (cursor <= patch.size - 3) {
            require(++records <= MAX_PATCH_RECORDS) { "Too many IPS records" }
            if (patch[cursor] == 'E'.code.toByte() && patch[cursor + 1] == 'O'.code.toByte() &&
                patch[cursor + 2] == 'F'.code.toByte()) {
                require(cursor + 3 == patch.size || cursor + 6 == patch.size) { "Trailing IPS data" }
                if (cursor + 6 == patch.size) {
                    val finalSize = readU24(patch, cursor + 3)
                    require(finalSize <= MAX_ROM_SIZE) { "IPS final size is too large" }
                    return output.copyOf(finalSize)
                }
                return output
            }
            require(cursor <= patch.size - 5) { "Truncated IPS record" }
            val offset = readU24(patch, cursor)
            val size = readU16(patch, cursor + 3)
            cursor += 5
            if (size == 0) {
                require(cursor <= patch.size - 3) { "Truncated IPS RLE record" }
                val count = readU16(patch, cursor)
                val value = patch[cursor + 2]
                cursor += 3
                require(count > 0) { "Invalid IPS RLE record" }
                val end = checkedEnd(offset.toLong(), count.toLong(), MAX_ROM_SIZE)
                output = output.ensureSize(end)
                output.fill(value, offset, end)
            } else {
                require(cursor + size <= patch.size) { "Truncated IPS record" }
                val end = checkedEnd(offset.toLong(), size.toLong(), MAX_ROM_SIZE)
                output = output.ensureSize(end)
                patch.copyInto(output, offset, cursor, cursor + size)
                cursor += size
            }
        }
        throw IllegalArgumentException("Missing IPS EOF marker")
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
        require(metadata <= actionEnd(patch) - cursor) { "Truncated BPS metadata" }
        cursor += metadata

        val output = ByteArray(targetSize)
        var outputCursor = 0
        var sourceRelative = 0
        var targetRelative = 0
        val actionEnd = patch.size - 12
        while (cursor < actionEnd) {
            val actionNumber = readBpsNumber(patch, cursor)
            val action = actionNumber.first
            cursor = actionNumber.second
            val length = checkedLength(action)
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
                    sourceRelative = checkedRelative(sourceRelative, sourceOffset.first)
                    cursor = sourceOffset.second
                    require(sourceRelative >= 0 && sourceRelative + length <= source.size) { "BPS source copy overflow" }
                    source.copyInto(output, outputCursor, sourceRelative, sourceRelative + length)
                    sourceRelative += length
                    outputCursor += length
                }
                3 -> {
                    val targetOffset = readBpsSigned(patch, cursor)
                    targetRelative = checkedRelative(targetRelative, targetOffset.first)
                    cursor = targetOffset.second
                    require(targetRelative >= 0 && targetRelative < outputCursor) { "BPS target copy overflow" }
                    repeat(length) {
                        require(targetRelative < outputCursor) { "BPS target copy overflow" }
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
        var shift = 1L
        var result = 0L
        while (true) {
            require(cursor < bytes.size) { "Truncated BPS number" }
            val value = bytes[cursor++].toInt() and 0xff
            require(result <= Int.MAX_VALUE) { "Oversized BPS number" }
            result += (value and 0x7f) * shift
            if (value and 0x80 != 0) {
                require(result in 0..MAX_ROM_SIZE.toLong()) { "Oversized BPS number" }
                return result.toInt() to cursor
            }
            require(shift <= Int.MAX_VALUE.toLong() / 128) { "Oversized BPS varint" }
            shift = shift shl 7
            require(result <= Int.MAX_VALUE - shift) { "Oversized BPS varint" }
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
        require(size <= MAX_ROM_SIZE) { "Patched ROM is too large" }
        return if (size <= this.size) this else copyOf(size)
    }

    private fun checkedEnd(start: Long, length: Long, limit: Int): Int {
        val end = start + length
        require(start >= 0 && end >= start && end <= limit) { "Patch target exceeds size limit" }
        return end.toInt()
    }

    private fun checkedLength(action: Int): Int {
        val length = (action.toLong() ushr 2) + 1
        require(length <= Int.MAX_VALUE && length <= MAX_ROM_SIZE) { "BPS action is too large" }
        return length.toInt()
    }

    private fun checkedRelative(current: Int, delta: Int): Int {
        val result = current.toLong() + delta
        require(result in 0..Int.MAX_VALUE.toLong()) { "BPS relative offset overflow" }
        return result.toInt()
    }

    private fun actionEnd(bytes: ByteArray): Int = bytes.size - 12

    private const val MAX_ROM_SIZE = 256 * 1024 * 1024
    private const val MAX_PATCH_RECORDS = 1_000_000
}
