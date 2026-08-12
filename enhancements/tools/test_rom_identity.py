#!/usr/bin/env python3
from __future__ import annotations

import binascii
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
import rom_identity

NO_BYTECODE_ENV = {**os.environ, "PYTHONDONTWRITEBYTECODE": "1"}


def make_rom(extra: int = 0) -> bytearray:
    header = bytearray(0x200)
    header[0x0C:0x10] = b"ASMP"
    header[0x12] = 0
    header[0x14] = 0
    header[0x20:0x24] = (0x400).to_bytes(4, "little")
    header[0x2C:0x30] = (0x100).to_bytes(4, "little")
    header[0x30:0x34] = (0x600).to_bytes(4, "little")
    header[0x3C:0x40] = (0x100).to_bytes(4, "little")
    return finalize(header) + bytearray(0x20000 - len(header) + extra)


def finalize(header: bytearray) -> bytearray:
    header[0x15E:0x160] = rom_identity.header_crc16(header).to_bytes(2, "little")
    return header


class RomIdentityTest(unittest.TestCase):
    def write(self, root: Path, data: bytes) -> Path:
        path = root / "game.nds"
        path.write_bytes(data)
        return path

    def test_identity_and_validation(self) -> None:
        self.assertEqual(rom_identity.header_crc16(bytes(0x200)), 0x1BCC)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = self.write(root, make_rom())
            expected = {
                "gameCode": "ASMP",
                "headerChecksum": f"{(~binascii.crc32(bytes(make_rom()[:0x200]))) & 0xFFFFFFFF:08X}",
            }
            self.assertEqual(rom_identity.read_identity(path), expected)
            self.assertEqual(rom_identity.read_identity(self.write(root, make_rom(99))), expected)
            changed = make_rom()
            changed[0x100] ^= 1
            finalize(changed)
            self.assertNotEqual(rom_identity.read_identity(self.write(root, changed))["headerChecksum"], expected["headerChecksum"])
            proc = subprocess.run(
                [sys.executable, "-B", str(Path(rom_identity.__file__)), str(path)],
                capture_output=True,
                text=True,
                env=NO_BYTECODE_ENV,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(set(json.loads(proc.stdout)), {"gameCode", "headerChecksum"})
            self.assertNotIn(str(path), proc.stdout)

    def test_rejections(self) -> None:
        cases = []
        base = make_rom()
        cases.append(("truncated", base[:0x1FF]))
        malformed = bytearray(base); malformed[0x0C] = 0xFF; finalize(malformed); cases.append(("malformed code", malformed))
        unsupported = bytearray(base); unsupported[0x12] = 1; finalize(unsupported); cases.append(("unit", unsupported))
        bad_crc = bytearray(base); bad_crc[0x15E] ^= 1; cases.append(("CRC", bad_crc))
        impossible = bytearray(base); impossible[0x14] = 8; finalize(impossible); cases.append(("size", impossible))
        arm9 = bytearray(base); arm9[0x20:0x24] = (0x1FFFF).to_bytes(4, "little"); finalize(arm9); cases.append(("ARM9", arm9))
        arm7 = bytearray(base); arm7[0x30:0x34] = (0x1FFFF).to_bytes(4, "little"); finalize(arm7); cases.append(("ARM7", arm7))
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for label, data in cases:
                with self.subTest(label=label):
                    path = self.write(root, data)
                    proc = subprocess.run(
                        [sys.executable, "-B", str(Path(rom_identity.__file__)), str(path)],
                        capture_output=True,
                        text=True,
                        env=NO_BYTECODE_ENV,
                    )
                    self.assertNotEqual(proc.returncode, 0)
                    self.assertTrue(proc.stderr.strip())
                    with self.assertRaises(ValueError):
                        rom_identity.read_identity(path)

            proc = subprocess.run(
                [sys.executable, "-B", str(Path(rom_identity.__file__))],
                capture_output=True,
                text=True,
                env=NO_BYTECODE_ENV,
            )
            self.assertEqual(proc.returncode, 2)
            self.assertTrue(proc.stderr.strip())


if __name__ == "__main__":
    unittest.main()
