package me.magnum.melonds.impl

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.magnum.enhancements.EnhancementPatchApply
import me.magnum.enhancements.EnhancementSession
import me.magnum.melonds.common.romprocessors.RomFileProcessorFactory
import me.magnum.melonds.domain.model.rom.Rom
import java.io.File
import java.util.UUID
import javax.inject.Inject

class EnhancedRomMaterializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val romFileProcessorFactory: RomFileProcessorFactory,
    private val enhancementCatalogLoader: EnhancementCatalogLoader,
) {
    suspend fun prepare(rom: Rom, session: EnhancementSession): File? = withContext(Dispatchers.IO) {
        if (session.patchPlan.temporaryCopyPatches.isEmpty()) {
            return@withContext null
        }
        val document = requireNotNull(DocumentFile.fromSingleUri(context, rom.uri)) {
            "Unable to resolve ROM document for Enhanced patching"
        }
        val processor = romFileProcessorFactory.getFileRomProcessorForDocument(document)
            ?: error("No ROM processor available for Enhanced patching")
        val realUri = requireNotNull(processor.getRealRomUri(rom)) {
            "Unable to materialize ROM for Enhanced patching"
        }
        val source = context.contentResolver.openInputStream(realUri)?.use { it.readBytes() }
            ?: error("Unable to read ROM for Enhanced patching")
        val patchFiles = session.addOns
            .filter { addOn -> addOn.patches.any { it.apply == EnhancementPatchApply.TEMPORARY_COPY } }
            .flatMap { addOn ->
                enhancementCatalogLoader.readFiles(
                    addOn,
                    addOn.patches
                        .filter { it.apply == EnhancementPatchApply.TEMPORARY_COPY }
                        .map { it.file }
                        .toSet(),
                ).entries.map { it.key to (addOn.id to it.value) }
            }
            .associate { (path, value) -> "${value.first}/$path" to value.second }
        val patched = session.patchPlan.applyTemporaryCopy(source, patchFiles)
        val directory = File(context.externalCacheDir ?: context.cacheDir, "enhanced_roms")
        require(directory.isDirectory || directory.mkdirs()) { "Unable to create Enhanced ROM cache" }
        val output = File(directory, "${UUID.randomUUID()}.nds")
        output.writeBytes(patched)
        output
    }

    fun uri(file: File) = DocumentFile.fromFile(file).uri

    fun cleanup(file: File?) {
        file?.delete()
    }
}
