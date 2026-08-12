#!/usr/bin/env python3
"""Regression checks for the source-only widescreen package contract."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).parents[1]
SHA256 = "28445a89a887a556b4a0564e21f8ca579eeab437471bff1b38c681efd6a3bbc6"
GUARDS = {
    "0x0200D03C": "0x00001555",
    "0x0200F64C": "0x00001555",
    "0x02015774": "0x00001555",
    "0x020C025C": "0x00001555",
}


def main() -> int:
    manifest = json.loads((ROOT / "manifest.json").read_text())
    patch = manifest["patches"][0]
    assert manifest["match"]["gameCode"] == "ASMP"
    assert manifest["match"]["revision"] == 0
    assert manifest["match"]["raHashes"] == ["ba3c4052e00c5cc31df5d5534c39de1b"]
    assert manifest["capabilities"] == ["LAYER_AWARE_PRESENTATION"]
    assert manifest.get("requiresCapabilities", []) == []
    assert manifest["distributionStatus"] == "SOURCE_ONLY"
    assert all("THOR" not in capability for capability in manifest["capabilities"])
    assert manifest["id"] not in manifest.get("conflictsWith", [])
    camera = json.loads(
        (ROOT.parent / "sm64ds.eu.right-stick-camera" / "manifest.json").read_text()
    )
    assert not (
        set(manifest["capabilities"]) & set(camera.get("capabilities", []))
    )
    assert patch["expectedOriginalWords"] == GUARDS
    assert patch["type"] == "ACTION_REPLAY"
    assert patch["apply"] == "RUNTIME"
    assert len(GUARDS) == len(set(GUARDS))
    payload = ROOT / "patches" / "widescreen.ards"
    assert len(payload.read_text().splitlines()) == 18
    assert hashlib.sha256(payload.read_bytes()).hexdigest() == SHA256
    assert patch["sha256"] == SHA256
    print("widescreen manifest tests: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
