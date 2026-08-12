# WatermelonDS Enhanced acceptance matrix

Scope: the full Enhanced fork objective in
[`ENHANCED_FORK_MIGRATION_GOAL_PROMPT.md`](ENHANCED_FORK_MIGRATION_GOAL_PROMPT.md)
and the attached objective wording. This is
repository evidence only;
it is not a release or gameplay claim. Status vocabulary is intentionally
limited to `PASS`, `SOURCE_ONLY`, `UNVERIFIED`, and
`BLOCKED-EXTERNAL-INPUT`. Terra is not used.

| Required gate | Current repository evidence | Status |
| --- | --- | --- |
| Fork, remotes, upstream base | `git remote -v`; `docs/FORK_MAINTENANCE.md`; `origin` is `joeblack2k/WatermelonDS-Enhanced`, `upstream` is `SapphireRhodonite/WatermelonDS`, base `Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9`. | PASS |
| Review branch / clean upstream diff | `git branch -vv`; `codex/watermelon-enhanced-pr` tracks `origin`; PR/reference is documented in `README.md` and `docs/FORK_MAINTENANCE.md`. A fresh clean diff from the current upstream base is not recorded here. | UNVERIFIED |
| Modular package model | `enhancements/`; `EnhancementManifest.kt`; `EnhancementCatalogLoader`; `EnhancementPackageInstaller`; three `manifest.json` and `README.md` packages. | PASS |
| Identity and fail-closed matching | `EnhancementManifest.kt` (`EnhancementRomIdentity`, `matches`); `EnhancementManifestValidationTest.kt`; `enhancements/tools/test_rom_identity.py`; no filename-only match. | PASS |
| Install safety | `EnhancementPackageInstaller.kt`; `EnhancementPackageInstallerTest.kt`; `EnhancementManifestInstallabilityTest.kt`; duplicate IDs, unsafe paths, malformed metadata and patch declarations are rejected. | PASS |
| Action Replay backend | `EnhancementPatchApplier.kt`; `ActionReplayParserTest.kt`; SM64DS manifests declare AR inputs. Payload correctness remains separately gated. | PASS |
| IPS/BPS backends | `EnhancementPatchApplier.kt`; `EnhancementPatchApplierTest.kt`; manifest validation requires `TEMPORARY_COPY` for IPS/BPS. | PASS |
| Guarded runtime/overlay backend | `EnhancementManifest.kt` guard validation; `EnhancementOverlayParserTest.kt`; `EnhancementRuntimeTest.kt`; package overlays are declared, but required legal runtime inputs are not present. | SOURCE_ONLY |
| Temporary-copy and original-ROM safety | `EnhancementPatchApplier.kt`; `EnhancementPackageInstallerTest.kt`; `EnhancementPatchPlanTest.kt`; no tracked ROM data. | PASS |
| Explicit axis ownership | `EnhancementManifest.kt` runtime axis fields; `CameraInputProtocolTest.kt`; `Slot2AnalogMappingTest.kt`; `InputSetupActivity.dispatchGenericMotionEvent`. | PASS |
| Lifecycle neutralization | `EnhancementManifest.kt` lifecycle contract; `TransientInputLifecycleContractTest.kt`; `EnhancementRuntimeTest.kt`; pause/reset/save-state load/disconnect/activity replacement/stop/teardown are covered structurally. | PASS |
| Per-ROM persistence and backward compatibility | `RomConfig.kt`, `RomConfigDtoTest.kt`, `EnhancementActivationTest.kt`; serialization/default behavior is tested. Game-side persistent camera consumer is not proven. | PASS |
| Effective capabilities | `EnhancementRuntimeTest.kt`; `EnhancementActivationTest.kt`; effective capability and active-session decisions are represented in source/tests. | PASS |
| RA Casual | `EnhancementManifest.kt` compatibility policy; `EnhancementActivationTest.kt`; Casual is allowed by the package policy. No server unlock is claimed. | PASS |
| RA Hardcore | `EnhancementRuntimeTest.kt`; `EnhancementActivationTest.kt`; incompatible active enhancements block Hardcore locally. | PASS |
| GBA Slot-2 compatibility | `Slot2AnalogMappingTest.kt`; `InputAssignmentDtoTypeAdapterTest.kt`; existing `None`, GBA, Memory Expansion and Rumble paths are not shown by a full hardware run. | UNVERIFIED |
| SM64DS EU right-stick camera | `enhancements/sm64ds.eu.right-stick-camera/manifest.json`, `README.md`, `CameraInputProtocolTest.kt`, `CameraAssemblyProtocolTest.kt`; exact identity, axes, deadzone, sensitivity, R3 and lifecycle declarations exist, but payload input/gameplay/device proof is absent. | SOURCE_ONLY |
| SM64DS EU widescreen | `enhancements/sm64ds.eu.widescreen/manifest.json`, `README.md`, `tools/test_widescreen_tools.py`, `tools/test_manifest.py`; guarded source package and fallback contract exist, but runtime geometry/gameplay proof is absent. | SOURCE_ONLY |
| SM64DS EU 60 FPS | `enhancements/sm64ds.eu.60fps/manifest.json`, `README.md`, `tools/test_patch_tools.py`, `tools/test_patch.py`; package and research payload are present, but legal revision-matched inputs and timing/gameplay evidence are absent. | SOURCE_ONLY |
| Three add-ons independently updateable | Separate IDs/directories: `sm64ds.eu.right-stick-camera`, `sm64ds.eu.widescreen`, `sm64ds.eu.60fps`; manifests declare conflicts/dependencies where applicable. | PASS |
| Upstream update invariant | `docs/FORK_MAINTENANCE.md` documents the bounded update procedure and required checks. No completed upstream-update run proving all invariants is recorded. | UNVERIFIED |
| Unit tests | `./gradlew :enhancements:test --no-daemon`: `BUILD SUCCESSFUL`; `./gradlew :app:test --no-daemon`: `BUILD SUCCESSFUL`; focused Python suites and `enhancements/tools/test_rom_identity.py`: PASS. | PASS |
| Native bridge build | `./gradlew :app:compileGitHubProdDebug --no-daemon`: `BUILD SUCCESSFUL`. | PASS |
| APK assemble | `./gradlew :app:assembleGitHubProdDebug --no-daemon`: `BUILD SUCCESSFUL`; current local/CI APK path and hash evidence is external and not tracked. | PASS |
| Lint | `./gradlew lintGitHubProdDebug --no-daemon` recorded checkout-wide failure: 635 errors, 574 warnings, 1 hint; first failure is `DeviceLayoutDisplayMapper.kt:20`. | UNVERIFIED |
| Original ROM launch with add-ons disabled | Existing M3 evidence in this document's prior audit recorded baseline package launch, but no fresh clean disabled-add-on launch is attached to this matrix. | UNVERIFIED |
| Thor physical install/input/launch/gameplay evidence | ADB/device connectivity and prior installation attempts are historical evidence only; accepted SM64DS camera, widescreen, 60 FPS, Slot-2 input, display, RA unlock, and gameplay proof require the physical Thor and valid runtime inputs. | BLOCKED-EXTERNAL-INPUT |
| Payload legality / redistribution permission | `enhancements/NOTICE.md` contains provenance metadata, not redistribution authorization; required legal ARM9/overlay/ROM-derived inputs are absent. | BLOCKED-EXTERNAL-INPUT |

## Decision

The generic framework, package validation, focused tests, build, and APK
assembly are repository `PASS` evidence. The three SM64DS packages remain
`SOURCE_ONLY`; this does not establish accepted payloads or gameplay. Lint,
the completed upstream-update invariant, a fresh disabled-add-on launch, and
full legacy Slot-2 compatibility remain `UNVERIFIED`. Legal payload permission
and physical Thor acceptance are `BLOCKED-EXTERNAL-INPUT`. No release,
distribution, or completion claim follows from this matrix.
