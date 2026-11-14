#!/usr/bin/env python3
"""Deterministic ExcelShops 1.3.9 jar packager: zips build-out/classes with a
fixed timestamp and the 1.2 jar's exact manifest bytes, so two builds from
identical classes produce byte-identical jars."""
import sys
import zipfile
from pathlib import Path

CLASSES = Path("/home/hatch/workspace/shops-fsync-fix-20261114/work/out-139-a/classes")
MANIFEST = b"Manifest-Version: 1.0\r\nCreated-By: 25.0.4 (Eclipse Adoptium)\r\n\r\n"
STAMP = (2026, 11, 14, 0, 0, 0)


def main(output):
    classes = Path(sys.argv[2]) if len(sys.argv) > 2 else CLASSES
    files = sorted(p.relative_to(classes).as_posix() for p in classes.rglob("*") if p.is_file())
    dirs = sorted({str(Path(f).parent) for f in files if "/" in f})
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        def entry(name, data, is_dir=False):
            info = zipfile.ZipInfo(name, date_time=STAMP)
            info.compress_type = zipfile.ZIP_STORED if is_dir else zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = (0o40755 if is_dir else 0o100644) << 16
            z.writestr(info, data)
        entry("META-INF/", b"", True)
        entry("META-INF/MANIFEST.MF", MANIFEST)
        written_dirs = {"META-INF"}
        for name in files:
            parts = name.split("/")[:-1]
            for i in range(len(parts)):
                d = "/".join(parts[: i + 1])
                if d not in written_dirs:
                    entry(d + "/", b"", True)
                    written_dirs.add(d)
            entry(name, (classes / name).read_bytes())
    print("packaged", output)


if __name__ == "__main__":
    main(sys.argv[1])
