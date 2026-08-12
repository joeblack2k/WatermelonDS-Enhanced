# WatermelonDS Enhanced acceptance audit

Audit scope: bounded FIX-FIRST evidence correction, performed against the
published branch `codex/watermelon-enhanced-pr`, after M1/M2. The live PR head
is authoritative via GitHub. The code-bearing parent is historical commit
`34ea454454bcf62c31f83b451078791153cec646`. This documentation-only correction
does not change code, manifests, payloads, or
private evidence. The base is
`Fet_OfflineChevos@1e0a463d785448ab47f78a12e2a88241df288cf9`.
This is an evidence record only. No payload was repaired, and no release or
runtime acceptance is claimed. One enhancement-owned `PluralsCandidate` warning remains for
`enhancement_import_success` and is recorded below rather than suppressed.
The current lint rerun was performed at `2026-08-12 12:38:21 +0200`; external
log SHA-256: `11e3ba3fa7b1ca291a34c816d8a2204619d91ea1e74ec3064ee19756ebdf3900`.

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
| origin | `https://github.com/joeblack2k/WatermelonDS-Enhanced.git`; live PR head for `codex/watermelon-enhanced-pr` is authoritative via GitHub |
| upstream | `https://github.com/SapphireRhodonite/WatermelonDS.git`; the maintained base branch is `Fet_OfflineChevos` at `1e0a463d785448ab47f78a12e2a88241df288cf9` |
| audit base | `1e0a463d785448ab47f78a12e2a88241df288cf9` (`Fet_OfflineChevos`) |
| published branch | `codex/watermelon-enhanced-pr`; live PR head is authoritative via GitHub |
| merge-base with upstream/master | `7e31d08a3e0e3801c0aca393832238395ac63983` |
| gitlink | `melonDS-android-lib` at `e4022d7e7ada535ff5ea9d087db389cc46e8da62` |
| PR status | PR #1 is OPEN and DRAFT; base `Fet_OfflineChevos` at `1e0a463d785448ab47f78a12e2a88241df288cf9`; live head is authoritative via GitHub |

### Evidence scope at current candidate

The requested audit target is the published branch
`codex/watermelon-enhanced-pr`; the PR base is
`1e0a463d785448ab47f78a12e2a88241df288cf9`. This correction updates only the
three permitted documentation files:

```text
docs/ENHANCED_ACCEPTANCE.md
README.md
docs/FORK_MAINTENANCE.md
```

The evidence below is audited against the published candidate.

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

All three SM64DS manifests report the same installability boundary:

| Manifest | `status` | `distributionStatus` | `payloadInput` | `guardedPayload` |
| --- | --- | --- | --- | --- |
| `sm64ds.eu.right-stick-camera` | `SOURCE_ONLY` | `SOURCE_ONLY` | `MISSING` | `UNVERIFIED` |
| `sm64ds.eu.widescreen` | `SOURCE_ONLY` | `SOURCE_ONLY` | `MISSING` | `UNVERIFIED` |
| `sm64ds.eu.60fps` | `SOURCE_ONLY` | `SOURCE_ONLY` | `MISSING` | `UNVERIFIED` |

## 60fps-v10 claim audit

This is a repository audit of the supplied v10 artifact, not gameplay or
device evidence. Status values in this table are deliberately limited to
`repository-verified`, `historical bounded evidence`, and `unavailable`.
The manifest status remains `SOURCE_ONLY`.

