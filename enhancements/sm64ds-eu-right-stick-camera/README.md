# SM64DS EU Right-Stick Camera

This is the first reference Enhanced add-on. It keeps the game-specific
camera code outside the WatermelonDS emulator and declares the runtime pieces
that the emulator must provide:

- right-stick X/Y axis ownership;
- deadzone, sensitivity and recenter input;
- `sm64ds-camera-v1` protocol delivery;
- guarded runtime overlay activation;
- neutralization on reset, pause, save-state load, disconnect and teardown.

The package contains no ROM data. The runtime patch must be generated from a
locally supplied, legally owned ARM9 image and must refuse to run when any
expected original word differs. The placeholder header checksum in the
manifest must be replaced with the supported ROM's header value before the
package can match a ROM.

The current ThorDS camera source is the reference implementation to port into
this package. It is not part of the generic WatermelonDS input or launch path.
