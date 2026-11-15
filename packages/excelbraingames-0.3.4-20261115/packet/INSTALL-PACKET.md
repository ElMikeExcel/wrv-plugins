# INSTALL PACKET — ExcelBrainGames 0.3.4 (Back-button pass, BB-02)

Lint: 4967 B / max line 78 / non-ASCII 15
Packet state: DRAFT staged by board-manager run 354 worker BB-02.
NOT filed to the MANIFEST by the worker — the supervisor files it;
per FIXBATCH-SPLIT-R348.md §6, this unit's filing waits for the
BB-01 (ExcelMenu) packet to exist. The build is parallel-safe: it
edits no ExcelMenu file and builds against the existing /menu
opener.

## PENDING — ExcelBrainGames 0.3.4 (BB-02, 13 Back rows)

- **Packet:** ~/workspace/back-button-wave1-r354/BB-02/INSTALL-PACKET.md
- **Live-premise receipts (G1):** every live claim below cites
  ~/workspace/back-button-wave1-r354/BB-02/RECEIPTS.md
  (SHA-256 f1799e120c91d6691de0270142e6ca458680aae1c7c4ef9d4b09c829765d7127),
  Homebase pull Nov 14, 2026, 16:55 CST, probe PASS attempt 1,
  no fallback:
  - Live jar is ExcelBrainGames 0.3.3, exactly the audited build —
    receipt: pull-g1/ExcelBrainGames-0.3.3.jar (see RECEIPTS.md).
  - Exactly one active BrainGames jar live; no 0.3.4 jar live —
    receipt: pull-g1/plugins-braingames-lsf.txt census.
- **Candidate:** ~/workspace/brain-games/build-0.3.4/ExcelBrainGames-0.3.4.jar
  — 136,140 B / SHA-256
  52d498e720b00a65cd9de5878e046d870d1fe140db399055338a91d46dc329d3;
  build EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f;
  tests 1,924,741/1,924,741 (two full runs, jar byte-identical);
  rig: two Paper 26.3-235 boots PASS, SEVERE 0 (RECEIPTS.md).
- **Live-jar gate (STOP on mismatch):** live file
  `plugins/ExcelBrainGames-0.3.3.jar` = 135,705 B /
  64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1
  — from the G1 receipt above, never from lineage.
- **Staged artifacts:** ~/workspace/brain-games/build-0.3.4/
  (SHA256SUMS in the build folder, 1,296 entries, all OK at
  staging) — jar + full source + tests + BUILD-RECORD.md +
  TEST-REPORT.md + SPEC-DELTA.md.
- **Scope:** one jar swap, 0.3.3 → 0.3.4. Navigation only: game
  picker gains Back to Server Menu (slot 26, /menu opener); Word
  Search / Memory Match boards, Sudoku 4x4/6x6 board and the
  Bedrock Sudoku box picker gain Back at slot 53 (the old Close's
  save, then the picker reopens — a round is never discarded);
  Sudoku 9x9 board gains Back at slot 51 with the Eraser moved to
  the high palette bank (slot 49); Quick Math / Word Scramble
  question cards gain Back at slot 18 (saves, never banks).
  Explicitly NOT changed: config.yml, word lists, player data and
  the save format (no new key; 0.3.3 saves load unchanged), the
  tile packs, the ExcelMenu entry file, tiers, payouts, the cap.
- **Gates:** standard gated-install discipline — marker with
  re-checks before Shut Down and before Start; BACKUP FIRST +
  cap-at-6 (name prunees; never the Daily newest or the two newest
  manuals); emptiness `list` = 0 twice. Swap shape follows the
  fleet pattern: old jar held aside as
  ExcelBrainGames-0.3.3.jar.bak-<date>-pre034, never deleted.
- **Boot verify:** enable line verbatim pattern:
  `ExcelBrainGames 0.3.4 enabled: games=[word-search, memory-match,
  sudoku, quick-math, word-scramble, riddle], kid-tiers=ENABLED,
  daily-cap=$1,500.00, ... build=EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f`;
  guild LOCKED 4 of 5; SEVERE 0 attributable.
  Marker: ~/workspace/brain-games/build-0.3.4/INSTALLED.txt.
- **Same-pass surfaces:** staged as drafts in this folder —
  surfaces/WHATSNEW-DRAFT.md (website What's New entry + in-game
  changelog entry text) and surfaces/GUIDE-NOTE-DRAFT.md (Guide
  knowledge note). Tips check: NO CHANGE (no tip text describes
  Brain Games navigation). Menus check: NO CHANGE (the ExcelMenu
  braingames entry is untouched; the picker change is inside the
  plugin's own GUI). The supervisor applies the two drafts in the
  install pass.
- **Proof owed after install:** player spot taps (batch
  acceptance, Mike): open Brain Games from the Server Menu, tap
  Back on the picker → Server Menu shown; on each board start a
  round, make a move, tap Back mid-round → picker shows the round
  in progress, Resume re-enters the same round; Sudoku 9x9: Back
  at slot 51, Eraser found on the 6-9 palette bank; Quick Math /
  Word Scramble cards: Back returns to the picker with the round
  saved (Scramble NOT banked). Kailey on Bedrock covers the box
  picker row. A proof is only discharged by the tester's own
  report/screenshot.
- **Marker on install:** ~/workspace/brain-games/build-0.3.4/INSTALLED.txt

## Verification by script output

- Gate hash: `sha256sum` of the fresh live pull at install time
  equals the Live-jar gate line above.
- Candidate hash: `sha256sum` of the staged jar equals the
  Candidate line above; after placement, pull-back hash equals it.
- Tests: build.sh output lines `BackNavTest: 26 passed, 0 failed`
  and total 1,924,741 / 0 (TEST-REPORT.md in the build folder).
- Boot: grep of latest.log for the enable line above + `Done (`;
  `grep -c SEVERE` attributable = 0.
