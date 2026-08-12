# Goal Prompt: WatermelonDS Enhanced Fork Migration

## Objective

Continue development in the real fork:

```text
https://github.com/joeblack2k/WatermelonDS-Enhanced
```

The fork must remain updateable from:

```text
https://github.com/SapphireRhodonite/WatermelonDS
```

using the upstream `Fet_OfflineChevos` branch as the base. The old
`joeblack2k/ThorDS` repository is a historical and hardware-validation source
only. Do not extend its architecture and do not copy its release
infrastructure.

## Product model

WatermelonDS is the emulator base. Game-specific functionality is delivered
as independently versioned Enhanced add-ons:

```text
Enhanced/
  <addon-id>/
    manifest.json
    README.md
    patches/
    runtime/
```

The emulator APK must not be rebuilt for every add-on payload release.
Upstream emulator updates must be mergeable without reimplementing the
game-specific features.

## Required SM64DS add-ons

Port the known working ThorDS Super Mario 64 DS functionality into the new
package model as separate, composable add-ons:

1. **SM64DS EU right-stick analog camera**
   - exact EU ROM identity;
   - explicit right-stick X/Y ownership;
   - deadzone, inversion, sensitivity and R3 recenter;
   - versioned runtime camera protocol;
   - guarded runtime code or overlay;
   - lifecycle neutralization.

2. **SM64DS EU widescreen**
   - exact ROM/revision match;
   - game-side widescreen patch payload outside generic emulator code;
   - guarded writes with expected original values;
   - safe fallback to native 4:3 when the guard fails;
   - no renderer or compositor branch hardcoded to SM64DS.

3. **SM64DS EU 60 FPS**
   - exact ROM/revision match;
   - port the working ThorDS timing/gameplay patch as package data;
   - keep cadence, physics, timers, animation, particles, audio and save-state
     behavior explicit in the add-on contract;
   - fail closed when the runtime guard or required capability is unavailable;
   - do not expose an unvalidated overclock as a substitute for the game patch.

These are three add-ons, not one permanent SM64DS mode. They may share
generic runtime capabilities and may declare conflicts or dependencies in their
manifests.

## Generic framework requirements

The framework must support:

- game code plus ROM header identity or revision matching;
- optional SHA-256 only as an additional guard;
- safe ZIP or app-managed directory installation;
- duplicate-ID, schema, metadata, unsafe-path and malformed-patch rejection;
- Action Replay, IPS, BPS and guarded runtime overlay backends;
- temporary-copy patching without modifying the original ROM;
- explicit controller-axis ownership;
- native emulator capability selection;
- runtime input and transient state delivery;
- neutralization on pause, reset, activity replacement, save-state load,
  controller disconnect, emulator stop and session teardown;
- per-ROM enable/disable persistence with backward-compatible defaults;
- effective capability reporting;
- RetroAchievements Casual support;
- Hardcore blocking for incompatible active add-ons.

## Fork update contract

Keep remotes and branches conceptually separate:

```text
origin    -> joeblack2k/WatermelonDS-Enhanced
upstream  -> SapphireRhodonite/WatermelonDS
```

Maintain an upstream tracking branch based on `Fet_OfflineChevos`. Rebase or
merge the Enhanced branch onto upstream updates in bounded steps. Resolve
conflicts by preserving the module boundary, not by reintroducing game-specific
logic into generic emulator code.

Every upstream update must verify:

- the `:enhancements` module still builds;
- the generic native bridge still compiles;
- all add-on manifests still validate;
- the SM64DS add-ons still match only their intended revision;
- original ROM launch remains unchanged with all add-ons disabled;
- existing GBA Slot-2, Rumble, Memory Expansion and `None` modes remain intact.

## Non-negotiables

- ThorDS is not the product branch.
- No ThorDS branding, release infrastructure, debug receivers or device
  assumptions in the new fork.
- No permanent SM64DS addresses, ARM payloads or profile decisions in generic
  emulator paths.
- Original ROM files remain untouched.
- No filename-only matching.
- Unknown revisions fail closed.
- No duplicate input dispatch.
- No mandatory full-ROM hash when header/revision identity is sufficient.
- No copyrighted ROM data in the repository.
- Every non-trivial behavior has a focused runnable test.

## Acceptance evidence

Before declaring the migration complete:

- new fork exists and upstream remotes are documented;
- draft PR or equivalent review branch exists in the new fork;
- manifest, identity, package, patch and session tests pass;
- backward-compatible ROM config serialization tests pass;
- Slot-2 mapping and lifecycle tests pass;
- Hardcore incompatibility tests pass;
- all three SM64DS add-on manifests and payload contracts are present;
- a clean diff from the current upstream `Fet_OfflineChevos` base is available;
- unit tests, native build, APK assemble and lint results are recorded;
- original launch with add-ons disabled is verified;
- known ThorDS runtime/hardware evidence is mapped to the new add-on contracts;
- any unavailable physical gate is recorded explicitly rather than inferred.

Do not declare success merely because the generic framework compiles. The
three SM64DS capabilities must be represented as independently updateable
add-ons with concrete migrated payloads and guards.
