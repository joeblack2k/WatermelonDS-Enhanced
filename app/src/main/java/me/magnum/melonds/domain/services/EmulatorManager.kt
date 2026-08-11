package me.magnum.melonds.domain.services

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.emulator.EmulatorEvent
import me.magnum.melonds.domain.model.emulator.FirmwareLaunchResult
import me.magnum.melonds.domain.model.emulator.RomLaunchResult
import me.magnum.melonds.domain.model.retroachievements.GameAchievementData
import me.magnum.melonds.domain.model.retroachievements.RAEvent
import me.magnum.melonds.domain.model.retroachievements.RaNativePendingRetryResult
import me.magnum.melonds.domain.model.retroachievements.RARuntimeBridgeConfig
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.enhancements.EnhancementRuntimeGuard
import me.magnum.enhancements.EnhancementOverlayWord
import me.magnum.enhancements.EnhancementActivationRequest
import me.magnum.enhancements.EnhancementActivationResult
import me.magnum.enhancements.RuntimeCapability
import me.magnum.melonds.ui.emulator.rewind.model.RewindSaveState
import me.magnum.melonds.ui.emulator.rewind.model.RewindWindow

interface EmulatorManager {

    val emulatorEvents: Flow<EmulatorEvent>

    suspend fun loadRom(rom: Rom, cheats: List<Cheat>): RomLaunchResult
    fun setEnhancedRuntimeGuards(guards: List<EnhancementRuntimeGuard>)
    fun setEnhancedRuntimeOverlay(words: List<EnhancementOverlayWord>)
    fun activateEnhancedAddOns(requests: List<EnhancementActivationRequest>): EnhancementActivationResult
    fun enhancedRuntimeCapabilities(): Set<RuntimeCapability>

    suspend fun loadFirmware(consoleType: ConsoleType): FirmwareLaunchResult

    suspend fun updateRomEmulatorConfiguration(rom: Rom)

    suspend fun updateFirmwareEmulatorConfiguration(consoleType: ConsoleType)

    suspend fun getRewindWindow(): RewindWindow

    fun getFps(): Float

    suspend fun pauseEmulator()

    suspend fun resumeEmulator()

    suspend fun debugStepFrame(): Boolean

    suspend fun resetEmulator()

    suspend fun updateCheats(cheats: List<Cheat>)
    suspend fun setupRetroAchievements(achievementData: GameAchievementData, runtimeConfig: RARuntimeBridgeConfig?)
    suspend fun retryPendingRetroAchievementsSubmissions(
        expectedNativeSubmissionIds: List<Long>,
    ): RaNativePendingRetryResult
    suspend fun refreshPendingRetroAchievementsSubmissions(): Long
    suspend fun discardPendingRetroAchievementsSubmissions(
        expectedNativeSubmissionIds: List<Long>,
    ): Int
    suspend fun setRetroAchievementsSubmissionTransportSuspended(suspended: Boolean)
    fun unloadRetroAchievementsData()

    suspend fun loadRewindState(rewindSaveState: RewindSaveState): Boolean

    suspend fun saveState(saveStateFileUri: Uri): Boolean

    suspend fun loadState(saveStateFileUri: Uri): Boolean

    suspend fun takeScreenshot(): Boolean

    fun stopEmulator()

    fun cleanEmulator()

    fun observeRetroAchievementEvents(): Flow<RAEvent>
}
