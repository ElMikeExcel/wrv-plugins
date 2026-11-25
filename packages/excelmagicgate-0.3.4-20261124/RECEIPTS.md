# RECEIPTS — ExcelMagicGate 0.3.4 (wand-owner repair re-based on 0.3.3)

All pulls Nov 24, 2026, ~02:19–02:24 CT (08:19–08:24 UTC), Homebase
route (~/workspace/server-gui-20261121/work/ssh-pc.sh scp from
MikeExcel@100.72.156.1, D:\muse-servers\wrv). No fallback used.

## Live-premise receipts

- Live jar `plugins/ExcelMagicGate-0.2.0.jar` — pull:
  ~/workspace/magicgate-rebase-20261124/pulls/live-ExcelMagicGate.jar
  — 247,765 B — SHA-256
  4894969672002da96ee3b1220efb7564b194ef3cf60f4aaacea8ce73cf7d066f.
  Its plugin.yml reads `version: 0.3.3` (extracted from the pull).
- Live config `plugins/ExcelMagicGate/config.yml` — pull:
  ~/workspace/magicgate-rebase-20261124/pulls/live-config.yml
  — 2,926 B — SHA-256
  6c46a7070d07de9c7c369e3a46f69e1573f1f16316b4f078950b66a9b7addf2e.
  (Identical to the Nov 15, 2026 G1 receipt for the 0.3.3 install —
  the config has not changed since.) Contains NO `wand-owner-repair`
  section (grep, 0 hits). It is a trimmed operator config: sections
  absent from it (floor-reconcile detail, earning) run on code
  defaults; the repair toggle likewise defaults ON in code.
- ExcelDowned 1.0.4 (the prevention half) is INSTALLED (Nov 24,
  2026 hand window hand-20261124-0200-d189) — no new contamination
  can be written from tonight on.

## Provenance receipts (0.3.3 source of truth)

- Shipped 0.3.3 candidate in its build tree
  (~/workspace/board-r365/magicgate-grant/ExcelMagicGate-0.3.3/
  ExcelMagicGate-0.3.3.jar and -repro.jar): 247,765 B /
  4894969672002da96ee3b1220efb7564b194ef3cf60f4aaacea8ce73cf7d066f.
- Pristine rebuild of that source tree in this lane
  (~/workspace/magicgate-rebase-20261124/work/base-0.3.3, ./build.sh):
  rebuilt jar SHA-256
  4894969672002da96ee3b1220efb7564b194ef3cf60f4aaacea8ce73cf7d066f
  — byte-identical to the shipped candidate AND to the fresh live
  pull above. Three-way match: source = shipped = live.

## Contaminated-wand data check (Nov 24, 2026)

Method: findstr /S /M for the literal `DOWNED` over the live Magic
data tree (plugins\Magic, *.yml + *.dat) and over all world
playerdata (world\players\data\*.dat), run on the PC against
D:\muse-servers\wrv; hit files pulled and read at line level.

- Mage saves: 11 live mage .dat files under
  plugins\Magic\data\players\ (list:
  ~/workspace/magicgate-rebase-20261124/pulls/mage-list.txt).
  Exactly ONE hit: 7401bc22-d42a-4492-a451-374bd12d9e2c.dat
  (MikeExcel). Line-level read of the pull
  (~/workspace/magicgate-rebase-20261124/pulls/mike-mage.dat):
  the stored wand copy still carries
  `owner:"✝ DOWNED MikeExcel"` with
  `mana_timestamp:1795497469460L` — the Nov 23, 2026 23:17:49
  incident save, untouched since. (His CARRIED wand was restored
  clean by playerdata surgery Nov 23; Magic's stored copy in the
  mage save still holds the contaminated owner.)
- Playerdata: ZERO hits — no carried wand (inventory or ender
  chest, both live in the same .dat) in any player's data carries
  the mark.
- Scope limit: wands parked inside world containers (base chests)
  are not covered by these two searches; the repair's activation
  leg covers a wand at the moment it is taken in hand, and the
  join legs cover carried/ender-chest wands.
