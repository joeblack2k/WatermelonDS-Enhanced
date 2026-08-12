#!/usr/bin/env python3
"""Focused standard-library tests for the upstream contract checker."""

from __future__ import annotations

import json
import sys
import subprocess
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from upstream_contract import check

PACKAGES = ("sm64ds.eu.60fps", "sm64ds.eu.right-stick-camera", "sm64ds.eu.widescreen")


class UpstreamContractTest(unittest.TestCase):
    def fixture(self) -> Path:
        root = Path(tempfile.mkdtemp())
        (root / "enhancements").mkdir()
        (root / "enhancements" / "build.gradle.kts").write_text("plugins {}\n")
        for package in PACKAGES:
            directory = root / "enhancements" / package
            directory.mkdir()
            (directory / "manifest.json").write_text(json.dumps({
                "status": "SOURCE_ONLY",
                "distributionStatus": "SOURCE_ONLY",
                "verification": {"payloadInput": "MISSING"},
                "patches": [{
                    "expectedOriginalWords": {
                        "0x02009E70": "0xE92D4FF0",
                    },
                }],
            }))
        subprocess.run(["git", "-C", str(root), "init", "-q"], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.email", "test@example.com"], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.name", "Test"], check=True)
        subprocess.run(["git", "-C", str(root), "add", "."], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-qm", "fixture"], check=True)
        subprocess.run(["git", "-C", str(root), "branch", "upstream/Fet_OfflineChevos"], check=True)
        return root

    def test_accepts_clean_ancestor_with_source_only_manifests(self) -> None:
        self.assertEqual(len(check(self.fixture())), 4)

    def test_rejects_dirty_tree(self) -> None:
        root = self.fixture()
        (root / "note.txt").write_text("dirty")
        with self.assertRaisesRegex(AssertionError, "clean"):
            check(root)

    def test_ignores_unrelated_address_outside_enhancements(self) -> None:
        root = self.fixture()
        (root / "note.txt").write_text("0x02012345")
        subprocess.run(["git", "-C", str(root), "add", "note.txt"], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-qm", "address"], check=True)
        self.assertEqual(len(check(root)), 4)

    def test_rejects_declared_package_address_outside_enhancements(self) -> None:
        root = self.fixture()
        (root / "note.txt").write_text("0x02009E70")
        subprocess.run(["git", "-C", str(root), "add", "note.txt"], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-qm", "address"], check=True)
        with self.assertRaisesRegex(AssertionError, "outside enhancements"):
            check(root)

    def test_rejects_non_source_only_manifest(self) -> None:
        root = self.fixture()
        manifest = root / "enhancements" / PACKAGES[0] / "manifest.json"
        data = json.loads(manifest.read_text())
        data["status"] = "VERIFIED"
        manifest.write_text(json.dumps(data))
        subprocess.run(["git", "-C", str(root), "add", "."], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-qm", "mutation"], check=True)
        with self.assertRaisesRegex(AssertionError, "SOURCE_ONLY"):
            check(root)

    def test_rejects_missing_ref(self) -> None:
        with self.assertRaisesRegex(AssertionError, "does not exist"):
            check(self.fixture(), "upstream/missing")


if __name__ == "__main__":
    unittest.main()
