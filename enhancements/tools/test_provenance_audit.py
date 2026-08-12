#!/usr/bin/env python3
"""Focused regression test for the SM64DS provenance audit."""

from __future__ import annotations

import copy
import json
import shutil
import tempfile
from pathlib import Path

from provenance_audit import audit

ROOT = Path(__file__).parents[1]


def copied_root(temp: str) -> Path:
    root = Path(temp)
    for package_id in ("sm64ds.eu.60fps", "sm64ds.eu.right-stick-camera", "sm64ds.eu.widescreen"):
        shutil.copytree(ROOT / package_id, root / package_id)
    return root


def rejected(root: Path, message: str) -> None:
    try:
        audit(root)
    except AssertionError as error:
        assert message in str(error), str(error)
    else:
        raise AssertionError(f"{message} mutation was accepted")


def main() -> int:
    assert len(audit(ROOT)) == 3
    with tempfile.TemporaryDirectory() as temp:
        root = copied_root(temp)
        fixture = root / "sm64ds.eu.widescreen"
        manifest = json.loads((fixture / "manifest.json").read_text())

        bad = copy.deepcopy(manifest)
        bad["patches"][0]["sha256"] = "0" * 64
        (fixture / "manifest.json").write_text(json.dumps(bad))
        rejected(root, "SHA-256")

    with tempfile.TemporaryDirectory() as temp:
        root = copied_root(temp)
        fixture = root / "sm64ds.eu.widescreen"
        manifest = json.loads((fixture / "manifest.json").read_text())
        manifest["patches"][0]["expectedOriginalWords"]["0x0200D03C"] = "0x00000000"
        (fixture / "manifest.json").write_text(json.dumps(manifest))
        rejected(root, "lacks payload/tool metadata")

    with tempfile.TemporaryDirectory() as temp:
        root = copied_root(temp)
        fixture = root / "sm64ds.eu.right-stick-camera"
        manifest = json.loads((fixture / "manifest.json").read_text())
        manifest["patches"][1]["expectedOriginalWords"] = {
            "0x03009E70": "0xE92D4FF0"
        }
        tool = fixture / "tools" / "pair_regression.py"
        tool.write_text("ADDRESS = 0x03009E70\nEXPECTED = 0x00000000\n")
        (fixture / "manifest.json").write_text(json.dumps(manifest))
        rejected(root, "lacks payload/tool metadata")

    with tempfile.TemporaryDirectory() as temp:
        root = copied_root(temp)
        fixture = root / "sm64ds.eu.widescreen"
        manifest = json.loads((fixture / "manifest.json").read_text())
        manifest["patches"][0]["file"] = "../outside.ards"
        (fixture / "manifest.json").write_text(json.dumps(manifest))
        rejected(root, "escapes package")

        manifest["patches"][0]["file"] = "patches/widescreen.ards"
        manifest["status"] = "VERIFIED"
        (fixture / "manifest.json").write_text(json.dumps(manifest))
        rejected(root, "status is not SOURCE_ONLY")
    print("provenance audit tests: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
