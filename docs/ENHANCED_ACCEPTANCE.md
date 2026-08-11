# WatermelonDS Enhanced acceptance audit

Audit scope: Milestone 3, performed from local HEAD
`5784068e15cc77fab71adfbf8217fa06940ccb2e` plus this uncommitted documentation
batch, after M1/M2. Remote PR head is `c68734fe226b70d60b20908b2af191293f084c7a`
and base is `Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9`.
This is an evidence record only. No payload was repaired, and no commit, push,
or review was performed. The enhancement lint batch covers eight
enhancement-owned findings: seven `MissingTranslation` findings received the
scoped resource-level correction requested for this batch in
`app/src/main/res/values/strings.xml`. That resource is part of this batch.
One enhancement-owned `PluralsCandidate` warning remains for
`enhancement_import_success` and is recorded below rather than suppressed.
The final lint rerun was performed at `2026-08-11 20:55:59 +0200`.

## Two-layer decision

- **REPOSITORY EVIDENCE AUDITED.**
- **DISTRIBUTION BLOCKED.** Source-license metadata is
  not confirmation of redistribution permission for the SM64DS-derived
  Slot-2 analog material. Until that permission is documented, this repository
  cannot be treated as an accepted distributable package.
- **RUNTIME/DEVICE NOT ACCEPTED.** No claim is made that the enhancements
  provide accepted gameplay, persistent state, or physical-device behavior.
  The manifests themselves keep the relevant packages `SOURCE_ONLY` or
  `UNVERIFIED`.

Repository acceptance is therefore not a release, gameplay, APK, or device
acceptance.

## Git and PR evidence

| Item | Evidence |
| --- | --- |
| origin | `https://github.com/joeblack2k/WatermelonDS-Enhanced.git`; PR head resolves to `c68734fe226b70d60b20908b2af191293f084c7a` |
| upstream | `https://github.com/SapphireRhodonite/melonDS-android.git`; the maintained base branch is `Fet_OfflineChevos` at `1e0a463d785448ab47f78a12e2a88241df288cf9` |
| audit base | `1e0a463d785448ab47f78a12e2a88241df288cf9` (`Fet_OfflineChevos`) |
| local head | `5784068e15cc77fab71adfbf8217fa06940ccb2e` plus this uncommitted batch |
| merge-base with upstream/master | `7e31d08a3e0e3801c0aca393832238395ac63983` |
| gitlink | `melonDS-android-lib` at `e4022d7e7ada535ff5ea9d087db389cc46e8da62` |
| PR status | PR #1 is OPEN and DRAFT; base `Fet_OfflineChevos` at `1e0a463d785448ab47f78a12e2a88241df288cf9`; remote head `codex/watermelon-enhanced-pr` at `c68734fe226b70d60b20908b2af191293f084c7a` |

### Documentation correction at current HEAD

The requested audit HEAD is local `5784068e15cc77fab71adfbf8217fa06940ccb2e`; the PR
base is `Fet_OfflineChevos` at
`1e0a463d785448ab47f78a12e2a88241df288cf9`. This correction updates only the
three permitted documentation files plus the scoped resource annotation batch
in `app/src/main/res/values/strings.xml`:

```text
docs/ENHANCED_ACCEPTANCE.md
README.md
docs/FORK_MAINTENANCE.md
```

No source code or payload was changed. The existing committed source and
payload evidence below is therefore audited against the requested current
HEAD, not against an older snapshot.

## Payload inventory

All hashes below are SHA-256 values observed in the files and compared with
the corresponding manifest values.

| Payload | SHA-256 | Lines |
| --- | --- | ---: |
| `enhancements/sm64ds.eu.60fps/patches/60fps-v10.ards` | `6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7` | 937 |
| `enhancements/sm64ds.eu.right-stick-camera/patches/camera.ards` | `b582a3372a0cf6c60c3c14da32dbc39d5e233c12f3d22efaa87b1abe25e63053` | 61 |
| `enhancements/sm64ds.eu.right-stick-camera/patches/slot2-analog.ards` | `e68025c3aad3a47941ab2903dd9d212b91bafedff705ea6252677c27d07bdb1c` | 18 |
| `enhancements/sm64ds.eu.widescreen/patches/widescreen.ards` | `28445a89a887a556b4a0564e21f8ca579eeab437471bff1b38c681efd6a3bbc6` | 18 |

