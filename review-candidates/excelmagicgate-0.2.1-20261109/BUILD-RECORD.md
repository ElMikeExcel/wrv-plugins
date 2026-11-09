# ExcelMagicGate 0.2.1 — BUILD RECORD (stage-template refresh, parent-backlog Item 41)

Built Nov 9, 2026 by the board-supervisor supervised desk lane (packet:
~/workspace/magicgate-template-refresh-20261109/PACKET.md, supervisor-verified
board-manager run 101); installed Nov 9, 2026, parent hand window 2:30–2:46 PM CST
(marker INSTALLED.txt in this package + in the packet folder). Records package
staged Nov 9, 2026 by board supervisor run 106.

## Artifact

- Jar: ExcelMagicGate-0.2.1.jar — 192,747 B · SHA-256 7110bfdcff48ef1bb6f68ff779276eb25a1118680ccaddbe6dc629fdc8121c5a
- Build ID: EXC-MAGICGATE-0.2.1-WRV-20261109-01-d34112 (suffix = stage-5 final hash prefix)
- Live gate it replaced: ExcelMagicGate 0.2.0 jar 173,699 B / 2e576bf4… (panel-verified at install, EXACT)
- Install: jar swap only, under the live filename ExcelMagicGate-0.2.0.jar; candidate panel
  checksum EXACT post-rename; outgoing 0.2.0 jar kept as ExcelMagicGate-0.2.0.jar.bak-0.2.0;
  BACKUP FIRST "MagicGate 0.2.1 pre-install 1109" with cap-at-6 prune of the oldest manual
  "BossTrial 0.5.2 pre-install 1109" (total 6); boot: Enabling v0.2.1, build line d34112,
  floor reconcile stage-4 master format-identical, Done 20.081s, SEVERE 0, guild LOCKED 4 of 5,
  server left Online 0/10.

## What changed

Templates only — see SOURCE.md delta + PACKET.md §2 for the full before/after hash table.
Stage{2,3,4,5} spells.yml = the Nov 8 composed-install finals byte-for-byte; stage{2,3,4,5}
grants.txt refreshed cumulatively (40 / 68 / 82 / 115); paths/wands NO EDIT; stage0/1 unchanged.
Embedded-template verification PASS: the built jar's stage{2,3,4,5} spells + grants hash to the finals.

## Tests + harness

- Unit suite: 158 checks, 0 failures (TEST-OUTPUT.txt; two assertions updated to encode the
  refreshed packet values — grant counts {0,13,40,68,82,115} + the 0.2.1 build-ID prefix).
- Spell logic harness on the refreshed templates (harness-stage{2,3,4,5}.txt in this package):
  CRITICAL 0 on every stage; checks 8/10/12/13 = 0 on every stage; finding counts
  123 / 225 / 278 / 278 match the resurrection packet's filed numbers for the same finals.
- Deterministic double-build: PASS (two clean builds byte-identical).
- No new rig run: this build changes no code paths; the 0.2.0 rig verdict stands for the
  unchanged code, and the install-window boot proof above is the live proof for the swap.

## Surfaces

None owed and none changed: no player-facing behavior changes (templates only), stated
explicitly per PACKET.md §6.7. Guide / website / What's New (both surfaces) / tips / menus:
no edit — the Guide pass placed Nov 9, 2026 already carries the MagicGate stage-floor content
from 0.2.0, which this build does not alter.
