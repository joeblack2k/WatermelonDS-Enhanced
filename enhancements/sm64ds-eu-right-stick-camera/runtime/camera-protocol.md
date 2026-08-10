# `sm64ds-camera-v1`

The protocol is a small transient state block owned by the active enhancement.

| Field | Meaning |
| --- | --- |
| `yawQ12` | Signed yaw input in Q4.12 |
| `pitchQ12` | Signed pitch input in Q4.12 |
| `yawUnitsPerTick` | Sensitivity scalar |
| `recenterSequence` | Incremented for each R3 recenter request |
| `flags` | Bit 0 means the camera capability is enabled |

All fields must be neutralized when the enhancement session is paused, reset,
reloaded from a save state, disconnected from its controller, or closed.
