package me.magnum.enhancements

data class EnhancementOverlayWord(
    val address: Long,
    val value: Long,
    val expectedOriginal: Long,
)

object EnhancementOverlayParser {
    fun parse(text: String, expectedOriginalWords: Map<String, String>): List<EnhancementOverlayWord> {
        return text.lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .map { line ->
                val parts = line.split(Regex("\\s+"))
                require(parts.size == 2 && parts.all { it.matches(Regex("(?:0x)?[0-9a-fA-F]{8}")) }) {
                    "Invalid runtime overlay word"
                }
                val address = parts[0].removePrefix("0x").toLong(16)
                val value = parts[1].removePrefix("0x").toLong(16)
                val expected = expectedOriginalWords.entries.firstOrNull {
                    it.key.removePrefix("0x").toLong(16) == address
                }?.value?.removePrefix("0x")?.toLong(16)
                    ?: error("Runtime overlay word has no expected-original guard")
                EnhancementOverlayWord(address, value, expected)
            }
            .toList()
            .also {
                require(it.isNotEmpty()) { "Runtime overlay is empty" }
                require(it.map(EnhancementOverlayWord::address).distinct().size == it.size) {
                    "Runtime overlay contains duplicate write addresses"
                }
            }
    }
}
