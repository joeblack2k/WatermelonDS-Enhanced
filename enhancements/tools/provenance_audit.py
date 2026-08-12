#!/usr/bin/env python3
"""Audit committed SM64DS add-on provenance and package boundaries."""

from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

PACKAGE_IDS = (
    "sm64ds.eu.60fps",
    "sm64ds.eu.right-stick-camera",
    "sm64ds.eu.widescreen",
)


def _fail(message: str) -> None:
    raise AssertionError(message)


def _words(path: Path) -> list[tuple[str, str]]:
    return [tuple(line.split()) for line in path.read_text(encoding="ascii").splitlines() if line]


def _tool_words(package: Path) -> set[tuple[str, str]]:
    """Read paired address/word literals from existing verifier tools."""
    found = set()
    for tool in (package / "tools").glob("*.py"):
        text = tool.read_text(encoding="ascii")
        constants = {
            name: value.lower()
            for name, value in re.findall(
                r"([A-Z][A-Z0-9_]*)\s*=\s*(0x[0-9A-Fa-f]{8})", text
            )
        }
        for address_name, address in constants.items():
            for word_name, word in constants.items():
                if (
                    address_name in word_name
                    and ("HOOK" in address_name or "ADDRESS" in address_name)
                    and ("EXPECTED" in word_name or "ORIGINAL" in word_name)
                ):
                    found.add((address, word))
        for address, word in re.findall(
            r"\(\s*(0x[0-9A-Fa-f]{8})\s*,\s*(0x[0-9A-Fa-f]{8})\s*,\s*0x[0-9A-Fa-f]{8}\s*\)",
            text,
        ):
            found.add((address.lower(), word.lower()))
        for address_name, address in constants.items():
            if "ADDRESS" in address_name:
                match = re.search(
                    rf"{re.escape(address_name)}.*?if original != 0x([0-9A-Fa-f]{{8}})",
                    text,
                    re.DOTALL,
                )
                if match:
                    found.add((address, f"0x{match.group(1).lower()}"))
        for line in text.splitlines():
            values = re.findall(r"0x[0-9A-Fa-f]{8}", line)
            found.update((address.lower(), word.lower()) for address, word in zip(values[::2], values[1::2]))
    return found


def audit(root: Path) -> list[str]:
    results = []
    for package_id in PACKAGE_IDS:
        package = root / package_id
        manifest_path = package / "manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="ascii"))
        if manifest.get("status") != "SOURCE_ONLY":
            _fail(f"{package_id}: status is not SOURCE_ONLY")
        if manifest.get("distributionStatus") != "SOURCE_ONLY":
            _fail(f"{package_id}: distributionStatus is not SOURCE_ONLY")
        verification = manifest.get("verification")
        if not isinstance(verification, dict) or verification.get("payloadInput") != "MISSING":
            _fail(f"{package_id}: payloadInput boundary is not explicit SOURCE_ONLY/MISSING")

        patches = manifest.get("patches")
        if not isinstance(patches, list) or not patches:
            _fail(f"{package_id}: no declared payloads")
        for patch in patches:
            provenance = patch.get("provenance")
            if not isinstance(provenance, str) or not provenance.strip():
                _fail(f"{package_id}: payload has no provenance statement")
            declared = patch.get("file")
            if not isinstance(declared, str) or not declared:
                _fail(f"{package_id}: payload path is missing")
            payload = (package / declared).resolve()
            try:
                payload.relative_to(package.resolve())
            except ValueError:
                _fail(f"{package_id}: payload escapes package: {declared}")
            if not payload.is_file():
                _fail(f"{package_id}: payload is missing: {declared}")
            actual_hash = hashlib.sha256(payload.read_bytes()).hexdigest()
            if patch.get("sha256") != actual_hash:
                _fail(f"{package_id}: SHA-256 mismatch: {declared}")

            expected = patch.get("expectedOriginalWords", {})
            if not isinstance(expected, dict):
                _fail(f"{package_id}: expectedOriginalWords is not an object")
            # AR guards are checked from the payload. Camera originals are
            # intentionally checked against its existing ARM9-image verifier
            # metadata because that package cannot prove them from AR text alone.
            if expected:
                guarded = {
                    (int(address[1:], 16) & 0x0FFFFFFF, int(word, 16))
                    for address, word in _words(payload)
                    if len(address) == 8 and address.startswith("52")
                }
                tool_words = {(address.lower(), word.lower()) for address, word in _tool_words(package)}
                for address, word in expected.items():
                    if (
                        not isinstance(address, str)
                        or not isinstance(word, str)
                        or len(address) != 10
                        or len(word) != 10
                        or not address.startswith("0x")
                        or not word.startswith("0x")
                    ):
                        _fail(f"{package_id}: malformed expected original word: {address}")
                    key = (address.lower(), word.lower())
                    ar_key = (int(address, 16) & 0x0FFFFFFF, int(word, 16))
                    if ar_key in guarded or (
                        key in tool_words
                    ):
                        continue
                    _fail(f"{package_id}: expected word lacks payload/tool metadata: {address}")
        results.append(package_id)
    return results


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    print("provenance audit: PASS", ", ".join(audit(args.root)))
