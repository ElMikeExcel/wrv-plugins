# ExcelMenu 3.0.0 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 13, 2026.
Commission of record: Menu Programme Item 3 (board supervisor
runs 280–292, Nov 13, 2026). Design of record: DESIGN.md in
this package (74,533 B, SHA-256
bb82539e143c914d7be8982d776eecef3fba50876001cab8be42d2b2b5e55db1),
including the §9 Q1–Q10 recommended defaults, all adopted as
written.

## Provenance

- Project tree: `~/workspace/excelmenu-30-build-20261113/`
  (plugin.yml version 3.0.0, main
  com.winkreyes.excelmenu.bukkit.ExcelMenu, api-version '1.13';
  softdepend gains ExcelSkillsPanel in 3.0.0).
- Base: ExcelMenu 2.0.1 exactly as installed live (jar
  287,906 B, SHA-256
  c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a,
  carried live under the filename ExcelMenu-2.0.0.jar). The
  base gate rebuilt that jar byte-identically from its sources
  zip before any 3.0.0 editing (suite OK (128 tests);
  base-reproduction/ in the build folder; BUILD-REPORT.md).
- Built by `build.sh` in this package (the house deterministic
  recipe; double-build byte-identical, plus two clean-room
  reproductions — a fresh tree copy and the shipped sources zip
  itself unpacked and built — both byte-identical with
  OK (163 tests); BUILD-REPORT.md).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). No third-party code is bundled; Geyser/Floodgate,
  PlaceholderAPI, Vault, Essentials and ExcelSkillsPanel are
  reached through their public APIs or reflectively in their
  own class loaders (softdepend only).

## What 3.0.0 changes vs 2.0.1 (the whole delta)

Design summary: DESIGN.md §2–§5. In short: (1) PINS (§2) —
`pinned:` in the layout (list of entry ids, duplicates refused,
data-driven cap 4) renders true one-tap Spawn/Teleport buttons
first in the Bedrock picker with FLAT-rule group-colour labels;
pinned entries leave their group form; the Java chest model is
untouched (pins are a Bedrock-picker feature by design); a
reload-time cross-check logs one SEVERE per pinned id with no
registry entry. (2) REARRANGE (§3) — per-player menu order:
pure core PlayerMenuPrefs / OrderMerge / PrefsCodec /
MenuEditorCore; Bukkit PrefsStore (playerdata/<uuid>.yml,
save-on-change via temp + atomic rename, corrupt file → SEVERE
once + default order, file left in place); Bedrock editors
E1–E5 and Java editors J1–J3 with per-group + global reset on
both platforms; doors are the picker chrome button, the new
`/rearrange` command, and a live-data fixed item (see the
filing-data note below); config knobs rearrange.enabled /
rearrange.menus (code defaults on, [server_menu]). (3) SKILLS
HOOK DELEGATION (§4) — ScreenHooks gains a tap-time presence
check for ExcelSkillsPanel: when the panel is present the
`skills` entry's own actions dispatch (single source of truth
= entry data); when absent, SkillsPerksScreen opens exactly as
in 2.0.1; the skills_perks definition remains as the fallback
surface. (4) MAGICLEVEL RETIREMENT (§5, jar side) —
`entries/magiclevel.yml` removed from the bundled tree and from
BUNDLED_ENTRIES; the bundled census goes 84 → 83 files and the
live registry effect (94 → 93) lands as an install-window data
deletion (INSTALL-PACKET.md). Bundled data inside the jar is
otherwise byte-identical to 2.0.1's, per DESIGN §6.3. One
rig-driven filing decision rides with the install, not the
jar: the Rearrange fixed item sits at page-2 slot 17, not the
as-designed slot 8 (decision D35 — slot 8 is refused statically
by LayoutLoader; RIG-REPORT.md §3).

## Artifacts

- ExcelMenu-3.0.0.jar — 329,425 bytes
  SHA-256 306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51
  MD5 21a21ac0cbb7dce0a74a7852191dcfa7
  Deterministic double-build byte-identical, and reproduced
  exactly by both clean-room builds above. Fingerprint note:
  ExcelMenu carries NO Layer B build ID — there is no
  BuildInfo class and no excel-build.txt in the jar (verified
  in the shipped jar and the source tree at staging); artifact
  identity is this hash + plugin.yml version 3.0.0 + the
  enable line (see FINGERPRINTS.md). NOT included in this
  records package (house rule); the jar of record lives at
  `~/workspace/excelmenu-30-build-20261113/ExcelMenu-3.0.0.jar`
  and is live on the server under the filename
  `plugins/ExcelMenu-2.0.0.jar` (see Install record below).
