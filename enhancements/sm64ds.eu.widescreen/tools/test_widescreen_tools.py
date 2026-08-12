#!/usr/bin/env python3
"""Small stdlib regression checks for the addon capture analyzers."""

from __future__ import annotations

import json
import subprocess
import sys
import tempfile
from pathlib import Path

import presenter_trace
import surface_geometry
import transition_proof
import widescreen_proof


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def test_negative_inputs() -> None:
    trace = presenter_trace._synthetic_trace()
    trace["records"][0]["topRect"]["x"] = 401
    _, passed = presenter_trace.analyze(trace)
    check(not passed, "out-of-bounds presenter rectangle accepted")

    with tempfile.TemporaryDirectory() as name:
        root = Path(name)
        missing = root / "missing.json"
        proc = subprocess.run(
            [sys.executable, "-B", str(Path(__file__).with_name("presenter_trace.py")), str(missing)],
            capture_output=True,
            text=True,
        )
        check(proc.returncode != 0, "missing trace accepted")

        malformed = root / "malformed.json"
        malformed.write_text("{}")
        try:
            surface_geometry.analyze(malformed, 0, None)
        except ValueError:
            pass
        else:
            raise AssertionError("malformed surface manifest accepted")

        transition = transition_proof.synthetic_manifest()
        transition["frames"][3]["finalSummary"]["pixelHash64"] = "a"
        _, passed = transition_proof.analyze(transition, 6)
        check(not passed, "stale transition hash accepted")

        transition = transition_proof.synthetic_manifest()
        transition["frames"][3]["pngBytes"] = 1
        transition["frames"][3]["sourcePngBytes"] = 1
        transition["frames"][3]["finalSummary"]["width"] = 2
        transition["frames"][3]["finalSummary"]["height"] = 2
        transition["frames"][3]["sourceSummary"]["width"] = 2
        transition["frames"][3]["sourceSummary"]["height"] = 2
        surface_geometry.write_png(root / "final-3.png", 1, 1, [(1, 2, 3)])
        surface_geometry.write_png(root / "source-3.png", 1, 1, [(1, 2, 3)])
        failures = transition_proof.validate_declared_keyframes(transition["frames"], root)
        check(
            any("3:final:dimensions" in failure for failure in failures),
            "mismatched keyframe dimensions accepted",
        )

        rgba = [(0, 0, 0, 255)] * 8
        rgba[0] = (0, 0, 0, 0)
        check(not widescreen_proof.opaque(rgba), "transparent frame accepted")

        manifest = {
            "frames": [
                {
                    "index": 0,
                    "file": "absent.png",
                    "sourceFile": "absent-source.png",
                }
            ]
        }
        manifest_path = root / "frames.json"
        manifest_path.write_text(json.dumps(manifest))
        try:
            surface_geometry.analyze(manifest_path, 0, None)
        except ValueError:
            pass
        else:
            raise AssertionError("absent frame files accepted")

        unknown = root / "unknown.json"
        unknown.write_text(json.dumps({"schema": "foreign.v1"}))
        proc = subprocess.run(
            [
                sys.executable,
                "-B",
                str(Path(__file__).with_name("transition_proof.py")),
                "--manifest",
                str(unknown),
                "--capture-dir",
                str(root),
            ],
            capture_output=True,
            text=True,
        )
        check(proc.returncode != 0 and "unsupported input schema" in proc.stderr,
              "unknown transition schema accepted")

        png = root / "valid.png"
        surface_geometry.write_png(png, 1, 1, [(1, 2, 3)])
        corrupted = bytearray(png.read_bytes())
        corrupted[29] ^= 1
        png.write_bytes(corrupted)
        try:
            surface_geometry.read_png(png)
        except ValueError:
            pass
        else:
            raise AssertionError("corrupted PNG CRC accepted")


def main() -> int:
    test_negative_inputs()
    print("widescreen tools tests: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
