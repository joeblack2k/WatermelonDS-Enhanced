package me.magnum.melonds.ui.emulator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TransientInputLifecycleContractTest {
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

        fun body(source: String, signature: String): String {
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
}
