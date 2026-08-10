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
- Surface active add-ons and their capabilities in the launch/session state.
- Block incompatible RetroAchievements Hardcore sessions before emulation starts.

The current implementation provides manifest parsing, validation, and exact
identity matching, plus a session/lifecycle contract. Runtime capability
execution and package import are deliberately separate follow-up work so each
backend can be reviewed safely.

An active session is created only from manifests that match the current ROM.
Enabling an unknown or mismatched add-on is rejected before emulation starts.
The session exposes the union of its capabilities and whether all active
add-ons are compatible with RetroAchievements Hardcore mode.
