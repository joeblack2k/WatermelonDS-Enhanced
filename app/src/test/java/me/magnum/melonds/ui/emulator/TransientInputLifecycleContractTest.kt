package me.magnum.melonds.ui.emulator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransientInputLifecycleContractTest {
    private fun body(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing function: $signature", start >= 0)
        val opening = source.indexOf('{', start)
        assertTrue("Missing body: $signature", opening >= 0)
        var depth = 0
        for (index in opening until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(opening, index + 1)
                }
            }
        }
        throw AssertionError("Unclosed body: $signature")
    }

    @Test
    fun everyTransientInputLifecycleBoundaryReachesGenericNeutralizer() {
        val root = File("src/main")
        val viewModel = File(root, "java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt").readText()
        val manager = File(root, "java/me/magnum/melonds/impl/emulator/AndroidEmulatorManager.kt").readText()
        val emulator = File(root, "java/me/magnum/melonds/MelonEmulator.kt").readText()
        val jni = File(root, "cpp/MelonDSAndroidJNI.cpp").readText()
        val native = File(root, "cpp/MelonDS.cpp").readText()
        val instance = File(root, "cpp/MelonInstance.cpp").readText()
        val activity = File(root, "java/me/magnum/melonds/ui/emulator/EmulatorActivity.kt").readText()

        assertTrue(viewModel.substringAfter("private fun resetEmulatorState")
            .substringBefore("private fun currentSessionIsActive")
            .contains("emulatorManager.clearTransientInputState()"))
        assertTrue(viewModel.substringAfter("override fun onCleared()")
            .substringBefore("private class EmulatorSessionCoroutineScope")
            .contains("emulatorManager.clearTransientInputState()"))
        assertTrue(manager.substringAfter("override fun cleanEmulator()")
            .substringBefore("override fun observeRetroAchievementEvents")
            .startsWith(" {\n        clearTransientInputState()"))
        assertTrue(emulator.contains("external fun clearTransientInputState()"))
        assertTrue(jni.contains("Java_me_magnum_melonds_MelonEmulator_clearTransientInputState"))
        assertTrue(native.contains("void clearTransientInputState()"))
        assertTrue(native.contains("void pause()"))
        assertTrue(native.contains("if (instance)\n            instance->clearTransientInputState();"))
        assertTrue(body(native, "void pause()").contains("instance->clearTransientInputState()"))

        assertTrue(body(instance, "void MelonInstance::reset()").contains("clearTransientInputState()"))
        assertTrue(body(instance, "void MelonInstance::stop()").contains("clearTransientInputState()"))
        assertTrue(body(instance, "bool MelonInstance::loadState(").contains("clearTransientInputState()"))
        assertTrue(body(instance, "bool MelonInstance::loadRewindState(").contains("loadState("))
        val clearBody = body(instance, "void MelonInstance::clearTransientInputState()")
        assertTrue(clearBody.contains("setSlot2AnalogInput(0.0f, 0.0f)"))
        assertTrue(clearBody.contains("setRuntimeTransientInputFrame(0, 0, 0"))
        assertTrue(native.contains("instance->clearTransientInputState()"))
        val controllerRemovalBody = body(activity, "connectedControllerManager.onControllerRemoved = {")
        assertTrue(controllerRemovalBody.contains("nativeInputListener.neutralizeTransientInputs()"))
        val inputSetupBody = body(activity, "private fun setupInputHandling(")
        val neutralizeIndex = inputSetupBody.indexOf("nativeInputListener.neutralizeTransientInputs()")
        val replacementIndex = inputSetupBody.indexOf("nativeInputListener = InputProcessor(")
        assertTrue(neutralizeIndex >= 0)
        assertTrue(replacementIndex > neutralizeIndex)
    }

    @Test
    fun slot2MotionContractKeepsMappingOwnershipAndLifecycleGuards() {
        val processor = File(
            "src/main/java/me/magnum/melonds/ui/emulator/input/InputProcessor.kt",
        ).readText()
        val activity = File(
            "src/main/java/me/magnum/melonds/ui/emulator/EmulatorActivity.kt",
        ).readText()
        val mapping = File(
            "src/main/java/me/magnum/melonds/domain/model/Slot2AnalogMapping.kt",
        ).readText()

        val slot2Body = body(processor, "private fun processSlot2AnalogFromMotionEvent")
        val motionBody = body(processor, "override fun onMotionEvent")
        val neutralizeBody = body(processor, "override fun neutralizeTransientInputs")
        val dispatchBody = body(activity, "override fun dispatchGenericMotionEvent")
        val sourceGuard = body(processor, "private fun isControllerMotionEvent")

        assertTrue(sourceGuard.contains("SOURCE_CLASS_JOYSTICK"))
        assertTrue(sourceGuard.contains("SOURCE_JOYSTICK"))
        assertTrue(sourceGuard.contains("SOURCE_GAMEPAD"))
        assertTrue(slot2Body.contains("slot2Mapping.axisXCode"))
        assertTrue(slot2Body.contains("slot2Mapping.axisYCode"))
        assertTrue(slot2Body.contains("slot2Mapping.effectiveDeviceId()"))
        assertTrue(slot2Body.contains("mappedDeviceId != motionEvent.deviceId && mappedDeviceConnected"))
        assertTrue(slot2Body.contains("mappedDeviceId == null || InputDevice.getDevice(mappedDeviceId) != null"))
        assertTrue(slot2Body.contains("slot2Mapping.invertX"))
        assertTrue(slot2Body.contains("slot2Mapping.invertY"))
        assertTrue(slot2Body.contains("slot2Mapping.normalizedDeadzone()"))
        assertTrue(slot2Body.contains("setSlot2AnalogInput(analogX, analogY)"))

        assertTrue(motionBody.contains("processSlot2AnalogFromMotionEvent(motionEvent)"))
        assertTrue(motionBody.contains("if (transientHandled &&"))
        assertTrue(motionBody.contains("axis.axisCode == runtimeInput?.axisXCode"))
        assertTrue(motionBody.contains("axis.axisCode == runtimeInput?.axisYCode"))
        assertTrue(motionBody.contains("axisState.value = 0f"))
        assertTrue(motionBody.contains("axisState.active = false"))

        assertTrue(dispatchBody.contains("nativeInputListener.onMotionEventSlot2(event)"))
        assertTrue(dispatchBody.contains("nativeInputListener.onMotionEvent(event)"))
        assertTrue(neutralizeBody.contains("setSlot2AnalogInput(0f, 0f)"))
        assertTrue(neutralizeBody.contains("sendTransientInput(requireNotNull(transientInputAdapter).neutral())"))

        assertTrue(mapping.contains("val axisXCode: Int"))
        assertTrue(mapping.contains("val axisYCode: Int"))
        assertTrue(mapping.contains("fun effectiveDeviceId(): Int?"))
        assertTrue(mapping.contains("return if (useDeviceFilter) deviceId else null"))
    }
}
