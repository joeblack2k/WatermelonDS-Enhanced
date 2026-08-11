package me.magnum.enhancements

data class RuntimeInputFrame(
    val yawQ12: Short,
    val pitchQ12: Short,
    val yawUnitsPerTick: Short,
    val recenterSequence: Short,
    val flags: Short,
)

class RuntimeInputProtocol(
    private val deadzone: Float = 0.12f,
    private val yawUnitsPerTick: Short = 850,
) {
    private var recenterSequence: Short = 0

    fun update(rawX: Float, rawY: Float): RuntimeInputFrame {
        return RuntimeInputFrame(
            yawQ12 = q12(applyDeadzone(rawX)),
            pitchQ12 = q12(applyDeadzone(rawY)),
            yawUnitsPerTick = yawUnitsPerTick,
            recenterSequence = recenterSequence,
            flags = 1,
        )
    }

    fun recenter(): RuntimeInputFrame {
        recenterSequence = (recenterSequence + 1).toShort()
        return update(0f, 0f)
    }

    fun neutral(): RuntimeInputFrame {
        return update(0f, 0f).copy(yawUnitsPerTick = 0, flags = 0)
    }

    private fun applyDeadzone(value: Float): Float {
        val magnitude = value.coerceIn(-1f, 1f).let { kotlin.math.abs(it) }
        if (magnitude <= deadzone) return 0f
        return ((magnitude - deadzone) / (1f - deadzone))
            .coerceIn(0f, 1f)
            .let { if (value < 0f) -it else it }
    }

    private fun q12(value: Float): Short {
        return (value * 4096f).toInt().coerceIn(-4096, 4096).toShort()
    }
}