| Claim | Status | Evidence path / commit / hash | Boundary |
| --- | --- | --- | --- |
| Cadence | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/README.md`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py`; published branch | Tooling and the bounded payload structure identify a cadence region, but no legal ARM9/overlay input or real run proves timing. Manifest: `UNVERIFIED`. |
| Gameplay physics | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/player_timestep.s`; `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; published branch | Source/runtime material describes intended timestep hooks only; movement and physics were not observed. Manifest: `UNVERIFIED`. |
| Timers | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/player_timestep.s`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py`; published branch | Structural timer branches are not proof of correct in-game timer behavior. Manifest: `UNVERIFIED`. |
| Animation | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/animation_timestep.s`; `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; published branch | Source-only animation logic is present, but pose continuity and visible animation were not tested. Manifest: `UNVERIFIED`. |
| Particles | `historical bounded evidence` | `enhancements/sm64ds.eu.60fps/runtime/world_timestep.s`; published branch | A bounded particle hook is an implementation input, not evidence that particle lifetimes/rendering remain correct. Manifest: `UNVERIFIED`. |
| Audio | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no audio capture or accepted runtime log | No repository artifact proves audio continuity or correct pitch/rate at the intended cadence. Manifest: `UNVERIFIED`. |
| Save-state | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no save/load session evidence | No repository artifact proves save-state behavior while the source-only patch is active. Manifest: `UNVERIFIED`. |
| Correct game speed | `unavailable` | `enhancements/sm64ds.eu.60fps/README.md`; no real gameplay run or speed measurement | A cadence counter, emulator overclock, or payload hash cannot prove 1x game speed. No new runtime claim is made. |
| Payload input integrity | `unavailable` | `enhancements/sm64ds.eu.60fps/manifest.json`; `enhancements/sm64ds.eu.60fps/patches/60fps-v10.ards`; SHA-256 `6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7` | Repository identity and hash binding do not supply the required legal revision-matched ARM9/overlay/raw gameplay input. Manifest: `payloadInput: MISSING`. |
| Guarded payload integrity | `unavailable` | `enhancements/sm64ds.eu.60fps/manifest.json`; `enhancements/sm64ds.eu.60fps/tools/verify_patch.py` | The declared expected words and bounded guard structure are present, but required source input is unavailable and no guarded payload verification was completed. Manifest: `guardedPayload: UNVERIFIED`. |

The seven gameplay claims represented by the manifest (`cadence`,
`gameplayPhysics`, `timers`, `animation`, `particles`, `audio`, and
`saveState`) remain exactly `UNVERIFIED`. The checked-in 60fps-v10 payload has
SHA-256 `6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7`,
but the repository does not supply the missing legal revision-matched
ARM9/overlay/raw gameplay input. The manifest therefore remains
`payloadInput: MISSING` and `guardedPayload: UNVERIFIED`; this does not turn the
source-only package into a runtime-verified payload or establish any gameplay
claim.

## Repository command matrix and evidence

All commands below were run against the published branch. The live PR head is
authoritative via GitHub. They are repository evidence only;
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
checkout-wide result with 635 errors, 574 warnings, and 1 hint. This is a
candidate snapshot, not a base comparison; the counts are not being classified
against the earlier 630/573 result. The seven
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
| Frontend key-down edge | `app/src/main/java/me/magnum/melonds/ui/emulator/input/InputProcessor.kt` at code-bearing parent commit `34ea454454bcf62c31f83b451078791153cec646`; the `KEYCODE_BUTTON_THUMBR` path sends recenter only for `ACTION_DOWN` with `repeatCount == 0`. | **PROVEN** |
| Sequence transport / Slot-2 offset `0x06` | `app/src/main/java/me/magnum/melonds/ui/emulator/input/InputProcessor.kt` and `enhancements/src/main/kotlin/me/magnum/enhancements/CameraInputProtocol.kt` at code-bearing parent commit `34ea454454bcf62c31f83b451078791153cec646` structurally transport `recenterSequence`; `enhancements/sm64ds.eu.right-stick-camera/README.md` records the required Slot-2 offset `0x06` read/compare/update structure. | **STRUCTURALLY PROVEN** |
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
| Candidate APK SHA-256 | `37b32ac828008e0c4fcc7b411f5fe06f8a268ec78ac4ff4cefa78155091e740c` |
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
- `./gradlew lintGitHubProdDebug --no-daemon`: **FAIL**, 635 errors, 574 warnings, 1 hint. The scoped enhancement counts do not classify every checkout-wide lint finding as baseline. Rerun: `2026-08-12 12:38:21 +0200`; log SHA-256: `11e3ba3fa7b1ca291a34c816d8a2204619d91ea1e74ec3064ee19756ebdf3900`.

The camera and 60 FPS packages remain `SOURCE_ONLY`. Licensing/provenance,
legal ROM inputs, RetroAchievements compatibility, enhancement runtime
activation, gameplay correctness, and physical Thor validation remain open.
No APK/build output, ROM, credential, or capture is tracked.

## Required verdict

Repository build and baseline debug launch evidenced; distribution and
enhancement runtime acceptance remain blocked.

## M3 device attempt

