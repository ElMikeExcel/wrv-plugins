#!/usr/bin/env python3
"""Repack the 0.3.5 candidate from the SHIPPED jar (Moon's discipline):
start from the shipped bytes, replace only entries whose new bytes differ,
add new classes, swap plugin.yml/config.yml, keep everything else identical.
Sorted entries, fixed 2026-01-01 timestamps, deflate-9."""
import hashlib, os, sys, zipfile

WORK = os.path.dirname(os.path.abspath(__file__))
SHIPPED = os.path.expanduser("~/workspace/wandbars-fix/ExcelWandBars-0.3.4-faketapfix.jar")
DEV = os.path.join(WORK, "dev-classes")
RES = os.path.join(WORK, "shipped")  # edited plugin.yml / config.yml live here
OUT = os.path.expanduser("~/workspace/wandbars-ux-build/ExcelWandBars-0.3.5.jar")
FIXED = (2026, 1, 1, 0, 0, 0)

def sha(b): return hashlib.sha256(b).hexdigest()

entries = {}   # name -> bytes (shipped)
order = []
with zipfile.ZipFile(SHIPPED) as z:
    for info in z.infolist():
        entries[info.filename] = z.read(info.filename)
        order.append(info.filename)

# The 5 Geyser hook classes ship as class-file v65 (built at --release 21; proven
# byte-identical at that level in base verification). Their sources are untouched by
# this release, so their shipped bytes carry over exactly, never recompiled bytes.
PINNED = {
    "com/winkreyes/excelwandbars/GeyserTapHook.class",
    "com/winkreyes/excelwandbars/GeyserTapHook$Wrapper.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook$AuthWrapper.class",
    "com/winkreyes/excelwandbars/GeyserHoldHook$UseWrapper.class",
}

changed, added = [], []
# classes from the new compile
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
# resources
for res in ("plugin.yml", "config.yml"):
    data = open(os.path.join(RES, res), "rb").read()
    if sha(entries[res]) != sha(data):
        entries[res] = data; changed.append(res)

names = sorted(entries)
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for name in names:
        zi = zipfile.ZipInfo(name, FIXED)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        z.writestr(zi, entries[name])

print("candidate:", OUT)
print("sha256:", sha(open(OUT, "rb").read()))
print("changed entries (%d):" % len(changed))
for c in sorted(changed): print("  ~", c)
print("added entries (%d):" % len(added))
for a in sorted(added): print("  +", a)
print("total entries:", len(names))
