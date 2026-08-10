# SM64DS EU 60 FPS

This is the package boundary for the European Super Mario 64 DS 60 FPS
enhancement.

The addon is source-only until the revision-specific payload has been built
and validated. The ThorDS implementation is a bounded ARM9/overlay patch with
player, world, animation, particle, coin-spin and cadence changes. A generic
emulator frame limiter or ARM9 overclock is not a substitute for this patch.

The finished package must contain:

- an exact `ASMP` revision match;
- guarded hooks and payload reservations;
- consistent player/world/animation timing;
- explicit cadence and save-state behavior;
- fail-closed handling when any expected word differs;
- no copyrighted ROM or user-supplied ARM image in the repository.

Build inputs are supplied outside the repository:

1. the user's supported ARM9 and overlay images;
2. the legal source objects for the timing payload;
3. the generated Action Replay or runtime overlay payload;
4. verifier output covering every hook and payload region.

The generator and verifier should be copied into this addon once their input
paths and output format are independent of the ThorDS repository.
