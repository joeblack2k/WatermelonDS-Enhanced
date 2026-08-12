#!/usr/bin/env python3
"""Small input-free tests for the copied patch toolchain."""
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).parent


def load(name: str, filename: str):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


build = load("test_patch_build", "build_patch.py")
verify = load("test_patch_verify", "verify_patch.py")


class PatchToolsTest(unittest.TestCase):
    def test_verified_manifest_binds_exact_patch_file_type_and_hash(self):
        import hashlib
        import json

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            patch = root / "payload.ards"
            patch.write_text("02000000 00000001\n", encoding="ascii")
            manifest = {
                "status": "VERIFIED",
                "patches": [{
                    "type": "ACTION_REPLAY",
                    "file": patch.name,
                    "sha256": hashlib.sha256(patch.read_bytes()).hexdigest(),
                }],
                "verification": {
                    claim: "VERIFIED"
                    for claim in (*verify.CONTRACT_CLAIMS, "guardedPayload")
                } | {"payloadInput": "VERIFIED"},
            }
            path = root / "manifest.json"
            path.write_text(json.dumps(manifest), encoding="ascii")
            verify.verify_manifest_contract(path, patch)
            patch.write_text("D0000000 00000000\n", encoding="ascii")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                verify.verify_manifest_contract(path, patch)

    def test_source_only_manifest_is_not_a_concrete_addon(self):
        with self.assertRaisesRegex(ValueError, "unverified contract claims"):
            verify.verify_manifest_contract(HERE.parent / "manifest.example.json")

    def test_source_only_manifest_with_missing_payload_input_fails_closed(self):
        import json

        manifest_path = HERE.parent / "manifest.json"
        patch_path = HERE.parent / "patches" / "60fps-v10.ards"
        with self.assertRaisesRegex(ValueError, "unverified contract claims"):
            verify.verify_manifest_contract(manifest_path, patch_path)
        self.assertEqual(len(patch_path.read_text(encoding="ascii").splitlines()), 937)

    def test_five_regions_and_no_camera_or_widescreen_guard_overlap(self):
        import json

        payload = HERE.parent / "patches" / "60fps-v10.ards"
        regions = verify.parse(payload)
        self.assertEqual([len(guards) for guards, _ in regions], [13, 87, 87, 173, 174])
        self.assertEqual([len(writes) for _, writes in regions], [174, 108, 108, 2, 1])

        own = {
            address.lower()
            for address in json.loads((HERE.parent / "manifest.json").read_text())["patches"][0]
            ["expectedOriginalWords"]
        }
        for package in ("sm64ds.eu.right-stick-camera", "sm64ds.eu.widescreen"):
            manifest = json.loads(
                (HERE.parent.parent / package / "manifest.json").read_text()
            )
            other = {
                address.lower()
                for patch in manifest["patches"]
                for address in patch.get("expectedOriginalWords", {})
            }
            self.assertTrue(own.isdisjoint(other), package)

    def test_contract_rejects_cadence_only_and_missing_payload(self):
        import json

        manifest = json.loads(
            (HERE.parent / "manifest.example.json").read_text(encoding="ascii")
        )
        manifest["status"] = "VERIFIED"
        manifest["verification"]["cadence"] = "VERIFIED"
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "manifest.json"
            path.write_text(json.dumps(manifest), encoding="ascii")
            with self.assertRaisesRegex(ValueError, "unverified contract claims"):
                verify.verify_manifest_contract(path)
            manifest["verification"] = {
                claim: "VERIFIED"
                for claim in (*verify.CONTRACT_CLAIMS, "guardedPayload")
            } | {"payloadInput": "MISSING"}
            path.write_text(json.dumps(manifest), encoding="ascii")
            with self.assertRaisesRegex(ValueError, "payload input"):
                verify.verify_manifest_contract(path)

    def test_contract_rejects_overclock_only(self):
        import json

        manifest = json.loads(
            (HERE.parent / "manifest.example.json").read_text(encoding="ascii")
        )
        manifest["status"] = "VERIFIED"
        manifest["capabilities"] = ["EMULATOR_OVERCLOCK"]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "manifest.json"
            path.write_text(json.dumps(manifest), encoding="ascii")
            with self.assertRaisesRegex(ValueError, "overclock"):
                verify.verify_manifest_contract(path)

    def test_shared_constants(self):
        for name in (
            "ARM9_BASE",
            "OVERLAY_BASE",
            "PLAYER_PAYLOAD",
            "WORLD_PAYLOAD",
            "WORLD_LIMIT",
            "CADENCE",
            "ANIMATION_HOOK",
            "COIN_SPIN_HOOK",
            "COIN_SPIN_HALF",
            "PLAYER_HOOKS",
            "WORLD_HOOKS",
        ):
            self.assertEqual(getattr(build, name), getattr(verify, name), name)
        self.assertEqual(build.PLAYER_LIMIT, verify.PLAYER_BYTES)
        self.assertEqual(build.COIN_SPIN_EXPECTED, verify.COIN_SPIN_ORIGINAL)
        self.assertEqual(build.ANIMATION_EXPECTED, verify.ANIMATION_ORIGINAL)
        self.assertEqual(build.PLAYER_EXPECTED, verify.PLAYER_ORIGINALS)
        self.assertEqual(build.WORLD_EXPECTED, verify.WORLD_ORIGINALS)

    def test_branch_round_trip_and_rejects(self):
        for link in (False, True):
            encoded = build.branch(0x02010000, 0x02010100, link)
            self.assertEqual(verify.branch_target(0x02010000, encoded), 0x02010100)
        with self.assertRaises(ValueError):
            build.branch(0x02010000, 0x02010002)
        with self.assertRaises(ValueError):
            build.branch(0x02010000, 0x06010000)
        with self.assertRaises(ValueError):
            verify.branch_target(0x02010000, 0xE0000000)

    def test_parse_requires_five_ordered_regions_and_resets(self):
        region = ["50000000 00000001", "02004000 00000002"]
        text = "\n".join(
            line
            for _ in range(5)
            for line in (*region, "D0000000 00000000", "D2000000 00000000")
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "synthetic.txt"
            path.write_text(text + "\n", encoding="ascii")
            parsed = verify.parse(path)
            self.assertEqual(len(parsed), 5)
            self.assertEqual(parsed[0], ([(0, 1)], [(0x02004000, 2)]))

            malformed = path.with_name("malformed.txt")
            malformed.write_text(
                "\n".join(region + ["D2000000 00000000", "D0000000 00000000"] * 5)
                + "\n",
                encoding="ascii",
            )
            with self.assertRaises(ValueError):
                verify.parse(malformed)

            missing_reset = path.with_name("missing-reset.txt")
            missing_reset.write_text(
                "\n".join(region + ["D0000000 00000000"] * 5) + "\n",
                encoding="ascii",
            )
            with self.assertRaises(ValueError):
                verify.parse(missing_reset)


if __name__ == "__main__":
    unittest.main()
