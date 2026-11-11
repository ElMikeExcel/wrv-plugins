# ExcelMagicGate 0.3.1 — Rig Verdict

The amended 0.3.0 rig verdicts (demotion seam, step-up, credit
feed, wand lore property seam, inventory sync — 3 boots, zero
SEVERE) stand for the carried code and are incorporated by
reference from `magicgate-rank-build/build-0.3.0-amended/
RIG-VERDICT.md`; the 0.3.1 delta touches only the wand-lore block
text. This file records the 0.3.1 rig: the lore milestone lines
on a live wand, the task's proof surface.

Rig: the amended rig copied whole into this folder
(`magicgate-031-build/rig/server/`). Paper 26.2 (port 25599),
Magic 11.2.5, ExcelWandBars 0.3.11, ExcelGuildGate 0.1.0,
LuckPerms; stage 4 applied (ceiling master); 0.3.1 jar
`EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311`. Fake players via the carried
FloorRigDriver (unmodified). Earned rows seeded in the rig
earned.yml: RigSyncMaster 2,500 (Master target), RigSyncAppr 500
(Apprentice target). Logs in `rig/server/`: boot-031.log (boot 1),
boot-031-2.log (boot 2), boot-031-3.log (boot 3, the shipped lore).

## HEADLINE — the milestone lore: PROVEN on the live wand

### Boot 3 (the shipped two-line form; RigSyncAppr, 500 credited)

- Stepped beginner → apprentice (2 upgrades, join leg), pool
  150/25, verified on the wand; lore property seam VERIFIED.
- Carried inventory sync unchanged: granted 35 spells at
  apprentice incl. archer + bruiser, verified on the wand.
- **`frig lore RigSyncAppr` read the wand's live item lore as:**
  ```
  [0] Rank: Apprentice (ceiling: Master)
  [1] Credited casts: 500
  [2] Next: Master — 1,500 to go
  [3] Milestones: Student 100 · Apprentice 400
  [4] · Master 2,000
  ```
  Credited count AND next-rank gap AND all three milestone
  numbers, each milestone phrase whole on one line.
- `/magicgate status`: build EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311,
  stage 4 of 5, ceiling master, Earning ON (3 tracked, placement
  done 2026-11-10), lore seam "property (auto, verified)",
  Inventory sync ON / in sync (53 path spells held).
- Zero SEVERE in boot 3; clean `stop` shutdown.

### Boot 2 (single-line form — the finding that shaped the ship)

RigSyncMaster (2,500 credited) stepped beginner → master, lore
seam verified, 46-spell inventory grant verified (the carried
master case). Its lore read:
```
[0] Rank: Master (ceiling: Master)
[1] Credited casts: 2,500
[2] Top rank earned — Master of the valley's magic
[3] Milestones: Student 100 · Apprentice 400 · Master
[4]  2,000
```
Magic wrapped the single 55-visible-character milestone line
mid-phrase, splitting "Master" from "2,000". The lore CONTENT was
complete, but Mike's ruling is that the numbers show CLEARLY, so
the shipped form is the two-line list above — and boot 3 proves
it renders unwrapped. Boot 2's other proofs (step-up, seam,
grant) stand for the carried legs.

### Boot 1 (smoke)

First 0.3.1 boot: plugin list shows ExcelMagicGate (0.3.1), the
enable line carries the 0.3.1 build ID, Earning ON, reconcile
sweep ran clean. Stopped to restart under a controllable console
for boots 2–3; no defect found in boot 1.
