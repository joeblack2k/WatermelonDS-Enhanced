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

## Capture tooling

The four tools in `tools/` validate user-supplied presenter traces, paired
surface captures, widescreen geometry/culling, and transition sequences. They
use only the Python standard library and implement their own PNG decoding.
Normal runs require explicit manifest and PNG paths; there is no repository or
local-project evidence directory. `--self-test` is the only input-free mode
and creates all synthetic inputs in a temporary directory.

Existing ThorDS v1 manifests/traces are accepted only when the corresponding
explicit legacy-input option is supplied. Reports always use addon-owned
schemas. Capture inputs are legal user-supplied files kept outside Git; do
not add captures, generated reports, or fixtures here.

Passing these tools proves only that the supplied captures satisfy the
measurement checks. It does not prove ROM identity, patch payload correctness,
runtime behavior, or physical-device behavior.