This bounded M3 attempt used ADB serial `6b0af897` and the current locally
assembled APK:
`app/build/outputs/apk/gitHubProd/debug/app-gitHub-prod-debug.apk`
(installed APK SHA-256
`a7641d5503df4a64543e03d17c480ec91a8540c245ca690f412739739f1e6ba4`).
The APK installed successfully over package
`me.magnum.melondualds.dev`, version `0.7.0.rc5`, versionCode `39`.
ThorDS package `io.github.joeblack2k.thords` remained installed at version
`0.1.2-beta.2.8`; its package state was captured before and after.

Separately, the current local candidate APK has SHA-256
`37b32ac828008e0c4fcc7b411f5fe06f8a268ec78ac4ff4cefa78155091e740c`. Its ABI
contents were inspected locally, but this candidate was not device-validated
and is not the APK installation evidence recorded above.

The existing directory `/sdcard/Roms/NDS` was inspected and contains the
requested ROM at the exact requested path. It also contains an existing
`0022 - Super Mario 64 DS (EU).sav` (8 KiB, dated 2026-08-09), already
colocated with the matching ROM. No save was copied, created, or overwritten;
the requested statement that no SM64DS save was found is therefore not
supported by the observed device state.

The normal `RomListActivity` launcher was started. Android displayed the
system `Use USB for` dialog with `No data transfer`, `File transfer`, and
`CANCEL`. Back, a normal visible-coordinate cancel attempt, and Tab/Enter did
not clear the dialog; the UI dump continued to report it. No debug receiver
was used. The ROM was not launched, and no ROM configuration was changed or
read through the blocked UI. Consequently this attempt provides no proof of
the original URI/config, empty `enabledEnhancements`, or an actual ROM frame.

Post-state evidence is in `/tmp/watermelon-m3-20260811-220901/`, including
the install result, redacted package/activity dumps, bounded filtered logcat,
ROM directory listing, launcher screenshots, and the materialization scan.
The scan found only the app's existing
`/sdcard/Android/data/me.magnum.melondualds.dev/cache` path; no
`Enhancements` package or temporary materialized ROM was observed. The
launcher/activity screenshot was black and is not counted as a game-frame
acceptance screenshot.

After a device reboot, the USB dialog was cleared and the normal UI flow
continued. SAF selected `/sdcard/ROMs/NDS`; the device path is uppercase
`ROMs`, while the earlier shell inventory also showed the existing
`/sdcard/Roms/NDS` alias/path. The user-visible ROM list showed
`0022 - Super Mario 64 DS (EU).nds` adjacent to
`0022 - Super Mario 64 DS (EU).sav` (8 KiB), confirming save adjacency
without any copy or overwrite.

The first search card was accidentally launched during search; bounded logs
identified that launch as USA Rev1 `ASME`, so it is explicitly excluded from
EU acceptance. The second card was launched through the
normal UI. Bounded logcat identified EU `ASMP`, a URI ending in
`0022 - Super Mario 64 DS (EU).nds`, and `Game is now booting`, with no fatal
exception. This proves normal EU ROM launch and save adjacency; it does not
claim USA launch acceptance. After launch, the pause menu showed the actual
game image behind it. Tapping Resume produced the non-black gameplay/title
frame `/tmp/watermelon-m3-20260811-220901/eu-game-visible.png`, showing the
Super Mario 64 DS EU title screen (a title-screen frame, not a gameplay
frame). Normal EU launch and actual game-frame
acceptance are therefore proven. No evidence established the original ROM
config or an empty `enabledEnhancements` set, and no add-on disabled-config
claim is made.

M3 normal EU launch and actual game-frame acceptance are **PROVEN**. Add-on
gameplay, camera, widescreen, 60fps, and explicit empty
`enabledEnhancements` config acceptance remain open. The earlier USB-dialog
blocker was cleared by the reboot. This evidence statement does not establish
any additional source, payload, license, workflow, ROM, save, or ThorDS
acceptance.

## M1 private widescreen fixture evidence

This bounded M1 follow-up used ADB serial `6b0af897` and the corrected private
fixture at `/tmp/watermelon-m1-20260811-232444/private-widescreen-installable.zip`.
The first fixture attempt failed for packaging only: the archive used a
`bundle/` wrapper, which did not match the declared package paths. The fixture
was rebuilt before import with `manifest.json` at the package root and
`patches/widescreen.ards` at the manifest-declared path.

The corrected fixture was imported successfully through the normal SAF flow
exactly once. `run-as` evidence proves the installed files:

```text
files/Enhancements/sm64ds.eu.widescreen/manifest.json
files/Enhancements/sm64ds.eu.widescreen/patches/widescreen.ards
```

