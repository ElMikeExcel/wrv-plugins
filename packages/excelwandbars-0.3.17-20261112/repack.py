#!/usr/bin/env python3
"""Repack an ExcelWandBars candidate from a shipped base jar (Moon's discipline):
start from the base jar's bytes, replace only entries whose new bytes differ,
add new classes, swap plugin.yml/config.yml, keep everything else identical.
Sorted entries, fixed 2026-01-01 timestamps, deflate-9.
Usage: repack.py BASE_JAR DEV_CLASSES_DIR RES_DIR OUT_JAR"""
import hashlib, os, sys, zipfile

BASE, DEV, RES, OUT = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
FIXED = (2026, 1, 1, 0, 0, 0)
def sha(b): return hashlib.sha256(b).hexdigest()

entries, order = {}, []
with zipfile.ZipFile(BASE) as z:
    for info in z.infolist():
        entries[info.filename] = z.read(info.filename)
        order.append(info.filename)

# The 5 Geyser hook classes ship as class-file v65 (built at --release 21; proven
# byte-identical at that level in the 0.3.5 base verification). Their sources are
# untouched, so their shipped bytes carry over exactly.
PINNED = {
    "com/winkreyes/excelwandbars/GeyserTapHook.class",
    "com/winkreyes/excelwandbars/GeyserTapHook$Wrapper.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook$AuthWrapper.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook$UseWrapper.class",
}

changed, added = [], []
for root, _, files in os.walk(DEV):
    for f in files:
        if not f.endswith(".class"): continue
        full = os.path.join(root, f)
        name = os.path.relpath(full, DEV).replace(os.sep, "/")
        if name in PINNED: continue
        data = open(full, "rb").read()
        if name in entries:
            if sha(entries[name]) != sha(data):
                entries[name] = data; changed.append(name)
        else:
            entries[name] = data; added.append(name)
for res in ("plugin.yml", "config.yml"):
    data = open(os.path.join(RES, res), "rb").read()
    if sha(entries[res]) != sha(data):
        entries[res] = data; changed.append(res)
# 0.3.12: the Layer B fingerprint file ships at the jar root (absent from
# the 0.3.6 base; added when the res dir carries it).
_bf = os.path.join(RES, "excel-build.txt")
if os.path.exists(_bf):
    data = open(_bf, "rb").read()
    if "excel-build.txt" not in entries or sha(entries["excel-build.txt"]) != sha(data):
        (changed if "excel-build.txt" in entries else added).append("excel-build.txt")
        entries["excel-build.txt"] = data

names = sorted(entries)
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for name in names:
        zi = zipfile.ZipInfo(name, FIXED)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        z.writestr(zi, entries[name])

print("candidate:", OUT)
print("sha256:", sha(open(OUT, "rb").read()))
print("size:", os.path.getsize(OUT))
print("changed entries (%d):" % len(changed))
for c in sorted(changed): print("  ~", c)
print("added entries (%d):" % len(added))
for a in sorted(added): print("  +", a)
print("total entries:", len(names))