The four payloads total 1,034 lines. The 60fps and camera manifests remain
runtime-unaccepted; matching a hash proves identity, not gameplay correctness.

## 60fps-v10 claim audit

This is a repository audit of the supplied v10 artifact, not gameplay or
device evidence. Status values in this table are deliberately limited to
`repository-verified`, `historical bounded evidence`, and `unavailable`.
The manifest status remains `SOURCE_ONLY`.

| Claim | Status | Evidence path / commit / hash | Boundary |
| --- | --- | --- | --- |
| Cadence | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/README.md`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py`; local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` | Tooling and the bounded payload structure identify a cadence region, but no legal ARM9/overlay input or real run proves timing. Manifest: `UNVERIFIED`. |
| Gameplay physics | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/player_timestep.s`; `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` | Source/runtime material describes intended timestep hooks only; movement and physics were not observed. Manifest: `UNVERIFIED`. |
| Timers | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/player_timestep.s`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py`; local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` | Structural timer branches are not proof of correct in-game timer behavior. Manifest: `UNVERIFIED`. |
| Animation | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/animation_timestep.s`; `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` | Source-only animation logic is present, but pose continuity and visible animation were not tested. Manifest: `UNVERIFIED`. |
| Particles | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` | A bounded particle hook is an implementation input, not evidence that particle lifetimes/rendering remain correct. Manifest: `UNVERIFIED`. |
| Audio | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no audio capture or accepted runtime log | No repository artifact proves audio continuity or correct pitch/rate at the intended cadence. Manifest: `UNVERIFIED`. |
| Save-state | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no save/load session evidence | No repository artifact proves save-state behavior while the source-only patch is active. Manifest: `UNVERIFIED`. |
| Correct game speed | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no real gameplay run or speed measurement | A cadence counter, emulator overclock, or payload hash cannot prove 1x game speed. No new runtime claim is made. |
| Payload input integrity | `repository-verified` | `enhancements/sm64ds.eu.60fps/manifest.json`; `enhancements/sm64ds.eu.60fps/patches/60fps-v10.ards`; SHA-256 `6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7` | Repository identity and hash binding only. Manifest: `payloadInput: VERIFIED`. |
| Guarded payload integrity | `repository-verified` | `enhancements/sm64ds.eu.60fps/manifest.json`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py` | Declared expected words and bounded guard structure are verified from repository artifacts; no raw gameplay input or runtime application is proven. Manifest: `guardedPayload: VERIFIED`. |

The seven gameplay claims represented by the manifest (`cadence`,
`gameplayPhysics`, `timers`, `animation`, `particles`, `audio`, and
`saveState`) remain exactly `UNVERIFIED`. Separately, the manifest records
`payloadInput: VERIFIED` and `guardedPayload: VERIFIED`: the checked-in
60fps-v10 payload has SHA-256
`6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7`, and its
declared identity/guard structure is internally bound. That integrity result
does not supply the missing legal revision-matched ARM9/overlay/raw gameplay
input, and does not turn the source-only package into a runtime-verified
payload or establish any gameplay claim.

## Repository command matrix and evidence

All commands below were run from this checkout at HEAD
`5784068e15cc77fab71adfbf8217fa06940ccb2e`. They are repository evidence only;
none is runtime, distribution, or device acceptance.

