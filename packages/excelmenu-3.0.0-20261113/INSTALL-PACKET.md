# INSTALL PACKET — ExcelMenu 3.0.0 (pins + per-player rearrange) — FINAL

**Status: FINAL — filed as a gated MANIFEST PENDING entry Nov 13, 2026,
board supervisor run 292 continuation.** Supersedes
INSTALL-PACKET-DRAFT-bm286.md. Design of record:
~/workspace/excelmenu-30-design-20261113/DESIGN.md (74,533 B /
bb82539e…). All §8 sequencing gates are CLOSED with evidence below;
the failsafe owns the install.

**Item:** ExcelMenu 2.0.1 → 3.0.0 (Menu Programme Item 3: true one-tap
Spawn/Teleport pins, per-player rearrange with Mike's arrangement as
default + reset, skills-panel hook delegation, magiclevel bundled-entry
retirement).

**Candidate jar:** `~/workspace/excelmenu-30-build-20261113/packet/ExcelMenu-3.0.0.jar`
— 329,425 B / SHA-256
306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51.
**Live filename at swap:** `plugins/ExcelMenu-2.0.0.jar` (carries 2.0.1
content, placed under that name in the Nov 13, 00:45 window) — the
3.0.0 jar is placed under the SAME live filename; the outgoing jar is
preserved in place as `ExcelMenu-2.0.0.jar.bak-20261113-pre300`.

## Sequencing gates (DESIGN §8) — ALL CLOSED

- G-a 2.0.1 landed first: SATISFIED — fresh pull Nov 13, 2026,
  06:34–06:39 CST (rig-bm292/RIG-REPORT.md §1): live
  `plugins/ExcelMenu-2.0.0.jar` = 287,906 B / c6da29ba… EXACT,
  exactly one live ExcelMenu jar.
- G-b panel gate: SATISFIED (SkillsPanel live Nov 12, 23:48 CST +
  Mike's Bedrock acceptance Nov 13, ~12:06 AM; Java pass does not
  gate 3.0 — it rides player proof 4).
- G-c no dependency on SkillsPanel 0.1.1+: satisfied by construction
  (presence-only hook; live panel is now 0.2.1, version-blind).
- G-d battle/weekend discipline: filing Fri Nov 13 morning; the
  install must NOT crowd the Sat Nov 14 Warden-attempt evening
  (~8:30 PM, flexible 9–10 PM); weekend protocol governs a weekend
  landing (empty → push; players on → countdown, push, bring back).
- G-e G1 fresh pulls: SATISFIED same-day — rig-bm292 pulls Nov 13,
  2026, 06:34–06:39 CST via the PC route (rclone from live, hashes
  re-verified on both machines; RIG-REPORT.md §1): config.yml
  2,218 B / d14d9cb1…; layouts/server_menu.yml 6,959 B / fceb1433…;
  layouts/commands_menu.yml 4,956 B / 146db1d9…;
  entries/skills.yml 438 B / ccc52764…; entries/magiclevel.yml
  2,778 B / 6d367005… (deletion premise); menus/skills_perks.yml
  1,957 B / 5df3f0a3…; entries census 94 live .yml. OPERATIVE GATE:
  the installing window re-pulls fresh and STOPs on any mismatch.
- Filing gate 1 (§3.2 platform-rank STOP): CLOSED run 288 (94/94
  entries, 15/15 groups order == order-bedrock; g1-fallback-bm288/).
