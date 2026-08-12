# SM64DS EU Widescreen

This is the source-only package for the European Super Mario 64 DS widescreen
enhancement.

The package targets `ASMP`, revision 0, with RetroAchievements system hash
`ba3c4052e00c5cc31df5d5534c39de1b`. Its Action Replay payload has 18 canonical
lines and SHA-256
`28445a89a887a556b4a0564e21f8ca579eeab437471bff1b38c681efd6a3bbc6`.

The finished package must contain:

- an exact `ASMP` revision and RetroAchievements hash match;
- a guarded game-side aspect/culling patch;
- only generic layer-aware presentation and native emulator capabilities;
- a safe native 4:3 fallback when the game-side guard or presenter capability fails;
- no SM64DS addresses in generic emulator code.

The four runtime guards replace `0x00001555` with the 16:9 Fix12 value
`0x00001C72` at `0x0200D03C`, `0x0200F64C`, `0x02015774`, and `0x020C025C`.
The live camera aspect field is updated only behind the non-zero camera-pointer
guard at `0x0209F318`; the package contains no ROM, ARM image, object file, or
private research path.

Provenance: the payload is the revision-specific SM64DS EU aspect profile
validated by the WatermelonDS Enhanced research and test evidence. The
repository includes only generated Action Replay text and metadata; no
third-party ROM or extracted game code is redistributed.

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
