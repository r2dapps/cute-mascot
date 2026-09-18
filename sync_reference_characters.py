#!/usr/bin/env python3
"""
Copy page-mascot reference character sheets into characters/ so DesktopMascot
can ship them without depending on the gitignored reference-page-mascot/ clone.

Usage:
  python sync_reference_characters.py
"""
from __future__ import annotations

import os
import shutil
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "reference-page-mascot", "characters")
DST = os.path.join(ROOT, "characters")

# Keep local original skins distinct; skip overwriting these names if already present
PROTECTED = {"chibi", "guy", "mascot", "pixel", "ink"}


def main() -> int:
    if not os.path.isdir(SRC):
        print(f"Missing reference folder:\n  {SRC}")
        print("Clone https://github.com/nilbuild/page-mascot into reference-page-mascot/ first.")
        return 1

    os.makedirs(DST, exist_ok=True)
    copied = 0
    skipped = 0
    for name in sorted(os.listdir(SRC)):
        src_dir = os.path.join(SRC, name)
        if not os.path.isdir(src_dir):
            continue
        directions = os.path.join(src_dir, "directions.png")
        if not os.path.isfile(directions):
            continue
        if name.lower() in PROTECTED:
            skipped += 1
            continue
        dst_dir = os.path.join(DST, name)
        if os.path.isdir(dst_dir):
            shutil.rmtree(dst_dir)
        shutil.copytree(src_dir, dst_dir)
        copied += 1
        print(f"  + {name}")

    print(f"\nDone. Copied {copied} characters into characters/ (skipped protected: {skipped}).")
    print("Desktop tray -> Switch Character will list them after restart.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
