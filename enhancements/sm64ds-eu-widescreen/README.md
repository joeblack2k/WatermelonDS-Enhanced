# SM64DS EU Widescreen

This is the package boundary for the European Super Mario 64 DS widescreen
enhancement.

The addon is intentionally not installable yet. The existing ThorDS evidence
proves the presentation path, but the distributable game-side patch and its
revision-specific expected-word map still need to be generated from the
supported ROM revision. Do not add a guessed patch or a full ROM to this
repository.

The finished package must contain:

- an exact `ASMP` revision match;
- a guarded game-side aspect/culling patch;
- a runtime capability declaration for layer-aware presentation;
- a safe 4:3 fallback when the game-side guard or presenter capability fails;
- no SM64DS addresses in generic emulator code.

Build inputs are supplied outside the repository:

1. the user's supported ARM9/overlay images;
2. the revision-specific symbol/expected-word map;
3. the generated patch and verifier output.

The evidence and derivation notes remain in the local project research dossier.
Only generated patch metadata and code that is legal to redistribute belong in
this addon.
