import unittest
import hashlib
import json
from pathlib import Path

import build_patch
import verify_patch


class CameraPatchToolsTest(unittest.TestCase):
    def test_reference_payload_hash_and_line_count_are_canonical(self) -> None:
        root = Path(__file__).parents[1]
        payload = root / "patches" / "camera.ards"
        manifest = json.loads((root / "manifest.json").read_text())
        camera_patch = next(p for p in manifest["patches"] if p["file"] == "patches/camera.ards")
        self.assertEqual(61, len(payload.read_text(encoding="ascii").splitlines()))
        self.assertEqual(camera_patch["sha256"], hashlib.sha256(payload.read_bytes()).hexdigest())

    def test_target_bridge_only_removes_eq_condition(self) -> None:
        self.assertEqual(build_patch.TARGET_BRIDGE_PATCHES, verify_patch.TARGET_BRIDGE_PATCHES)
        for _, original, replacement in build_patch.TARGET_BRIDGE_PATCHES:
            self.assertEqual(0, original >> 28)
            self.assertEqual(0xE, replacement >> 28)
            self.assertEqual(original & 0x0FFFFFFF, replacement & 0x0FFFFFFF)

    def test_generic_hook_still_targets_camera_payload(self) -> None:
        branch = build_patch.arm_branch(build_patch.HOOK, build_patch.PAYLOAD)
        self.assertEqual(build_patch.PAYLOAD, verify_patch.branch_target(build_patch.HOOK, branch))

if __name__ == "__main__":
    unittest.main()