The fixture manifest was private-installable with `distributionStatus:
INSTALLABLE`, exact EU `ASMP` revision matching, and verified
`payloadInput`/`guardedPayload` evidence. The committed widescreen source-only
manifests were not changed; the direct source-only ZIP remains rejected by the
installer contract.

The existing EU ROM and adjacent save remained unchanged. No USA ROM was
launched or modified. The existing EU save remains 8 KiB and colocated with
the matching ROM. Raw M1 evidence remains under
`/tmp/watermelon-m1-20260811-232444/`; no evidence was staged or committed.

This proves private normal SAF import and materialized package paths only.
Per-ROM activation, guard activation, wrong-guard fail-closed 4:3 fallback,
paired viewport/geometry screenshots, and disabled-add-on comparison remain
**UNPROVEN** because normal navigation did not reach the per-ROM activation
UI. No widescreen visual or guard acceptance claim is made from the successful
import alone.

### M1 normal ROM config evidence

Subsequent normal UI evidence proves that the retained package is exposed by
the EU ROM's normal ROM Config UI. The screenshot
`/tmp/watermelon-m1-20260811-232444/enhancement-enabled.png` shows the
`sm64ds.eu.widescreen` toggle **ON**. The redacted/config evidence
`/tmp/watermelon-m1-20260811-232444/rom_data.json`, checked with `jq`, proves
that the EU ROM has exactly:

```json
["sm64ds.eu.widescreen"]
```

in `enabledEnhancements`. This proves normal UI per-ROM activation/config
selection for the EU ROM; it does not by itself prove guard activation,
widescreen presentation, or the wrong-guard 4:3 fallback.

The copied Thor save artifacts are confined to the private
`/tmp/m1-evidence-20260811/thor-saves` evidence directory and were not used to alter
device data. The device's existing EU save remained unchanged; its recorded
SHA-256 is
`56a27e0d78ea2ea6a767567897b6167bf3774f0b064a10c3d56054ac41d067b0`. USA was
not launched or modified. The committed source-only manifests remain
unchanged.

### M1 gameplay-capture correction

The attempted post-activation gameplay capture is invalid as EU evidence. After
force-stop and relaunch, the assumed right-side **Continue Playing** card
launched the USA revision, identified in bounded logs as `ASME`, not the EU
`ASMP` ROM. The app was immediately force-stopped. No debug receiver, direct
configuration mutation, or other non-UI launch path was used. This USA launch
is a negative-control observation and must not count as EU gameplay,
widescreen, guard, viewport, or geometry evidence.

The current read-only `rom_data.json` state remains the authoritative config
snapshot: USA has `enabledEnhancements=[]`, while EU has exactly
`enabledEnhancements=["sm64ds.eu.widescreen"]`. Device saves remain unchanged:
the EU save SHA-256 is
`56a27e0d78ea2ea6a767567897b6167bf3774f0b064a10c3d56054ac41d067b0`, and the
USA zero-byte save SHA-256 is
`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
Accordingly, no EU gameplay or visual acceptance claim is made from that
capture attempt.

### M1 renderer/runtime correction

A later search-result long-press on the second Super Mario 64 DS entry opened
the normal ROM details screen and its normal Play path. The resulting emulator
session rendered black; bounded logcat showed repeated
`melonDS: EGL_BAD_ACCESS` and `Failed to use OpenGL context` messages. The
session was force-stopped. This is a renderer/runtime gate, not visual
widescreen or gameplay evidence, and no such claim is made.

The final read-only `rom_data.json` state remains unchanged: USA has
`enabledEnhancements=[]`, and EU has exactly
`enabledEnhancements=["sm64ds.eu.widescreen"]`.

### M1 clean EU baseline

After a Thor reboot and normal app launch, the third **Continue Playing** card
was verified by fresh log evidence as EU `ASMP`. The EU add-on was disabled
through the normal ROM Config UI, and the session was launched normally. It
remained black after 10 seconds. The fresh log captured
`Inserted cart with game code: ASMP` and `Game is now booting`, with no new EGL
error lines in the captured tail. The session was force-stopped afterward.

The final read-only `rom_data.json` state is now USA
`enabledEnhancements=[]` and EU `enabledEnhancements=[]`. This is a repeatable
SM64DS baseline black-screen gate, not widescreen evidence; it provides no
visual gameplay or widescreen acceptance.
