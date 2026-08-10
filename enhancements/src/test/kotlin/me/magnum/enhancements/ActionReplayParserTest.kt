package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionReplayParserTest {
    @Test
    fun parsesNormalizedCodeLinesAndComments() {
        assertEquals(
            listOf("02000000 00000001", "D0000000 00000000"),
            ActionReplayParser.parse(
                """
                # generated payload
                02000000 00000001
                D0000000 00000000
                """.trimIndent(),
            ),
        )
    }
}
