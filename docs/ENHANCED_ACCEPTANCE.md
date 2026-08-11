# WatermelonDS Enhanced acceptance audit

Audit scope: Milestone 3, performed from the current checkout and remote
evidence after M1/M2. This is an evidence record only. No source code or
payload was repaired, and no commit, push, or review was performed.

## Two-layer decision

- **REPOSITORY: REJECTED / DISTRIBUTION BLOCKED.** Source-license metadata is
  not confirmation of redistribution permission for the AM64DS-derived
  Slot-2 analog material. Until that permission is documented, this repository
  cannot be treated as an accepted distributable package.
- **RUNTIME/DEVICE: NOT ACCEPTED.** No claim is made that the enhancements
  provide accepted gameplay, persistent state, or physical-device behavior.
  The manifests themselves keep the relevant packages `SOURCE_ONLY` or
  `UNVERIFIED`.

Repository acceptance is therefore not a release, gameplay, APK, or device
acceptance.

## Git and PR evidence

| Item | Evidence |
| --- | --- |
| origin | `https://github.com/joeblack2k/WatermelonDS-Enhanced.git`; `origin/codex/watermelon-enhanced-pr` resolves to `b08148237c16650eb08ab2fb2ba8c2d189485ae7` |
| upstream | `https://github.com/SapphireRhodonite/melonDS-android.git`; `upstream/master` resolves to `ff049684765dadae608cc1ed677d7dbc8a31227a` |
| audit base | `b08148237c16650eb08ab2fb2ba8c2d189485ae7` |
| head | `b08148237c16650eb08ab2fb2ba8c2d189485ae7` |
| merge-base with upstream/master | `7e31d08a3e0e3801c0aca393832238395ac63983` |
| gitlink | `melonDS-android-lib` at `e4022d7e7ada535ff5ea9d087db389cc46e8da62` |
| PR status | PR #1 is OPEN and DRAFT; base `Fet_OfflineChevos` at `1e0a463d785448ab47f78a12e2a88241df288cf9`; head `codex/watermelon-enhanced-pr` at `b08148237c16650eb08ab2fb2ba8c2d189485ae7` |

### Changed files since the requested base

The cumulative committed diff since `b0814823` is empty because that commit is
the current HEAD. The actual uncommitted worktree diff is separate and includes
the following files:

```text
.gitmodules
README.md
app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt
app/src/main/java/me/magnum/melonds/ui/romdetails/RomDetailsViewModel.kt
enhancements/src/main/kotlin/me/magnum/enhancements/*
enhancements/src/test/kotlin/me/magnum/enhancements/*
enhancements/sm64ds-eu-right-stick-camera/manifest.example.json
enhancements/sm64ds-eu-widescreen/manifest.example.json
enhancements/sm64ds-eu-60fps/manifest.example.json
docs/ENHANCED_ACCEPTANCE.md
docs/FORK_MAINTENANCE.md
enhancements/NOTICE.md
```

This is intentional evidence: the requested base is the current HEAD. The
working tree is not clean, but those existing changes are outside this
document's write scope and were not treated as changes since `b0814823`.

## Payload inventory

All hashes below are SHA-256 values observed in the files and compared with
the corresponding manifest values.

| Payload | SHA-256 | Lines |
| --- | --- | ---: |
| `enhancements/sm64ds-eu-60fps/patches/60fps-v10.ards` | `6b7134f07745400b1978b07450c0e45d0e3352d28a80010b8dbee0ece20f05b7` | 937 |
| `enhancements/sm64ds-eu-right-stick-camera/patches/camera.ards` | `b582a3372a0cf6c60c3c14da32dbc39d5e233c12f3d22efaa87b1abe25e63053` | 61 |
| `enhancements/sm64ds-eu-right-stick-camera/patches/slot2-analog.ards` | `e68025c3aad3a47941ab2903dd9d212b91bafedff705ea6252677c27d07bdb1c` | 18 |
| `enhancements/sm64ds-eu-widescreen/patches/widescreen.ards` | `28445a89a887a556b4a0564e21f8ca579eeab437471bff1b38c681efd6a3bbc6` | 18 |

The four payloads total 1,034 lines. The 60fps and camera manifests remain
runtime-unaccepted; matching a hash proves identity, not gameplay correctness.

## Tests and build evidence

- `:enhancements:test --no-daemon`: **PASS**, Gradle `BUILD SUCCESSFUL`.
- `test_patch_tools.py`: **PASS**, 9 tests.
- `test_manifest.py`: **PASS**.
- `test_widescreen_tools.py`: **PASS**.
- `test_patch.py`: **NOT RUN / INPUT MISSING**. It requires explicit ARM9 and
  overlay-2 images; no ROM-derived inputs were supplied for this audit.
- `assembleDebug --no-daemon`: **PASS**, `BUILD SUCCESSFUL` in 3m44s
  (240 actionable tasks; 138 executed, 102 up-to-date). The task graph
  compiled the configured debug variants and emitted existing compiler
  warnings; APK assembly was not used as runtime or device evidence.
- `lintGitHubProdDebug --no-daemon`: **FAIL**, with 630 errors, 572 warnings,
  and 1 hint when rerun. This is not acceptance evidence.

## Boundary and content scans

- Secret scan: no embedded private key, credential value, or token was found.
  Workflow references to CI secret variables are expected configuration
  references, not leaked values.
- Private-path scan: no private absolute path is recorded in this document or
  tracked enhancement artifact. Local worktree metadata may contain checkout
  paths; that is not package content.
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
5. Physical AYN Thor/device acceptance, including real launch, controls,
   camera, presentation, persistent-state, and gameplay evidence.

## Judgment record

The repository decision is limited by the unresolved redistribution gate:
identity matching, guarded manifest structure, payload reproducibility, and
focused tests do not authorize distribution. A build or hash match cannot close
runtime gates. The explicit two-layer result prevents repository acceptance from being read as a claim of
working 60fps gameplay or physical Thor behavior.