- Filing gate 2 (R2 desk rig matrix + R3 mirror rig): **CLOSED run
  292 — GATE 2 PASS** (~/workspace/excelmenu-30-build-20261113/rig-bm292/RIG-REPORT.md):
  R0 control 94/2/13 SEVERE 0; R2 desk + R3 PC mirror (live-pulled
  Geyser/floodgate + companions) 93/2/13, bridge PRESENT, SEVERE 0;
  harness 26 PASS / 0 FAIL; magiclevel absent after restart (no
  resurrection); /rearrange registered with no plugin-set collision
  (gate 4's plugin-set half CLOSED). Client-tap cases 2, 3, 7, 8, 9,
  16 honestly NOT EXERCISED — they are DESIGN §7's post-install
  player proofs, not filing gates.
- Filing gates 3 + 4 (config remainder read; /rearrange entries
  collision): CLOSED run 288 + run 292 (above).

## Scope — ONE window, ONE backup, ONE restart

1. Jar swap (above) under the same live filename.
2. Place `packet/filing-data/layouts/server_menu.yml` — 7,182 B /
   9af8ddefa386837c1a6e8073be5e1a14285e0679f8bc4158571e819e13a27f6d
   (adds `pinned: [spawn, tpmenu]` + the Rearrange fixed item at
   page-2 **slot 17**) → `plugins/ExcelMenu/layouts/server_menu.yml`.
   In-place .bak of the outgoing file first.
   **SLOT 17, NOT SLOT 8 (decision D35):** the as-designed slot 8 is
   refused statically by LayoutLoader (it sits in chest_tools'
   page-2 slot list [1..8]; the whole layout fails — rig evidence
   RIG-REPORT.md §3, boot-r2-panel.log). Slot 17 is DESIGN §9 Q7's
   pre-named fallback, proven green on both rigs.
3. Place `packet/filing-data/config.yml` — 2,564 B /
   910c2f834215ded57a4f24576f06eba6f8f5fb8017227f63106fba2d5f31c3be
   (the live file + only the §3.7 `rearrange:` knob block appended)
   → `plugins/ExcelMenu/config.yml`. In-place .bak first.
4. Place `packet/filing-data/menus/skills_perks.yml` — 1,969 B /
   236f1cd26bbb1294d3f342d9fa93b3c5a34565689eb4da94e3e87bf5da08bfe0
   (`magic:` item → Magic Level door per §4.3) →
   `plugins/ExcelMenu/menus/skills_perks.yml`. In-place .bak first.
5. DELETE `plugins/ExcelMenu/entries/magiclevel.yml` (in-place .bak
   kept as `magiclevel.yml.bak-20261113-pre300`; resurrection check
   post-restart per §5.3).

Staged data diffs were reviewed against §6.2 at rig time (any extra
hunk = stop): the filing-data set is byte-identical to the rig-proven
seed-r2 data set (hashes above re-verified supervisor-side at filing).

## Install steps / gates

Marker discipline + re-checks; BACKUP FIRST (one backup for the
window) + cap-at-6 (name prunees; never the Daily's newest or the two
newest manuals); emptiness `list` 0/10 twice; fresh-pull live gates
STOP on mismatch; place jar + 3 data files hash-verified pull-back;
delete magiclevel; ONE restart.

**Boot verify:** "ExcelMenu 3.0.0 enabled" line with **93 entries /
2 registry menus / 13 definition menus**; bridge PRESENT; SEVERE 0;
TPS 20.0 after settle; `entries/magiclevel.yml` still absent after
the restart; placed files pull-back EXACT; guild LOCKED 4 of 5
unchanged. Console smoke: `/rearrange` answers the player-command
line; `/magiclevel` (typed) still answers via ExcelMagicLevel.

**Rollback:** restore the .bak jar + the three data files +
magiclevel.yml from its .bak, restart, verify the 2.0.1 enable line
and 94/2/13.

## Same-pass surfaces (supervisor legs, sequenced after landing)

Guide §5.4 re-point + rearrange chunk; website pair; What's New BOTH
surfaces; tips checked; menus = this packet. Records per DESIGN §8
step 8: G2 duplicate check + claim line, Drive deposit, draft PR in
wrv-plugins (stays OPEN DRAFT); website PR merged + G3 deploy check.

## Player proofs owed after install (Mike — DESIGN §7)

1. Bedrock: Spawn pin one tap → spawn confirm; Teleport pin one tap;
   Travel no longer lists them; Back returns from every section.
2. Bedrock: Rearrange → move one door in Me → relog → order kept;
   Reset → default returns.
3. Bedrock: Skills & Perks → the Skills panel (closes Item 5's
   Bedrock proof gap).
4. Java: compass unchanged except page-2 Rearrange item; Skills &
   Perks → panel (closes the owed Java panel pass); /rearrange move
   shows on Bedrock.
5. Magic Level: no compass door; Skills panel shows the Magic block;
   typed /magiclevel still answers.

**Marker on install:** `~/workspace/excelmenu-30-build-20261113/INSTALLED.txt`