- ExcelMenu-3.0.0-sources.zip — 286,127 bytes
  SHA-256 792a2ec8c6bd54762ad4f40d592c14f43975f1a04fd3b0fa51f11382c5e382df
  MD5 604f6bccdf3f6ba87eb95e50d8201095
  (211 files: build.sh + fixtures/ (4) + src/ (203: 73 main
  java, 31 test java, 99 resources — 83 entries, 12 menus,
  2 layouts, config.yml, plugin.yml) + tools/ (3); timestamps
  pinned 2026-10-31.) The src/, fixtures/ and tools/ trees and
  build.sh in this package were verified byte-identical to
  this shipped zip at staging (STAGING-REPORT.md).

## Verification summary

Unit suite OK (163 tests) = 128 carried + 35 new (PinsModelTest
8, LayoutLoaderPinsTest 6, OrderMergeTest 8, PrefsCodecTest 7,
MenuEditorCoreTest 6); the carried suite carries three
enumerated intentional deltas, all the magiclevel retirement
(FormModel/ChestModel count shifts; GoldenHarness acceptance
D10 removed — it existed solely to classify the magiclevel
row; golden harness otherwise D1–D9, D11 over 5,526 rows;
catalogue harness C1–C8, 56 rows). Full account:
BUILD-REPORT.md + TEST-RESULTS.md. Filing Gate 2 rig
(RIG-REPORT.md): R0 control (2.0.1 data) 94 entries / 2 registry
menus / 13 definition menus, SEVERE 0; R2 desk rig and R3 PC
mirror rig (live-pulled Geyser/floodgate + companions) both
93/2/13, platform bridge PRESENT, SEVERE 0; harness 26 PASS /
0 FAIL; magiclevel.yml absent after restart (no resurrection);
`/rearrange` registered with no plugin-set collision —
GATE 2 PASS. Client-tap cases 2, 3, 7, 8, 9, 16 were honestly
NOT EXERCISED on either rig — they are DESIGN §7's five
post-install player proofs, still owed by Mike (below).

## Install record

INSTALLED Nov 13, 2026 — wrv-install-failsafe run scheduled
07:10 CST (parent manual fire, file-and-fire), PC lane, probe
passed attempt 1, no fallback. Window backup
pre-ExcelMenu-3.0.0-20261113 (verified completed); cap-at-6
pruned the oldest manual "Skills011 Menu201 pre-install 1113"
(total after = 6). Placed: the 3.0.0 jar under the same live
filename plugins/ExcelMenu-2.0.0.jar (329,425 B / 306607ba…
EXACT pull-back pre- and post-restart), filing-data config.yml
(2,564 B / 910c2f83…), layouts/server_menu.yml (7,182 B /
9af8ddef…), menus/skills_perks.yml (1,969 B / 236f1cd2…) —
copies of the placed data files are in this package under
filing-data/ — and entries/magiclevel.yml deleted (census 93,
still absent after restart). Boot: Done (21.582s)!; enable line
"ExcelMenu 3.0.0 enabled … 93 entries, 2 registry menus, 13
definition menus live."; bridge PRESENT; SEVERE 0; TPS
20.0/20.0/20.0; guild LOCKED 4 of 5 unchanged. Console smokes:
/rearrange → the player-command line; /magiclevel → the
ExcelMagicLevel usage line. Full record: INSTALLED.txt +
OUTCOME.txt in this package. Proof still owed: Mike's five
DESIGN §7 player proofs (Bedrock pins one-tap; Rearrange +
relog + reset; Skills panel on Bedrock and Java; Magic Level
door gone / typed command answers).

## Drive deposit (later leg — NOT done by this staging)

The Drive trio for the shared Drive Minecraft WRV folder is:
(1) `ExcelMenu-3.0.0.jar` from the build folder above
(329,425 B, SHA-256 306607ba…df51); (2)
`ExcelMenu-3.0.0-sources.zip` (286,127 B, SHA-256 792a2ec8…82df;
copy also in this package); (3) `ExcelMenu-3.0.0-SOURCE.md` — a
copy of this file under that name. See PUSH-NOTES.md.

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
