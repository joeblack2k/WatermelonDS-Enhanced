#!/usr/bin/env python3
"""Read and validate the identity-bearing Nintendo DS header."""

from __future__ import annotations

import binascii
import json
import os
import stat
import sys
from pathlib import Path

HEADER_SIZE = 0x200
HEADER_CRC_END = 0x15E
DS_UNIT_CODES = {0x00, 0x02}


def header_crc16(header: bytes) -> int:
    value = 0xFFFF
    for byte in header[:HEADER_CRC_END]:
        value ^= byte
        for _ in range(8):
            value = ((value >> 1) ^ 0xA001) & 0xFFFF if value & 1 else value >> 1
    return value


def read_identity(path: str | os.PathLike[str]) -> dict[str, str]:
    file_path = Path(path)
    try:
        with file_path.open("rb") as stream:
            metadata = os.fstat(stream.fileno())
            if not stat.S_ISREG(metadata.st_mode):
                raise ValueError("not a regular file")
            size = metadata.st_size
            if size < HEADER_SIZE:
                raise ValueError("file is smaller than the DS header")
            header = stream.read(HEADER_SIZE)
    except OSError as error:
        raise ValueError(f"cannot read ROM: {error.strerror or error}") from error

    if len(header) != HEADER_SIZE:
        raise ValueError("cannot read the complete DS header")
    try:
        game_code = header[0x0C:0x10].decode("ascii")
    except UnicodeDecodeError as error:
        raise ValueError("game code is not ASCII") from error
    if not game_code.isalnum() or not game_code.isupper():
        raise ValueError("game code is malformed")
    if header[0x12] not in DS_UNIT_CODES:
        raise ValueError("unsupported DS unit code")
    if int.from_bytes(header[0x15E:0x160], "little") != header_crc16(header):
        raise ValueError("stored header CRC16 does not match")

    size_exponent = header[0x14]
    if size_exponent >= size.bit_length():
        raise ValueError("declared ROM size is inconsistent with the file size")
    declared_size = 0x20000 << size_exponent
    sections = ((0x20, 0x2C, "ARM9"), (0x30, 0x3C, "ARM7"))
    used_size = HEADER_SIZE
    for offset_field, size_field, name in sections:
        offset = int.from_bytes(header[offset_field : offset_field + 4], "little")
        length = int.from_bytes(header[size_field : size_field + 4], "little")
        if not length:
            raise ValueError(f"{name} region is empty")
        end = offset + length
        if end < offset or end > size:
            raise ValueError(f"{name} region is outside the ROM")
        used_size = max(used_size, end)
    if declared_size < used_size or declared_size > size:
        raise ValueError("declared ROM size is inconsistent with the file size")

    return {
        "gameCode": game_code,
        "headerChecksum": f"{(~binascii.crc32(header)) & 0xFFFFFFFF:08X}",
    }


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: rom_identity.py ROM", file=sys.stderr)
        return 2
    try:
        print(json.dumps(read_identity(argv[1]), separators=(",", ":")))
    except ValueError as error:
        print(f"rom_identity: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
