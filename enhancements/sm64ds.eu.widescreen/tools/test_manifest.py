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
EXPECTED_LINES = [
    "5200D03C 00001555", "0200D03C 00001C72", "D0000000 00000000",
    "5200F64C 00001555", "0200F64C 00001C72", "D0000000 00000000",
    "52015774 00001555", "02015774 00001C72", "D0000000 00000000",
    "520C025C 00001555", "020C025C 00001C72", "D0000000 00000000",
    "B209F318 00000000", "50000000 02086F84", "DC000000 000000F8",
    "00000000 00001C72", "D0000000 00000000", "D2000000 00000000",
]

def validate_payload(lines: list[str], expected_hash: str) -> None:
    assert lines == EXPECTED_LINES
    assert len(lines) == 18
    assert len([line for line in lines if line.startswith("52")]) == 4
    for address, word in GUARDS.items():
        index = lines.index("5" + address[3:] + " " + word[2:])
        assert lines[index + 1] == address[2:] + " 00001C72"
        assert lines[index + 2] == "D0000000 00000000"
    assert lines[12:18] == EXPECTED_LINES[12:18]
    assert lines[-1] == "D2000000 00000000"
    assert hashlib.sha256(("\n".join(lines) + "\n").encode()).hexdigest() == expected_hash


def main() -> int:
    manifest = json.loads((ROOT / "manifest.json").read_text())
    patch = manifest["patches"][0]
    assert manifest["match"]["gameCode"] == "ASMP"
    assert manifest["match"]["revision"] == 0
    assert manifest["match"]["raHashes"] == ["ba3c4052e00c5cc31df5d5534c39de1b"]
    assert manifest["capabilities"] == ["LAYER_AWARE_PRESENTATION", "RUNTIME_CODE_PATCH"]
    assert manifest.get("requiresCapabilities", []) == []
    assert manifest["distributionStatus"] == "SOURCE_ONLY"
    assert all("THOR" not in capability for capability in manifest["capabilities"])
    assert manifest["id"] not in manifest.get("conflictsWith", [])
    camera = json.loads(
        (ROOT.parent / "sm64ds.eu.right-stick-camera" / "manifest.json").read_text()
    )
    assert set(manifest["capabilities"]) & set(camera.get("capabilities", [])) == {
        "RUNTIME_CODE_PATCH"
    }
    assert patch["expectedOriginalWords"] == GUARDS
    assert patch["type"] == "ACTION_REPLAY"
    assert patch["apply"] == "RUNTIME"
    assert len(GUARDS) == len(set(GUARDS))
    payload = ROOT / "patches" / "widescreen.ards"
    lines = payload.read_text().splitlines()
    validate_payload(lines, SHA256)
    for bad_lines, bad_hash in (
        (lines + ["02000000 00000000"], SHA256),
        (lines[:3] + lines[:3] + lines[6:], SHA256),
        (lines[:1] + ["5200D03C 00001554"] + lines[2:], SHA256),
        (lines, "0" * 64),
    ):
        try:
            validate_payload(bad_lines, bad_hash)
        except AssertionError:
            pass
        else:
            raise AssertionError("synthetic invalid widescreen payload was accepted")
    assert patch["sha256"] == SHA256
    print("widescreen manifest tests: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
