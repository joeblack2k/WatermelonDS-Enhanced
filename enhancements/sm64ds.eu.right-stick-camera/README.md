# SM64DS EU Right-Stick Camera

This is the first reference Enhanced add-on. It keeps the game-specific
camera code outside the WatermelonDS emulator and declares the runtime pieces
that the emulator must provide:

- right-stick X/Y axis ownership;
- deadzone, sensitivity and recenter input;
- `sm64ds-camera-v1` protocol delivery;
- guarded runtime overlay activation;
- neutralization on reset, pause, save-state load, disconnect and teardown.

The 61-line camera Action Replay payload remains present as reference metadata,
but this addon is source-only for R3 recenter. The existing payload has no
verified edge-trigger implementation and must not be installed as if it did.
No ROM, ARM9 image, object file, or private path is distributed.

The checked-in assembly source remains a rebuild input. A future R3 fix must
read Slot-2 offset `0x06`, compare it with a proven persistent addon-state
slot, update that slot, and reset yaw only on a new sequence. The existing
evidence does not prove such a slot or the required payload words, so the
builder must fail closed until a supported ARM9 image and object are supplied.
The emulator owns lifecycle neutralization and axis delivery; this package owns
only the generic camera protocol and payload selection.

The analog code is attributed to AM64DS Europe by LRFLEW. Redistribution
licensing of that upstream Action Replay payload is not established by this
repository, so release redistribution requires a license confirmation. The
camera payload provenance is ThorDS project metadata.
