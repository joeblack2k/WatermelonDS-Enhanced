package me.magnum.melonds.impl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementRomIdentity
import me.magnum.melonds.common.romprocessors.RomFileProcessorFactory
import me.magnum.melonds.domain.model.rom.Rom
import java.security.MessageDigest
import javax.inject.Inject

class EnhancementRomIdentityResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val romFileProcessorFactory: RomFileProcessorFactory,
) {
    suspend fun resolve(rom: Rom, catalog: EnhancementCatalog): EnhancementRomIdentity? =
        withContext(Dispatchers.IO) {
            val processor = romFileProcessorFactory.getFileRomProcessorForDocument(rom.uri) ?: return@withContext null
            val info = processor.getRomInfo(rom) ?: return@withContext null
            val header = info.headerChecksumString()
            val sha = if (catalog.hasShaGuard(info.gameCode, header)) {
                val uri = processor.getRealRomUri(rom) ?: return@withContext null
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                } ?: return@withContext null
                digest.digest().joinToString("") { "%02x".format(it) }
            } else {
                ""
            }
            EnhancementRomIdentity(
                info.gameCode,
                header,
                sha,
                revision = info.revision,
                raHash = rom.retroAchievementsHash,
            )
        }
}
