package me.magnum.enhancements

import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementManifestValidationTest {
    @Test
    fun acceptsSupportedPatchApplyPairs() {
        listOf(
            EnhancementPatchType.ACTION_REPLAY to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.RUNTIME_OVERLAY to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.IPS to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.BPS to EnhancementPatchApply.TEMPORARY_COPY,
        ).forEach { (type, apply) ->
            EnhancementManifestParser.validate(manifest(type, apply))
        }
    }

    @Test
    fun rejectsInversePatchApplyPairs() {
        listOf(
            EnhancementPatchType.ACTION_REPLAY to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.RUNTIME_OVERLAY to EnhancementPatchApply.TEMPORARY_COPY,
            EnhancementPatchType.IPS to EnhancementPatchApply.RUNTIME,
            EnhancementPatchType.BPS to EnhancementPatchApply.RUNTIME,
        ).forEach { (type, apply) ->
            try {
                EnhancementManifestParser.validate(manifest(type, apply))
                throw AssertionError("Expected ${type}/${apply} to be rejected")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains(type.name))
            }
        }
    }

    private fun manifest(type: EnhancementPatchType, apply: EnhancementPatchApply) =
        EnhancementManifest(
            id = "test.addon",
            name = "Test",
            version = "1.0.0",
            match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
            patches = listOf(
                EnhancementPatch(
                    type = type,
                    file = "payload/patch",
                    apply = apply,
                    provenance = if (type == EnhancementPatchType.ACTION_REPLAY ||
                        type == EnhancementPatchType.RUNTIME_OVERLAY
                    ) "test" else "",
                ),
            ),
        )
}
