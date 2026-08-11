# SM64DS EU 60 FPS

This is the package boundary for the European Super Mario 64 DS 60 FPS
enhancement.

The addon is source-only until the revision-specific payload has been built
and validated. The ThorDS implementation is a bounded ARM9/overlay patch with
player, world, animation, particle, coin-spin and cadence changes. A generic
emulator frame limiter or ARM9 overclock is not a substitute for this patch.

The finished package must contain all of these independently verified claims:

- an exact `ASMP` revision match;
- guarded hooks and payload reservations;
- cadence;
- gameplay physics;
- timers;
- animation;
- particles;
- audio continuity;
- save-state behavior;
- a verified revision-specific payload input;
- fail-closed handling when any expected word differs;
- no copyrighted ROM or user-supplied ARM image in the repository.

Build inputs are supplied outside the repository:

1. the user's supported ARM9 and overlay images;
2. the legal source objects for the timing payload;
3. the generated Action Replay or runtime overlay payload;
4. verifier output covering every hook and payload region.

The generator and verifier are present in `tools/`. Their tooling test is
input-free and checks their shared constants, branch encoding, and strict
five-region parser:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 tools/test_patch_tools.py
```

The end-to-end tool test remains private and requires legal, revision-matched
ARM9 and overlay images. It generates only temporary objects and patch output:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 tools/test_patch.py \
  --arm9-image /private/path/arm9_dec.bin \
  --overlay2-image /private/path/overlay_0002.bin
```

This is tooling-only validation. It does not generate or install a payload for
the addon, and it does not validate a ROM revision without the user's legal
input images. Cadence-only evidence, emulator overclock evidence, missing
guards, missing payload input, or a source-only manifest are rejected as a
concrete addon. The current gate is explicit: both image arguments are required
and must point to existing files; `llvm-mc` must also be available on `PATH`.

The acceptance gate that remains blocked is a legal, revision-matched ROM/ARM9
and overlay input plus a real run on the target Thor hardware. Until that run
exists, the seven claims above remain unverified and this directory is not a
release addon.
