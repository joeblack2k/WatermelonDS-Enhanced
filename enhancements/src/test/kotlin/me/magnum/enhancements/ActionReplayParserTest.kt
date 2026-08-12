package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ActionReplayParserTest {
    @Test
    fun parsesOneNormalizedProgramAndPreservesLineBoundaries() {
        assertEquals(
            "02000000 00000001\nD0000000 00000000",
            ActionReplayParser.parse(
                """
                # generated payload
                02000000 00000001
                D0000000 00000000
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesMultilineProgramAsOnePatchPayload() {
        assertEquals(
            "02000000 00000001\nD0000000 00000000",
            ActionReplayParser.parse("02000000\t00000001\r\nD0000000 00000000"),
        )
    }

    @Test
    fun rejectsEmptyPayload() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionReplayParser.parse("# comments only")
        }
    }

    @Test
    fun rejectsOddWordCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionReplayParser.parse("02000000")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionReplayParser.parse("02000000 00000001 D0000000")
        }
    }
}
