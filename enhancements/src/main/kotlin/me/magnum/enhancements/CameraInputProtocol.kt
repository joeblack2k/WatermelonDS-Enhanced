package me.magnum.enhancements

data class RuntimeInputFrame(
    val axisXQ12: Short,
    val axisYQ12: Short,
    val scalar: Short,
    val actionSequence: Short,
    val flags: Short,
)

class RuntimeTransientInputAdapter(
    private val deadzone: Float = 0.12f,
    private val scalar: Short = 850,
) {
    private var actionSequence: Short = 0

    fun update(rawX: Float, rawY: Float): RuntimeInputFrame {
        return RuntimeInputFrame(
            axisXQ12 = q12(applyDeadzone(rawX)),
            axisYQ12 = q12(applyDeadzone(rawY)),
            scalar = scalar,
            actionSequence = actionSequence,
            flags = 1,
        )
    }

    fun action(): RuntimeInputFrame {
        actionSequence = (actionSequence + 1).toShort()
        return update(0f, 0f)
    }

    fun neutral(): RuntimeInputFrame {
        return update(0f, 0f).copy(scalar = 0, flags = 0)
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
