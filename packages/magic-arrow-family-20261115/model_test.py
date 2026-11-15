#!/usr/bin/env python3
"""Post-fix model: physical arrow = ceil(speed x arrow_damage) at point blank
(vanilla formula, no crit) must reproduce the config's balanced-as arrow part
(old arrow_damage) within rounding (<=1 HP from the 2dp rounding + ceil).
Per-cast total = physical + entity_damage payload (payload unchanged).
Hierarchy anchors (Nov 8 audit): netherite Sharp V sword ~17.6 DPS,
Power V bow ~20 DPS; magic top tier target = bow parity 20.0, never above."""
import re, math, sys
def blocks(path):
    lines=open(path).read().split("\n"); starts={}
    for i,l in enumerate(lines):
        m=re.match(r'^([A-Za-z0-9_|]+):\s*$',l)
        if m: starts.setdefault(m.group(1),i)
    order=sorted(starts.items(), key=lambda kv:kv[1]); out={}
    for idx,(k,i) in enumerate(order):
        end=order[idx+1][1] if idx+1<len(order) else len(lines)
        vals={}
        for ln in lines[i:end]:
            m=re.match(r'^\s+(arrow_damage|speed|entity_damage|cooldown|mana):\s*([0-9.]+)',ln)
            if m and m.group(1) not in vals: vals[m.group(1)]=float(m.group(2))
        out[k]=vals
    return out
fails=0; rows=[]
pairs=[("live","g1-pull/live-spells.yml","build/staged/live-spells.yml"),
       ("stage2","g1-pull/stage2/spells.yml","build/staged/stage2-spells.yml"),
       ("stage3","g1-pull/stage3/spells.yml","build/staged/stage3-spells.yml"),
       ("stage5","g1-pull/stage5/spells.yml","build/staged/stage5-spells.yml")]
import os
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
for name,bp,sp in pairs:
    b=blocks(os.path.join(ROOT,bp)); s=blocks(os.path.join(ROOT,sp))
    for k in ["arrow","arrow|2","arrow|3","arrow|4","arrow|5","arrow|6","arrow_ice","arrow_shock","arrow_regular","arrowrain"]:
        if k not in b or "arrow_damage" not in b[k]: continue
        bo,so=b[k],s[k]
        phys=math.ceil(so["speed"]*so["arrow_damage"])
        want=bo["arrow_damage"]
        ok=abs(phys-want)<=1
        # payload + cooldown + mana + speed untouched
        same=all(bo.get(x)==so.get(x) for x in ("entity_damage","cooldown","mana","speed"))
        if not (ok and same): fails+=1
        rows.append(f"{name} {k}: phys {phys} vs intent {want:g} {'OK' if ok else 'FAIL'}; payload/cd/mana/speed {'unchanged' if same else 'CHANGED!'}")
        if k=="arrow|6" and name=="live":
            total=phys+so["entity_damage"]; dps=total/(so["cooldown"]/1000)
            rows.append(f"  -> arrow|6 post-fix per-cast {total} (phys {phys} + payload {so['entity_damage']:g}), DPS {dps:.1f} (bow parity ~20, sword ~17.6)")
            if dps>20.05: fails+=1; rows.append("  FAIL: exceeds bow parity")
print("\n".join(rows)); print(f"MODEL TEST: {'PASS' if fails==0 else f'{fails} FAILURES'}")
sys.exit(1 if fails else 0)
