"""Patch arm64 ELF PT_LOAD segment alignment to 16 KB (0x4000) for Play compliance."""
from __future__ import annotations

import struct
import sys
from pathlib import Path

EI_CLASS = 4
ELFCLASS64 = 2
PT_LOAD = 1
TARGET_ALIGN = 0x4000


def patch_elf(path: Path) -> bool:
    data = bytearray(path.read_bytes())
    if data[EI_CLASS] != ELFCLASS64:
        raise ValueError(f"{path}: only ELF64 supported")

    e_phoff = struct.unpack_from("<Q", data, 32)[0]
    e_phentsize = struct.unpack_from("<H", data, 54)[0]
    e_phnum = struct.unpack_from("<H", data, 56)[0]

    changed = False
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        p_type, p_flags = struct.unpack_from("<II", data, off)
        if p_type != PT_LOAD:
            continue
        p_align = struct.unpack_from("<Q", data, off + 0x30)[0]
        if p_align < TARGET_ALIGN:
            struct.pack_into("<Q", data, off + 0x30, TARGET_ALIGN)
            changed = True

    if changed:
        path.write_bytes(data)
    return changed


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("Usage: patch_elf_16kb.py <file.so> [more.so ...]", file=sys.stderr)
        return 1

    any_changed = False
    for arg in argv[1:]:
        path = Path(arg)
        if patch_elf(path):
            print(f"Patched {path}")
            any_changed = True
        else:
            print(f"Already aligned {path}")
    return 0 if any_changed or len(argv) > 1 else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
