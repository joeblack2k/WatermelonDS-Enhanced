# Goal Prompt: WatermelonDS Enhanced Add-ons

## Objective

Implement a general, opt-in `Enhanced` add-on system in WatermelonDS Android.
Users must be able to install and enable game-specific enhancements without
requiring a new WatermelonDS APK for every enhancement release.

The system must support more than Action Replay. An enhancement may combine
controller input ownership, native emulator capabilities, runtime input
protocols, guarded runtime code, overlays, Action Replay codes, or IPS/BPS
patches applied to temporary runtime data.

The right-stick camera for Super Mario 64 DS is the reference use case. It
requires right-stick capture, deadzone and sensitivity handling, recentering,
native/runtime state delivery, game-side runtime code, and lifecycle cleanup.
It cannot be reduced to a list of cheat codes.

## Product direction

ThorDS is being phased out as the product branch. This WatermelonDS-based
branch is the implementation path. ThorDS remains only a source of prior
design knowledge and a hardware-validation reference.

Do not copy ThorDS-specific branding, release code, debug receivers, device
assumptions, or SM64DS-only profile decisions into the general framework.

## Identity matching

An add-on must match the ROM identity read from the ROM header:

- game code;
- header checksum or equivalent revision data;
- optional SHA-256 only when a particular variant needs an additional guard.

Never match by filename alone. Never silently apply an add-on to an unknown
revision. A mismatch must fail closed and leave the original emulator behavior
unchanged.

## Package model

Support a package or app-managed directory equivalent to:

```text
Enhancements/
  MarioCamera/
    manifest.json
    README.md
    patches/
      camera.ards
      camera.ips
    runtime/
      camera.overlay
```

The exact Android storage/import mechanism may follow existing SAF patterns.
The manifest must be versioned and must declare:

- stable add-on ID;
- display name, author and version;
- ROM identity match;
- capabilities;
- patch backends and package-relative files;
- compatibility with RetroAchievements Hardcore;
- conflicts or required capabilities where applicable.

Reject duplicate IDs, unsupported schema versions, unsafe paths, missing
metadata, malformed patch declarations, and invalid capability combinations.

## Capability model

Define a small runtime contract instead of hard-coding individual games into
the emulator:

- `prepare` or launch validation;
- controller-axis ownership;
- runtime input/protocol delivery;
- native emulator capability selection;
- guarded runtime code/overlay activation;
- reset and pause handling;
- save-state load handling;
- controller disconnect handling;
- session teardown and neutralization.

Capabilities must be composable. A right-stick camera should be able to
declare, for example:

```text
controller-axis-owner
runtime-input-protocol
runtime-code-patch
```

An active session must expose its effective capabilities and whether Hardcore
RetroAchievements is allowed.

## First native capability: GBA Slot-2 analog

Port the general GBA Slot-2 analog capability from the ThorDS design without
bringing over ThorDS-only code.

Requirements:

- selectable per ROM;
- configurable X/Y axes;
- inversion and deadzone;
- no heuristic axis guessing when an explicit mapping exists;
- no interference with ordinary DS input mappings;
- correct neutral value on pause, reset, activity replacement, controller
  disconnect and emulator stop;
- preserve existing `None`, GBA ROM, Memory Expansion and Rumble behavior;
- safe no-op for unsupported modes such as DSi where appropriate;
- persist the mapping with backward-compatible defaults.

The Controls UI must be able to assign both axes through the dispatch boundary
without processing the same motion event twice.

## Reference enhancement: right-stick camera

The framework must be able to host the SM64DS right-stick camera without a
custom WatermelonDS build.

The enhancement must be able to declare:

- exact game/revision match;
- right-stick X/Y ownership;
- deadzone, inversion and sensitivity;
- R3 recenter;
- a versioned runtime camera protocol;
- guarded game-side runtime code or overlay;
- lifecycle neutralization;
- Casual-only RetroAchievements policy if active.

Do not embed SM64DS addresses, ARM code, or copyrighted ROM data in the generic
framework. Game-specific runtime code belongs in the add-on package and must
carry expected-value guards and provenance metadata.

## RetroAchievements

Resolve the active Enhanced session before RetroAchievements Hardcore starts.
If any active capability is incompatible with Hardcore, block Hardcore
locally before emulator/native/RA side effects.

Casual mode may allow the enhancement. The original ROM identity and normal
RetroAchievements hash must remain unchanged.

## Implementation order

1. Finalize manifest, identity matching and validation.
2. Add catalog loading and per-ROM enable/disable state.
3. Add the capability/session lifecycle contract.
4. Add GBA Slot-2 analog as the first native capability.
5. Add runtime input and guarded patch interfaces needed by the camera.
6. Add the SM64DS camera as an external/reference add-on, not a permanent
   game-specific branch.
7. Add UI status for active add-ons and effective capabilities.
8. Add RetroAchievements policy enforcement.

## Non-negotiables

- Original ROM files remain untouched.
- Unknown ROM revisions fail closed.
- No filename-only matching.
- No permanent SM64DS-specific code in generic emulator paths.
- No ThorDS release infrastructure in the upstream PR.
- No copyrighted ROM, overlay payload, or copied Nintendo code in the repo.
- No duplicate input dispatch.
- Every non-trivial behavior gets a focused runnable test.

## Acceptance evidence

Before calling this complete, provide:

- manifest parser and validation tests;
- header identity matching tests;
- backward-compatible config serialization tests;
- GBA Slot-2 mapping and neutralization tests;
- capability/session lifecycle tests;
- Hardcore blocking test for incompatible enhancements;
- a clean diff from the WatermelonDS release base;
- unit-test, lint and build results;
- physical input evidence for GBA Slot-2 and the right-stick camera when the
  required hardware/runtime package is available.

If a required native or physical gate is unavailable, report it explicitly and
leave the feature marked incomplete. Do not claim the external enhancement is
working from parser tests alone.
