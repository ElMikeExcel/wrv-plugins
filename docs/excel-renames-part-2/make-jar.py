#!/usr/bin/env python3
"""Deterministic jar writer: sorted entries, fixed zip timestamps and
attributes, and the same manifest bytes the JDK jar tool emits here.
Usage: make-jar.py <classes_dir> <out.jar>"""
import os
import sys
import zipfile

MANIFEST = b"Manifest-Version: 1.0\r\nCreated-By: 25.0.4.1 (Eclipse Adoptium)\r\n\r\n"
FIXED_DATE = (2026, 1, 1, 0, 0, 0)


def main() -> None:
    classes_dir, out_jar = sys.argv[1], sys.argv[2]
    entries = []
    for root, _dirs, files in os.walk(classes_dir):
        for name in files:
            full = os.path.join(root, name)
            arc = os.path.relpath(full, classes_dir).replace(os.sep, "/")
            entries.append((arc, full))
    entries.sort(key=lambda e: e[0])
    with zipfile.ZipFile(out_jar, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        info = zipfile.ZipInfo("META-INF/MANIFEST.MF", FIXED_DATE)
        info.external_attr = 0o644 << 16
        info.create_system = 3
        zf.writestr(info, MANIFEST)
        for arc, full in entries:
            info = zipfile.ZipInfo(arc, FIXED_DATE)
            info.external_attr = 0o644 << 16
            info.create_system = 3
            with open(full, "rb") as fh:
                zf.writestr(info, fh.read())


if __name__ == "__main__":
    main()
