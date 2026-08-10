package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancementOverlayParserTest {
    @Test
    fun parsesGuardedOverlayWords() {
        val words = EnhancementOverlayParser.parse(
            "0x02000000 0xE3A00000\n",
            mapOf("0x02000000" to "0xE1A00000"),
        )

        assertEquals(0x02000000L, words.single().address)
        assertEquals(0xE1A00000L, words.single().expectedOriginal)
    }
}
