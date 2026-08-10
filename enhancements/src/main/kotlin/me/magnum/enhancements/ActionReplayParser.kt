package me.magnum.enhancements

object ActionReplayParser {
    fun parse(text: String): String {
        val lines = text.lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .map { line ->
                val words = line.split(Regex("\\s+"))
                require(words.isNotEmpty() && words.size % 2 == 0 &&
                    words.all { it.matches(Regex("[0-9a-fA-F]{8}")) }) {
                    "Invalid Action Replay code line"
                }
                words.joinToString(" ") { it.uppercase() }
            }
            .toList()
        require(lines.isNotEmpty()) { "Action Replay payload is empty" }
        return lines.joinToString("\n")
    }
}
