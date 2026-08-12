package me.magnum.melonds.impl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import me.magnum.enhancements.EnhancementCatalog
import me.magnum.enhancements.EnhancementCapability
import me.magnum.enhancements.EnhancementManifestParser
import me.magnum.enhancements.isInstallable
import me.magnum.enhancements.EnhancementPackageInstaller
import kotlin.coroutines.CoroutineContext
import java.security.MessageDigest

class EnhancementCatalogLoaderTest {
    @Test
    fun exceptionAfterStagingCleansUntransferredArtifact() = runTest {
        val root = Files.createTempDirectory("loader-owner").toFile()
        try {
            val staged = EnhancementPackageInstaller(root).stage(ByteArrayInputStream(packageBytes()))
            val owner = StagedEnhancementOwner()
            launch {
                try {
                    withContextIo(Dispatchers.IO) { owner.adopt(staged) }
                    error("catalog lookup failed")
                } catch (_: IllegalStateException) {
                    // expected
                } finally {
                    owner.close()
                }
            }.join()
        } finally {
            assertFalse(root.listFiles()?.any { it.name.startsWith(".staged-") } == true)
            root.deleteRecursively()
        }
    }

    @Test
    fun cancellationAtIoToMainHandoffCleansUntransferredArtifact() = runTest {
        val root = Files.createTempDirectory("loader-cancel").toFile()
        try {
            val staged = EnhancementPackageInstaller(root).stage(ByteArrayInputStream(packageBytes()))
            val owner = StagedEnhancementOwner()
            val returnedToMain = CompletableDeferred<Unit>()
            val io = QueuedDispatcher()
            val main = QueuedDispatcher()
            val job = launch(main) {
                try {
                    withContextIo(io) {
                        owner.adopt(staged)
                        returnedToMain.complete(Unit)
                    }
                    owner.transfer()
                } finally {
                    owner.close()
                }
            }
            main.runNext()
            io.runAll()
            returnedToMain.await()
            job.cancel()
            main.runAll()
            job.join()
        } finally {
            assertFalse(root.listFiles()?.any { it.name.startsWith(".staged-") } == true)
            root.deleteRecursively()
        }
    }

    @Test
    fun stagingFailureLeavesNoResidue() {
        val root = Files.createTempDirectory("loader-failure").toFile()
        try {
            EnhancementPackageInstaller(root).stage(ByteArrayInputStream("not a zip".toByteArray()))
        } catch (_: Exception) {
            // expected
        }
        assertFalse(root.listFiles()?.any { it.name.startsWith(".staged-") } == true)
        root.deleteRecursively()
    }

