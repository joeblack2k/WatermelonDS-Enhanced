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
import org.junit.Test
import me.magnum.enhancements.EnhancementPackageInstaller
import kotlin.coroutines.CoroutineContext

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
