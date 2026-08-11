package me.magnum.enhancements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class EnhancementRuntimeTest {
    private val identity = EnhancementRomIdentity("ASMP", "12345678", "")

    @Test
    fun rejectsMatchingDuplicateAxisOwnersWithoutRuntimeProtocols() {
        val ownerA = manifest("owner.a", protocol = null, axisOwner = true)
        val ownerB = manifest("owner.b", protocol = null, axisOwner = true)
        val catalog = EnhancementCatalog(listOf(ownerA, ownerB))

        try {
            catalog.createSession(identity, setOf(ownerA.id, ownerB.id))
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("controller axis owner"))
            return
        }
        throw AssertionError("Expected duplicate controller axis owners to be rejected")
    }

    @Test
    fun rejectsMatchingDuplicateAxisOwnersWithOneRuntimeProtocol() {
        val ownerA = manifest("owner.a", protocol = "camera-v1")
        val ownerB = manifest("owner.b", protocol = null, axisOwner = true)

        assertRejects("controller axis owner") {
            EnhancementCatalog(listOf(ownerA, ownerB)).createSession(identity, setOf(ownerA.id, ownerB.id))
        }
    }

    @Test
    fun rejectsDuplicateRuntimeGuardAddressWithinAddon() {
        val addOn = manifest(
            id = "patches",
            protocol = null,
            axisOwner = false,
            patches = listOf(runtimePatch("0x02000010", "first.ards"), runtimePatch("0x02000010", "second.ards")),
        )

        assertRejects("guard the same runtime address") {
            EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))
        }
    }

    @Test
    fun rejectsDuplicateRuntimeGuardAddressAcrossAddons() {
        val addOnA = manifest("patch.a", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x02000010")))
        val addOnB = manifest("patch.b", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x02000010")))

        assertRejects("guard the same runtime address") {
            EnhancementCatalog(listOf(addOnA, addOnB)).createSession(identity, setOf(addOnA.id, addOnB.id))
        }
    }

    @Test
    fun rejectsMixedCaseDuplicateRuntimeGuardAddressAcrossAddons() {
        val addOnA = manifest("patch.a", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x020000AF")))
        val addOnB = manifest("patch.b", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x020000af")))

        assertRejects("guard the same runtime address") {
            EnhancementCatalog(listOf(addOnA, addOnB)).createSession(identity, setOf(addOnA.id, addOnB.id))
        }
    }

    @Test
    fun composesDistinctRuntimeGuardAddresses() {
        val addOnA = manifest("patch.a", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x02000010")))
        val addOnB = manifest("patch.b", protocol = null, axisOwner = false, patches = listOf(runtimePatch("0x02000014")))

        val session = EnhancementCatalog(listOf(addOnA, addOnB))
            .createSession(identity, setOf(addOnA.id, addOnB.id))

        assertEquals(listOf(0x02000010L, 0x02000014L), session.runtimeGuards.map { it.address })
    }

    @Test
    fun preservesEmptySession() {
        val session = EnhancementCatalog(emptyList()).createSession(identity, emptySet())

        assertEquals(emptyList<EnhancementManifest>(), session.addOns)
        assertEquals(null, session.runtimeInput)
    }

    @Test
    fun preservesSingleRuntimeProtocolOwnerInput() {
        val owner = manifest(
            id = "owner",
            protocol = "camera-v1",
            axisX = 17,
            axisY = 19,
            invertX = true,
            invertY = false,
            deadzone = 0.23f,
            sensitivity = 1.7f,
        )

        val session = EnhancementCatalog(listOf(owner)).createSession(identity, setOf(owner.id))

        assertEquals(
            EnhancementRuntimeInput(
                capability = RuntimeCapability("camera-v1", 1),
                axisXCode = 17,
                axisYCode = 19,
                invertX = true,
                invertY = false,
                deadzone = 0.23f,
                sensitivity = 1.7f,
            ),
            session.runtimeInput,
        )
        assertFalse(session.runtimeInput == null)
    }

    @Test
    fun runtimeCapabilityRequiresExactReportedMajorVersion() {
        val owner = manifest("owner", protocol = null).copy(
            runtimeCapability = RuntimeCapability("transient-input", 2),
            capabilities = setOf(
                EnhancementCapability.CONTROLLER_AXIS_OWNER,
                EnhancementCapability.RUNTIME_INPUT_PROTOCOL,
            ),
        )
        val catalog = EnhancementCatalog(listOf(owner))
        val identity = this.identity

        assertRejects("missing or unsupported") {
            catalog.createSession(
                identity,
                setOf(owner.id),
                setOf(RuntimeCapability("transient-input", 1)),
            )
        }
        assertEquals(
            RuntimeCapability("transient-input", 2),
            catalog.createSession(
                identity,
                setOf(owner.id),
                setOf(RuntimeCapability("transient-input", 2)),
            ).declaredRuntimeCapability,
        )
    }

    @Test
    fun runtimeInputIsUnavailableWhenReportedCapabilityDoesNotMatchDeclaration() {
        val owner = manifest("owner", protocol = null).copy(
            runtimeCapability = RuntimeCapability("transient-input", 2),
            capabilities = setOf(
                EnhancementCapability.CONTROLLER_AXIS_OWNER,
                EnhancementCapability.RUNTIME_INPUT_PROTOCOL,
            ),
        )
        val session = EnhancementCatalog(listOf(owner)).createSession(identity, setOf(owner.id))

        assertEquals(
            null,
            session.runtimeInputIfSupported(setOf(RuntimeCapability("transient-input", 1))),
        )
        assertEquals(
            session.runtimeInput,
            session.runtimeInputIfSupported(setOf(RuntimeCapability("transient-input", 2))),
        )
    }

    @Test
    fun genericRuntimeSourcesContainNoAddonSpecificAddressLiterals() {
        val roots = listOf(
            File("../app/src/main/java"),
            File("../app/src/main/cpp"),
            File("src/main/kotlin"),
        )
        val sources = roots
            .flatMap { root -> root.walkTopDown().filter { it.isFile }.toList() }
            .filter { it.extension in setOf("kt", "java", "cpp", "h", "cc", "cxx") }
            .joinToString("\n", transform = File::readText)
        val addOnIds = File(".").listFiles()
            ?.filter { it.isDirectory && File(it, "manifest.json").isFile }
            ?.map { it.name }
            .orEmpty()
        val manifests = addOnIds.mapNotNull { id ->
            File("$id/manifest.json").takeIf(File::isFile)?.readText()
        }.joinToString("\n")

        assertFalse(addOnIds.any { id ->
            Regex.escape(id).toRegex(RegexOption.IGNORE_CASE).containsMatchIn(sources)
        })
        val manifestLiterals = Regex("0x[0-9A-Fa-f]{8}").findAll(manifests).map { it.value }.toSet()
        assertFalse(manifestLiterals.any { literal ->
            Regex.escape(literal).toRegex().containsMatchIn(sources)
        })
    }

    @Test
    fun preservesSingleNonRuntimeAddon() {
        val addOn = manifest("plain", protocol = null, axisOwner = false)

        val session = EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))

        assertEquals(listOf(addOn), session.addOns)
        assertEquals(null, session.runtimeInput)
    }

    @Test
    fun rejectsRuntimeProtocolWithoutCapability() {
        val addOn = manifest("invalid", protocol = "camera-v1", axisOwner = true).copy(
            capabilities = setOf(EnhancementCapability.CONTROLLER_AXIS_OWNER),
        )

        assertRejects("Runtime input protocol requires its capability") {
            EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))
        }
    }

    @Test
    fun reportsRequestedAndEffectiveLayerAwarePresentationWhenGuardsPass() {
        val addOn = manifest("widescreen", protocol = null, axisOwner = false).copy(
            capabilities = setOf(EnhancementCapability.LAYER_AWARE_PRESENTATION),
        )
        val session = EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))

        assertEquals(
            EnhancementPresentationState(
                requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                effective = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
            ),
            session.presentationState(
                verifiedGamePatch = true,
                availableNativeCapabilities = setOf(EnhancementCapability.NATIVE_EMULATOR_CAPABILITY),
            ),
        )
    }

    @Test
    fun fallsBackToNative43WhenNativePresentationCapabilityIsMissing() {
        val addOn = manifest("widescreen", protocol = null, axisOwner = false).copy(
            capabilities = setOf(EnhancementCapability.LAYER_AWARE_PRESENTATION),
        )
        val session = EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))

        assertEquals(
            EnhancementPresentationState(
                requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                effective = EnhancementPresentationMode.NATIVE_4_3,
            ),
            session.presentationState(verifiedGamePatch = true, availableNativeCapabilities = emptySet()),
        )
    }

    @Test
    fun fallsBackToNative43WhenGamePatchGuardFails() {
        val addOn = manifest("widescreen", protocol = null, axisOwner = false).copy(
            capabilities = setOf(EnhancementCapability.LAYER_AWARE_PRESENTATION),
        )
        val session = EnhancementCatalog(listOf(addOn)).createSession(identity, setOf(addOn.id))

        assertEquals(
            EnhancementPresentationState(
                requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
                effective = EnhancementPresentationMode.NATIVE_4_3,
            ),
            session.presentationState(
                verifiedGamePatch = false,
                availableNativeCapabilities = setOf(EnhancementCapability.NATIVE_EMULATOR_CAPABILITY),
            ),
        )
    }

    @Test
    fun requestsAndUsesNative43WithoutLayerAwareAddon() {
        val session = EnhancementCatalog(emptyList()).createSession(identity, emptySet())

        val state = session.presentationState(
            verifiedGamePatch = true,
            availableNativeCapabilities = setOf(EnhancementCapability.NATIVE_EMULATOR_CAPABILITY),
        )
        assertEquals(EnhancementPresentationMode.NATIVE_4_3, state.requested)
        assertEquals(EnhancementPresentationMode.NATIVE_4_3, state.effective)
        assertEquals(false, state.requiresNative43Fallback())
    }

    @Test
    fun native43FallbackOnlyAppliesWhenLayerAwareEnhancementIsNotEffective() {
        val fallback = EnhancementPresentationState(
            requested = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
            effective = EnhancementPresentationMode.NATIVE_4_3,
        )
        val effective = fallback.copy(
            effective = EnhancementPresentationMode.LAYER_AWARE_PRESENTATION,
        )
        val native = EnhancementPresentationState(
            requested = EnhancementPresentationMode.NATIVE_4_3,
            effective = EnhancementPresentationMode.NATIVE_4_3,
        )

        assertEquals(true, fallback.requiresNative43Fallback())
        assertEquals(false, effective.requiresNative43Fallback())
        assertEquals(false, native.requiresNative43Fallback())
    }

    @Test
    fun sourceOnlyAddonCannotBeEnabled() {
        val sourceOnly = manifest("source.only", protocol = null, axisOwner = false).copy(
            status = EnhancementStatus.SOURCE_ONLY,
            capabilities = setOf(EnhancementCapability.LAYER_AWARE_PRESENTATION),
        )
        assertThrows(IllegalArgumentException::class.java) {
            EnhancementCatalog(listOf(sourceOnly)).createSession(identity, setOf(sourceOnly.id))
        }
    }

    private fun manifest(
        id: String,
        protocol: String? = "camera-v1",
        axisX: Int = 1,
        axisY: Int = 2,
        invertX: Boolean = false,
        invertY: Boolean = false,
        deadzone: Float = 0.12f,
        sensitivity: Float = 1f,
        axisOwner: Boolean = true,
        patches: List<EnhancementPatch> = emptyList(),
    ) = EnhancementManifest(
        id = id,
        name = id,
        version = "1.0.0",
        match = EnhancementMatch("ASMP", headerChecksum = "12345678"),
        capabilities = buildSet {
            if (axisOwner) add(EnhancementCapability.CONTROLLER_AXIS_OWNER)
            if (protocol != null) add(EnhancementCapability.RUNTIME_INPUT_PROTOCOL)
        },
        patches = patches,
        runtimeProtocol = protocol,
        runtimeAxisXCode = axisX,
        runtimeAxisYCode = axisY,
        runtimeInvertX = invertX,
        runtimeInvertY = invertY,
        runtimeDeadzone = deadzone,
        runtimeSensitivity = sensitivity,
    )

    private fun runtimePatch(address: String, file: String = "$address.ards") = EnhancementPatch(
        type = EnhancementPatchType.ACTION_REPLAY,
        file = file,
        provenance = "test",
        expectedOriginalWords = mapOf(address to "0x00000000"),
    )

    private fun assertRejects(message: String, block: () -> Unit) {
        try {
            block()
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains(message))
            return
        }
        throw AssertionError("Expected IllegalArgumentException containing: $message")
    }
}
