package me.magnum.enhancements

object ActionReplayParser {
    fun parse(text: String): List<String> {
        return text.lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .map { line ->
                val words = line.split(Regex("\\s+"))
                require(words.isNotEmpty() && words.all { it.matches(Regex("[0-9a-fA-F]{8}")) }) {
                    "Invalid Action Replay code line"
                }
                words.joinToString(" ") { it.uppercase() }
            }
            .toList()
    }
}
