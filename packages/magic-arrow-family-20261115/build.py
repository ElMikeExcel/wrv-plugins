#!/usr/bin/env python3
"""R8 arrow-family velocity fix: arrow_damage_new = arrow_damage_old / speed,
inside the real-TippedArrow spell blocks only. Everything else byte-identical.
Vanilla: impact damage = ceil(velocity x base damage); Magic's ProjectileAction
writes arrow_damage onto the Bukkit arrow via setDamage (base damage)."""
import re, sys, os, hashlib

SRC = {
 "live":   "g1-pull/live-spells.yml",
 "stage2": "g1-pull/stage2/spells.yml",
 "stage3": "g1-pull/stage3/spells.yml",
 "stage4": "g1-pull/stage4/spells.yml",
 "stage5": "g1-pull/stage5/spells.yml",
}
IN_SCOPE = {"arrow","arrow|2","arrow|3","arrow|4","arrow|5","arrow|6",
            "arrow_ice","arrow_shock","arrow_regular","arrowrain","arrowrain|2"}
BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(BASE)

def fmt(v):
    s = f"{v:.2f}".rstrip("0")
    if s.endswith("."): s += "0"
    return s

report = []
for name, rel in SRC.items():
    path = os.path.join(ROOT, rel)
    lines = open(path).read().split("\n")
    starts = {}
    for i, l in enumerate(lines):
        m = re.match(r'^([A-Za-z0-9_|]+):\s*$', l)
        if m: starts.setdefault(m.group(1), i)
    order = sorted(starts.items(), key=lambda kv: kv[1])
    changes = []
    for idx, (k, i) in enumerate(order):
        if k not in IN_SCOPE: continue
        end = order[idx+1][1] if idx+1 < len(order) else len(lines)
        speed = None; ad_idx = None; ad_old = None
        for j in range(i, end):
            ms = re.match(r'^(\s+)speed:\s*([0-9.]+)\s*$', lines[j])
            if ms and speed is None: speed = float(ms.group(2))
            ma = re.match(r'^(\s+)arrow_damage:\s*([0-9.]+)\s*$', lines[j])
            if ma and ad_idx is None:
                ad_idx, ad_old = j, float(ma.group(2)); indent = ma.group(1)
        if ad_idx is None: continue
        assert speed, f"{name}:{k} has arrow_damage but no speed"
        new = round(ad_old / speed, 2)
        if new != ad_old:
            lines[ad_idx] = f"{indent}arrow_damage: {fmt(new)}"
            changes.append((k, ad_old, speed, new))
        else:
            changes.append((k, ad_old, speed, new))  # identity (arrowrain speed 1)
    out = "\n".join(lines)
    outpath = os.path.join(BASE, "staged", f"{name}-spells.yml")
    open(outpath, "w").write(out)
    h = hashlib.sha256(out.encode()).hexdigest()
    report.append((name, len(out.encode()), h, changes))
    print(f"{name}: {len(out.encode())} B sha256 {h}")
    for k, old, sp, new in changes:
        print(f"   {k}: arrow_damage {old} / speed {sp} -> {fmt(new)}")
print("BUILD OK")
