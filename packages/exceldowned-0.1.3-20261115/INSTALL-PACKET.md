# INSTALL PACKET (DRAFT - supervisor files) - ExcelDowned 0.1.3 - P-DOWNED-REVIVE-1

Lint: 6940 B / max line 87 / non-ASCII 0
(re-based run 393, Nov 15, 2026; was 6299/87/0 before the
spells-leg re-base note in Scope - see FILING-AMENDMENT.md
Amendment 3)
Packet status: DRAFT ONLY. This worker did not file to MANIFEST.md.
The supervisor files after the G1 receipts in RECEIPTS.md are filled
from a same-day live pull.

## PENDING - ExcelDowned 0.1.3 (date added: <supervisor fills>)
- Packet: ~/workspace/board-r364/downed-build/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r364/downed-build/RECEIPTS.md
  (G1 slots blank in this draft - fill before filing, never from
  disk lineage.)

## Candidate
- Artifact: ~/workspace/board-r364/downed-build/ExcelDowned-0.1.3/ExcelDowned-0.1.3.jar
- Size: 59875 B
- SHA-256: 31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93
- Build: EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d
- Tests: 34/34 green (27 carried + 7 new regression, DownedTest)
- Deterministic: two independent local-build.sh runs, one hash;
  the build script's internal double-build also byte-identical.
- Base gate: unmodified 0.1.2 tree rebuilt byte-identical to the
  shipped 0.1.2 jar (2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2ea
  df73cef97c519396, 56778 B) before any change was applied.
- Rig: local rig boot (Paper 26.3 + Magic 11.2.5) - see RESULT.md
  for the verdict lines and the AdminRig exercise tally.

## Live-jar gate (STOP on mismatch)
- Live state at filing time is the HELD state (parent hand window
  Nov 14, 22:33 CST): plugins/ExcelDowned-0.1.2.jar.held-20261114
  and plugins/ExcelDowned.held-20261114/ (data folder).
- Gate values come ONLY from the G1 receipts filled in RECEIPTS.md
  at filing. Expected lineage (NOT a gate source): held jar should
  hash to the shipped 0.1.2 value above; any mismatch stops.

## Scope
- REPLACE the held jar with ExcelDowned-0.1.3.jar at
  plugins/ExcelDowned-0.1.3.jar and RESTORE the held data folder
  plugins/ExcelDowned.held-20261114/ back to plugins/ExcelDowned/.
- CONFIG: the restored data folder's config.yml is REPLACED by the
  candidate config.yml (bleed-out 300 unchanged; gate-range 24 -> 20;
  revive-range 20; new key damage-eats-window: false). Sessions file
  downed.yml and records/ inside the data folder are NOT touched.
- SPELLS LEG (separate file in the same packet): Magic spells.yml
  'resurrection' parameters.range 16 -> 20 (range alignment ruling,
  one number: 20). G1 receipt for the live spells.yml is owed in
  RECEIPTS.md; the exact live path/key shape is confirmed from that
  pull at filing, not from lineage.
  RE-BASE (run 393, Nov 15, 2026): the spells gate values in
  FILING-AMENDMENT.md Amendment 2 are SUPERSEDED by Amendment 3
  in the same file (Arrow fix changed spells.yml after this
  packet was built; fresh G1 pull Nov 15, 2026, 12:19 CST:
  both destinations 188,297 B, Magic 96c61a50..., stage4
  3572fc5e...; diff vs the old base is Arrow-only, 9 lines per
  destination; resurrection line 533 / range line 570 unchanged,
  still 16). Receipts:
  ~/workspace/board-r393/downed-rebase/REBASE-RECEIPTS.md.
- NOT in scope: ExcelMagicGate held-key audit / grant-verification
  build (Defect 2 grant side) - a separate plugin and a separate
  build, named owed in RESULT.md. No other plugin, world, or player
  data changes.

## What the candidate changes (summary; DESIGN-0.1.3.md carries it)
- Totem first: lethal hit with a totem in either hand is not
  intercepted; vanilla consumes the totem. No totem = downed.
- Window rule: damage while downed eats 0 bleed-out window by
  default (damage-eats-window: false); absorption also cancels the
  event; combust guard; fire ticks cleared; MONITOR audit line;
  bypassDeath latch bounded to the session's login.
- Resurrection: 'Resurrect' action registered at onLoad (before
  Magic template load) + onEnable; boot self-check WARNING-names
  any of resurrection / grouprenewal whose template does not
  resolve (probe failure is also a WARNING, never fail-open);
  ResurrectAction target + fallback both same-world + range-bound;
  ranges aligned at 20 (gate / revive / spell per the spells leg).

## Gates (installer / failsafe)
- Marker discipline: WINDOW-IN-PROGRESS.txt at window start,
  deleted at finish; re-check the marker immediately before
  Shut Down and again before Start.
- BACKUP FIRST + cap-at-6: one fresh Shockbyte backup for the
  window; prune oldest manual backups, name prunees; never delete
  the Daily Backup's newest copy or the two newest manuals.
- Emptiness gate: list = 0 twice before the swap (this is a jar +
  config + spells file change - fully gated, NOT a text live-edit).

## Install sequence (installer executes; this draft does not)
1. Verify G1 receipts + live-jar gate (held names + hashes).
2. Backup first (above), marker up, emptiness gate.
3. Rename plugins/ExcelDowned.held-20261114/ -> plugins/ExcelDowned/
   with the candidate config.yml placed; remove the held jar name
   and place plugins/ExcelDowned-0.1.3.jar (candidate hash above,
   re-hashed fresh at the provenance gate).
4. Apply the spells.yml range leg (16 -> 20) with an in-place .bak.
5. Restart; boot verify (below); marker down.

## Boot verify (script-shaped)
- grep boot log for "Enabling ExcelDowned v0.1.3" - present once.
- grep for "ExcelDowned 0.1.3 ready - build
  EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d" - present.
- grep for "custom Magic action 'Resurrect' registered." - present
  (expected twice: load + enable).
- Self-check lines: one line per key (resurrection, grouprenewal):
  either "resolves at boot" (INFO) or a WARNING naming the key.
  A WARNING on either key = the grant NPE is still live for that
  key: report it in the install report verbatim; do not call the
  grant verified.
- SEVERE count attributable to ExcelDowned: 0.
- guild LOCKED <n> of 5 unchanged by this window.
- Marker on install: ~/workspace/board-r364/downed-build/INSTALLED.txt

## Same-pass surfaces (owed at filing / install, named here)
- Guide knowledge + intents: owed (downed rules changed: totem
  first, damage no longer eats the window, revive range 20).
- Website pair + What's New BOTH surfaces (website section and
  in-game ExcelWhatsNew changelog): owed.
- Broadcaster tips check: owed (any downed/rescue tip reworded).
- In-game menus check: no menu definitions change in this packet;
  verify /downed + /giveup entries still resolve (check only).

## Proof owed after install
- D2-CAST-1 / D2-LIVE-1: a named-player live Resurrection revive
  (two accounts, one ExcelTogether party) before any boss attempt
  relies on it - discharged only by the tester's own report.
- D3 live spot-check: lethal hit with a held totem fires the totem
  and does not down (volunteer, controlled).
- D1 live spot-check: damage taken while downed leaves the bleed-out
  clock at wall-clock pace (records rows show
  window_seconds_eaten: 0.0).
