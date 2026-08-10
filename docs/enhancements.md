# Enhanced Add-ons

Enhanced add-ons are opt-in packages for game-specific improvements that cannot
be represented by normal emulator settings alone. An add-on is selected only
when its manifest matches the ROM identity read from the cartridge header.

## Manifest

```json
{
  "schemaVersion": 1,
  "id": "mario-camera",
  "name": "SM64DS right-stick camera",
  "version": "1.0.0",
  "match": {
    "gameCode": "ASMP",
    "headerChecksum": "12345678"
  },
  "capabilities": [
    "CONTROLLER_AXIS_OWNER",
    "RUNTIME_INPUT_PROTOCOL",
    "RUNTIME_CODE_PATCH"
  ],
  "patches": [
    {
      "type": "RUNTIME_OVERLAY",
      "file": "runtime/camera.overlay",
      "apply": "RUNTIME"
    }
  ],
  "hardcoreCompatible": false
}
```

Installed add-ons use a canonical filesystem layout:
`<Enhancements root>/<manifest.id>/manifest.json`. The package directory name
must exactly equal the manifest `id`. Discovery checks only direct, visible
package directories under each configured root; nested manifests and
dot-prefixed staging directories are ignored.

Import rejects an already installed `id` by default. App-managed imports may
explicitly opt into replacement; the incoming package is extracted and fully
validated first, then promoted in the same root. The previous package is kept
as a hidden temporary backup until promotion succeeds and is restored if
promotion fails. Staging and backup directories are removed before the import
returns.

The manifest is deliberately broader than an Action Replay file. An add-on
may combine controller-axis ownership, a native emulator capability, a
versioned runtime input protocol, guarded runtime code, or an IPS/BPS patch
against temporary runtime data.

The right-stick camera is the reference use case: the add-on owns the right
stick, sends deadzone-filtered input through a runtime protocol, and activates
game-side code only for the matching ROM revision. It must also neutralize its
state on reset, pause, save-state load, controller disconnect, and teardown.

## Safety rules

- Match game code and header identity data; filenames are never sufficient.
- Allow an optional SHA-256 as an additional guard for revisions that need it.
- Reject unsafe paths and unknown manifest fields.
- Keep the original ROM untouched.
- Validate patch bounds and expected original values before applying runtime code.
- Runtime Action Replay and overlay patches must use `RUNTIME`; IPS and BPS
  patches must use `TEMPORARY_COPY`. Other type/apply pairs are rejected.
- Surface active add-ons and their capabilities in the launch/session state.
- Block incompatible RetroAchievements Hardcore sessions before emulation starts.

The current implementation provides manifest parsing, validation, exact
identity matching, package import, temporary ROM patching, runtime Action
Replay, guarded runtime overlays, and a session/lifecycle contract. Native
capabilities remain explicit bridges selected by the active session rather
than game-specific branches in the emulator.

An active session is created only from manifests that match the current ROM.
Enabling an unknown or mismatched add-on is rejected before emulation starts.
The session exposes the union of its capabilities and whether all active
add-ons are compatible with RetroAchievements Hardcore mode.

## Private ROM identity tool

For private local use, inspect a regular Nintendo DS ROM with:

```sh
python3 enhancements/tools/rom_identity.py /private/game.nds
```

The tool reads only the first `0x200` bytes and the filesystem size. It validates
the game code, DS unit code, stored header CRC16, declared ROM size, and ARM9
and ARM7 bounds before emitting exactly `gameCode` and `headerChecksum` as JSON.
`headerChecksum` is the uppercase eight-digit value
`(~binascii.crc32(header)) & 0xffffffff`; it is an identity fingerprint, not
the Nintendo DS header's stored CRC16. It never emits a path, title, SHA, or ROM
bytes, and does not modify the ROM.
