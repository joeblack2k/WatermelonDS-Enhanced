# Enhanced Add-ons

Enhanced add-ons are opt-in packages for game-specific improvements that cannot
be represented by normal emulator settings alone. An add-on is selected only
when its manifest matches the exact ROM identity.

## Manifest

```json
{
  "schemaVersion": 1,
  "id": "mario-camera",
  "name": "SM64DS right-stick camera",
  "version": "1.0.0",
  "match": {
    "gameCode": "ASMP",
    "sha256": ["<exact ROM SHA-256>"]
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

- Match game code and an exact SHA-256; filenames are never sufficient.
- Reject unsafe paths and unknown manifest fields.
- Keep the original ROM untouched.
- Validate patch bounds and expected original values before applying runtime code.
- Surface active add-ons and their capabilities in the launch/session state.
- Block incompatible RetroAchievements Hardcore sessions before emulation starts.

The current implementation provides manifest parsing, validation, and exact
identity matching. Runtime capability execution and package import are
deliberately separate follow-up work so each backend can be reviewed safely.