| Area | Command | Result |
| --- | --- | --- |
| Camera payload tools | `python3 enhancements/sm64ds.eu.right-stick-camera/tools/test_patch_tools.py` | **PASS**, 3 tests |
| Camera verifier | `python3 enhancements/sm64ds.eu.right-stick-camera/tools/verify_patch.py` | **NOT RUN / INPUT MISSING**, requires `--arm9-image` and a patch |
| 60fps payload tools | `python3 enhancements/sm64ds.eu.60fps/tools/test_patch_tools.py` | **PASS**, 9 tests |
| 60fps patch test | `python3 enhancements/sm64ds.eu.60fps/tools/test_patch.py` | **NOT RUN / INPUT MISSING**; required legal ROM-derived inputs are unavailable |
| 60fps verifier | `python3 enhancements/sm64ds.eu.60fps/tools/verify_patch.py` | **NOT RUN / INPUT MISSING**, requires manifest, ARM9 and overlay-2 images |
| Widescreen payload tools | `python3 enhancements/sm64ds.eu.widescreen/tools/test_widescreen_tools.py`; `python3 enhancements/sm64ds.eu.widescreen/tools/test_manifest.py` | **PASS**, both tools |
| Enhancements unit tests | `./gradlew :enhancements:test --no-daemon` | **PASS**, `BUILD SUCCESSFUL` |
| App unit tests | `./gradlew :app:test --no-daemon` | **PASS**, `BUILD SUCCESSFUL` |
| App compile | `./gradlew :app:compileGitHubProdDebugKotlin --no-daemon` | **PASS**, `BUILD SUCCESSFUL` |
| Debug assembly | `./gradlew :app:assembleGitHubProdDebug --no-daemon` | **PASS**, `BUILD SUCCESSFUL` |

### Lint result

`./gradlew lintGitHubProdDebug --no-daemon`: **FAIL**, an actual current
checkout-wide result with 630 errors, 573 warnings, and 1 hint. The seven
enhancement `MissingTranslation` annotations leave zero enhancement-owned
`MissingTranslation` findings. One enhancement-owned `PluralsCandidate`
warning remains for `enhancement_import_success` and is left explicitly
recorded rather than suppressed. These counts are scoped to the enhancement
resources and do not classify every checkout-wide lint finding as baseline; the first reported failure is
`DeviceLayoutDisplayMapper.kt:20` (`WrongConstant` for `createWindowContext`).
This is recorded separately from the passing compile and assembly results and
is not acceptance evidence.

## Boundary and content scans

- Secret scan: no embedded private key, credential value, or token was found.
  Workflow references to CI secret variables are expected configuration
  references, not leaked values.
- User-specific host-path scan: no user-specific host path is recorded in this
  document or tracked enhancement artifact. Tracked `/private` placeholders
  are repository text, not a claim about a particular user's host.
- ROM scan: no ROM image was added to the enhancement packages.
- ARM/object scan: no ARM binary, object file, or compiled payload was added to
  the enhancement packages.
- Capture scan: no capture artifact was added to the enhancement packages.
- SM64DS address isolation: the SM64DS addresses used by the enhancement
  payloads were not found in generic emulator paths outside `enhancements/`.
  This confirms isolation of the audited address literals; it does not prove
  the payloads are correct on hardware.

## Open gates

The following gates remain open and keep the second layer **RUNTIME/DEVICE
NOT ACCEPTED**:

1. R3 persistent per-ROM state and lifecycle proof.
2. Camera and 60fps packages remain source-only until their required source
   inputs, guarded payload verification, and package-level evidence exist.
3. No accepted 60fps gameplay claim: cadence, physics/timers, animation,
   particles, audio, save-state behavior, and game-speed correctness remain
   unverified.
4. Licensing/provenance confirmation for every redistributed payload,
   especially the Slot-2 analog payload and its referenced upstream material.
5. Physical AYN Thor/device acceptance of enhancement/ROM launch, controls,
   and gameplay. A black screenshot is non-UI evidence only and does not prove
   enhancement behavior or UI acceptance.

## R3 evidence

