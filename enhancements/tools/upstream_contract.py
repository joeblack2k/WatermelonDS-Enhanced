#!/usr/bin/env python3
"""Read-only contract checks for updating the WatermelonDS Enhanced fork."""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

UPSTREAM_REF = "upstream/Fet_OfflineChevos"
PACKAGE_IDS = (
    "sm64ds.eu.60fps",
    "sm64ds.eu.right-stick-camera",
    "sm64ds.eu.widescreen",
)
SM64DS_ADDRESS = re.compile(r"\b0x020[0-9a-fA-F]{5}\b")


def _git(root: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(root), *args],
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode:
        raise AssertionError(result.stderr.strip() or f"git {' '.join(args)} failed")
    return result.stdout.strip()


def _package_addresses(root: Path, package_id: str, manifest: dict) -> set[str]:
    addresses = {
        address.lower()
        for patch in manifest.get("patches", [])
        for address in patch.get("expectedOriginalWords", {})
        if isinstance(address, str)
    }
    package_root = root / "enhancements" / package_id
    for source_root in (package_root / "tools", package_root / "runtime"):
        if not source_root.is_dir():
            continue
        for path in source_root.rglob("*"):
            if path.is_file():
                text = path.read_text(encoding="utf-8", errors="ignore")
                addresses.update(match.lower() for match in SM64DS_ADDRESS.findall(text))
    return addresses


def check(root: Path, upstream_ref: str = UPSTREAM_REF) -> list[str]:
    root = root.resolve()
    try:
        _git(root, "rev-parse", "--verify", f"{upstream_ref}^{{commit}}")
    except AssertionError as error:
        raise AssertionError(f"upstream ref does not exist: {upstream_ref}") from error
    if _git(root, "status", "--porcelain"):
        raise AssertionError("working tree is not clean")
    if not (root / "enhancements" / "build.gradle.kts").is_file():
        raise AssertionError("enhancements module is missing")

    package_addresses = set()
    for package_id in PACKAGE_IDS:
        manifest_path = root / "enhancements" / package_id / "manifest.json"
        if not manifest_path.is_file():
            raise AssertionError(f"manifest is missing: {package_id}")
        manifest = json.loads(manifest_path.read_text(encoding="ascii"))
        package_addresses.update(_package_addresses(root, package_id, manifest))
        if manifest.get("status") != "SOURCE_ONLY":
            raise AssertionError(f"{package_id}: status is not SOURCE_ONLY")
        if manifest.get("distributionStatus") != "SOURCE_ONLY":
            raise AssertionError(f"{package_id}: distributionStatus is not SOURCE_ONLY")
        verification = manifest.get("verification")
        if not isinstance(verification, dict) or verification.get("payloadInput") != "MISSING":
            raise AssertionError(f"{package_id}: payloadInput is not MISSING")

    files = _git(root, "ls-files", "-z").split("\0")
    offenders = []
    for name in files:
        if not name or name.startswith("enhancements/"):
            continue
        path = root / name
        if path.is_file():
            text = path.read_text(encoding="utf-8", errors="ignore")
            if any(address.lower() in package_addresses for address in SM64DS_ADDRESS.findall(text)):
                offenders.append(name)
    if offenders:
        raise AssertionError("SM64DS package address literal outside enhancements/: " + ", ".join(offenders))

    if not _git(root, "merge-base", "HEAD", upstream_ref):
        raise AssertionError(f"current branch has no merge-base with {upstream_ref}")
    return [upstream_ref, *PACKAGE_IDS]


def main(argv: list[str]) -> int:
    if not 2 <= len(argv) <= 3:
        print(f"usage: {argv[0]} ROOT [UPSTREAM_REF]", file=sys.stderr)
        return 2
    try:
        checked = check(Path(argv[1]), argv[2] if len(argv) == 3 else UPSTREAM_REF)
    except (AssertionError, OSError, json.JSONDecodeError) as error:
        print(f"upstream contract: FAIL: {error}", file=sys.stderr)
        return 1
    print("upstream contract: PASS (" + ", ".join(checked) + ")")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