    @Test
    fun missingOrMismatchedInstalledPatchDoesNotHideValidBundledSameIdDescriptor() {
        val root = Files.createTempDirectory("loader-catalog").toFile()
        try {
            val validBundled = EnhancementManifestParser.parse(
                """{"schemaVersion":3,"id":"sm64ds.eu.widescreen","name":"Bundled","version":"2",
                    "distributionStatus":"SOURCE_ONLY","match":{"gameCode":"ASMP",
                    "raHashes":["ba3c4052e00c5cc31df5d5534c39de1b"]},
                    "capabilities":["${EnhancementCapability.LAYER_AWARE_PRESENTATION}"]}"""
                    .replace(Regex("\\s+"), " "),
            )
            listOf("missing.ards" to false, "mismatch.ards" to true).forEach { (patch, present) ->
                val invalid = File(root, "sm64ds.eu.widescreen").also { it.mkdirs() }
                File(invalid, "manifest.json").writeText(
                        """{"schemaVersion":3,"id":"sm64ds.eu.widescreen","name":"Invalid","version":"1",
                            "distributionStatus":"INSTALLABLE","match":{"gameCode":"ASMP"},
                            "capabilities":["RUNTIME_CODE_PATCH"],"patches":[{"type":"ACTION_REPLAY",
                            "file":"$patch","provenance":"test","apply":"RUNTIME",
                            "sha256":"${"00".repeat(32)}"}]}"""
                        .replace(Regex("\\s+"), " "),
                )
                if (present) File(invalid, patch).writeText("tampered")

                val merged = EnhancementCatalogLoader.loadInstalled(listOf(root))
                    .mergeBundled(EnhancementCatalog(listOf(validBundled)))
                assertEquals("Bundled", merged.find("sm64ds.eu.widescreen")?.name)
                assertTrue(merged.find("sm64ds.eu.widescreen")?.isInstallable() == false)
                invalid.deleteRecursively()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun validHashedInstalledPackageOverridesBundledSameIdDescriptor() {
        val root = Files.createTempDirectory("loader-hash").toFile()
        try {
            val packageRoot = File(root, "sm64ds.eu.widescreen").also { it.mkdirs() }
            val patch = byteArrayOf(1)
            File(packageRoot, "patch.ards").writeBytes(patch)
            val hash = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a"
            File(packageRoot, "manifest.json").writeText(
                """{"schemaVersion":3,"id":"sm64ds.eu.widescreen","name":"Installed","version":"3",
                    "distributionStatus":"INSTALLABLE","match":{"gameCode":"ASMP","headerChecksum":"12345678"},
                    "capabilities":["RUNTIME_CODE_PATCH"],"patches":[{"type":"ACTION_REPLAY",
                    "file":"patch.ards","provenance":"test","apply":"RUNTIME","sha256":"$hash"}]}"""
                    .replace(Regex("\\s+"), " "),
            )
            val bundled = EnhancementManifestParser.parse(
                """{"schemaVersion":3,"id":"sm64ds.eu.widescreen","name":"Bundled","version":"2",
                    "distributionStatus":"SOURCE_ONLY","match":{"gameCode":"ASMP","headerChecksum":"12345678"}}"""
                    .replace(Regex("\\s+"), " "),
            )

            val merged = EnhancementCatalogLoader.loadInstalled(listOf(root))
                .mergeBundled(EnhancementCatalog(listOf(bundled)))

            assertEquals("Installed", merged.find("sm64ds.eu.widescreen")?.name)
            assertTrue(merged.find("sm64ds.eu.widescreen")?.isInstallable() == true)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun invalidInternalPackageDoesNotWinSameIdFileResolution() {
        val root = Files.createTempDirectory("loader-roots").toFile()
        try {
            val internal = File(root, "internal/Enhancements/same.id").also { it.mkdirs() }
            File(internal, "manifest.json").writeText("""{"schemaVersion":3,"id":"same.id","name":"Valid",
                "version":"1","distributionStatus":"INSTALLABLE","match":{"gameCode":"ASMP",
                "raHashes":["ba3c4052e00c5cc31df5d5534c39de1b"]},
                "patches":[{"type":"ACTION_REPLAY","file":"patch.ards","provenance":"test",
                "apply":"RUNTIME","sha256":"${"00".repeat(32)}"}]}""".replace(Regex("\\s+"), " "))
            File(internal, "patch.ards").writeText("tampered")

            val external = File(root, "external/Enhancements/same.id").also { it.mkdirs() }
            val contents = "valid".toByteArray()
            File(external, "patch.ards").writeBytes(contents)
            File(external, "manifest.json").writeText("""{"schemaVersion":3,"id":"same.id","name":"Valid",
                "version":"1","distributionStatus":"INSTALLABLE","match":{"gameCode":"ASMP",
                "raHashes":["ba3c4052e00c5cc31df5d5534c39de1b"]},
                "patches":[]}""".replace(Regex("\\s+"), " "))
            val manifest = EnhancementManifestParser.parse("""{"schemaVersion":3,"id":"same.id","name":"Valid",
                "version":"1","distributionStatus":"INSTALLABLE","match":{"gameCode":"ASMP",
                "raHashes":["ba3c4052e00c5cc31df5d5534c39de1b"]},
                "capabilities":["RUNTIME_CODE_PATCH"],
                "patches":[]}""".replace(Regex("\\s+"), " "))

            val accepted = EnhancementCatalogLoader.acceptedPackageRoot(
                listOf(File(root, "internal/Enhancements"), File(root, "external/Enhancements")),
                manifest,
            ).also { requireNotNull(it) }

            assertEquals(external.canonicalFile, accepted?.canonicalFile)
            assertEquals("valid", File(requireNotNull(accepted), "patch.ards").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun packageBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("bundle/manifest.json"))
            zip.write(
                """{"id":"owner.addon","name":"Owner","version":"1.0.0",
                    "match":{"gameCode":"ASMP","headerChecksum":"12345678"}}"""
                    .replace(Regex("\\s+"), " ")
                    .toByteArray(),
            )
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private suspend fun <T> withContextIo(dispatcher: CoroutineDispatcher, block: () -> T): T =
        kotlinx.coroutines.withContext(dispatcher) { block() }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.addLast(block)
        }

        fun runNext() {
            queue.removeFirst().run()
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }
}
