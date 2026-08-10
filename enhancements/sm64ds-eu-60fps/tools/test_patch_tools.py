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
