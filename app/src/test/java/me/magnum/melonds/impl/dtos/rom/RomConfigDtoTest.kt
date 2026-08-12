package me.magnum.melonds.impl.dtos.rom

import com.google.gson.Gson
import me.magnum.melonds.domain.model.rom.config.RomConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RomConfigDtoTest {
    private val gson = Gson()

    @Test
    fun legacyJsonWithoutEnabledEnhancementsDefaultsToEmptySet() {
        val json = gson.toJson(RomConfigDto.fromModel(RomConfig())).let {
            gson.fromJson(it, Map::class.java).toMutableMap().apply {
                remove("enabledEnhancements")
            }
        }.let(gson::toJson)

        val model = gson.fromJson(json, RomConfigDto::class.java).toModel()

        assertEquals(emptySet<String>(), model.enabledEnhancements)
    }

    @Test
    fun enabledEnhancementsSurviveModelDtoJsonRoundTrip() {
        val original = RomConfig(enabledEnhancements = setOf("camera", "60fps"))

        val decoded = gson.fromJson(
            gson.toJson(RomConfigDto.fromModel(original)),
            RomConfigDto::class.java,
        ).toModel()

        assertEquals(original.enabledEnhancements, decoded.enabledEnhancements)
    }
}