| R3 item | Evidence | Result |
| --- | --- | --- |
| Frontend key-down edge | `app/src/main/java/me/magnum/melonds/ui/emulator/input/InputProcessor.kt` at local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e`; the `KEYCODE_BUTTON_THUMBR` path sends recenter only for `ACTION_DOWN` with `repeatCount == 0`. | **PROVEN** |
| Sequence transport / Slot-2 offset `0x06` | `app/src/main/java/me/magnum/melonds/ui/emulator/input/InputProcessor.kt` and `enhancements/src/main/kotlin/me/magnum/enhancements/CameraInputProtocol.kt` at local HEAD `5784068e15cc77fab71adfbf8217fa06940ccb2e` structurally transport `recenterSequence`; `enhancements/sm64ds.eu.right-stick-camera/README.md` records the required Slot-2 offset `0x06` read/compare/update structure. | **STRUCTURALLY PROVEN** |
| Game-side persistent consumer | `enhancements/sm64ds.eu.right-stick-camera/README.md` states that the existing evidence does not prove the persistent addon-state slot or the required payload words; the checked-in assembly remains only a rebuild input. | **ABSENT / NOT PROVEN** |
| Physical result | Existing audit evidence records no accepted AYN Thor/device result, including camera or persistent-state behavior. | **UNAVAILABLE** |

**R3 GAME-SIDE BLOCKED.** The frontend edge and transport structure do not
prove a game-side persistent consumer or physical behavior. Camera remains
`SOURCE_ONLY`.

## Judgment record

The repository decision is limited by the unresolved redistribution gate:
identity matching, guarded manifest structure, payload reproducibility, and
focused tests do not authorize distribution. A build or hash match cannot close
runtime gates. The explicit two-layer result prevents repository acceptance from being read as a claim of
working 60fps gameplay or physical Thor behavior.

## Final verdict

## M3 current evidence

Evidence directory: `/tmp/watermelon-enhanced-m2-20260811-203230`.

| Item | Evidence |
| --- | --- |
| APK | `app/build/outputs/apk/gitHubProd/debug/app-gitHub-prod-debug.apk` |
| Installed M2 APK SHA-256 | `09c3672d426162fec397d7500001fff732948bb86c42a9dd5dda51fa42890f4c` |
| Final local rebuild APK SHA-256 | `547c911a52b99cba4eab995d55d9f47bd9d82ce250ae5844f67c50664fd08f55` |
| Package and version | `me.magnum.melondualds.dev`, version `0.7.0.rc5`, versionCode `39` |
| SDK metadata | min SDK `24`, target/compile SDK `36` |
| Packaged APK ABI contents | `arm64-v8a`, `armeabi-v7a`, and `x86_64` |
| Baseline launch | M2 `activity-state.txt`, `am-start-W.txt`, `metadata.txt`, and `logcat-500.txt` record package launch evidence; this is not enhancement gameplay proof |

The exact final-worktree checks rerun for this batch were:

- `./gradlew :enhancements:test --no-daemon`: **PASS**
- `./gradlew :app:test --no-daemon`: **PASS**
- `./gradlew :app:assembleGitHubProdDebug --no-daemon`: **PASS**
- All three renamed SM64DS Python tool suites and `enhancements/tools/test_rom_identity.py`: **PASS**
- Scoped generic isolation, secret, ROM, binary/object, and capture scans: **PASS**
- `git diff --check`: **PASS**
- `./gradlew lintGitHubProdDebug --no-daemon`: **FAIL**, 630 errors, 573 warnings, 1 hint; seven enhancement `MissingTranslation` annotations leave 0 enhancement-owned `MissingTranslation` findings, with one enhancement-owned `PluralsCandidate` warning remaining for `enhancement_import_success`. The scoped enhancement counts do not classify every checkout-wide lint finding as baseline. Rerun: `2026-08-11 20:55:59 +0200`.

The camera and 60 FPS packages remain `SOURCE_ONLY`. Licensing/provenance,
legal ROM inputs, RetroAchievements compatibility, enhancement runtime
activation, gameplay correctness, and physical Thor validation remain open.
No APK/build output, ROM, credential, or capture is tracked.

## Required verdict

Repository build and baseline debug launch evidenced; distribution and
enhancement runtime acceptance remain blocked.
