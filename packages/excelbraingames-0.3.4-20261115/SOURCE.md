# ExcelBrainGames 0.3.4 — source of record

**Plugin:** ExcelBrainGames · **Version:** 0.3.4
**Build ID:** EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f
**Jar:** ExcelBrainGames-0.3.4.jar — 136,140 bytes — SHA-256
`52d498e720b00a65cd9de5878e046d870d1fe140db399055338a91d46dc329d3`
— MD5 `440b9b6dd91470c0c0728bf72fdd58dc`
(the jar itself is NOT inside this package — it is deposited
separately per the publication route; its hash is recorded here)
**Authors:** Iris / Mike
**Build folder (complete, self-contained):** `~/workspace/brain-games/build-0.3.4/`
**Installed:** Nov 15, 2026, wrv-install-failsafe run scheduled 02:33 CST (see INSTALLED.txt + OUTCOME.txt in this package)

## What this package is

The complete 0.3.4 build record: BUILD-RECORD.md (what/why/proof),
SPEC-DELTA.md (the design delta), TEST-REPORT.md (the results:
1,924,741 checks, 0 failures — carried suites at exactly their
0.3.3 counts, 1,924,715, plus new BackNavTest 26),
packet/INSTALL-PACKET.md + RECEIPTS.md + MANIFEST-ENTRY-DRAFT.md
(the gated install), FINGERPRINTS.md (the house fingerprint;
markers persist from 0.3.3, build ID rotated), build.sh (the
reproducible build), src/ + tests/ (the full trees),
config.yml + plugin.yml (the shipped descriptors), wordlists/
(the shipped lists), packs/ (the tile-pack generator + validator —
unchanged from 0.3.3; the packs themselves are byte-identical to
0.3.3's and hash-recorded in BUILD-RECORD.md), menu-entry/
(the compass door — unchanged), rig/ (both boot logs:
no-Floodgate and live Geyser+Floodgate), surfaces/ (the What's
New + Guide drafts staged at build time), and
ExcelBrainGames-0.3.4-sources.zip (the src/ tree, 32 entries,
timestamps pinned 2026-11-14; 255,632 bytes, SHA-256
`b16ad5a26adc9febdaa0d886bdf59e8cb9dc4dca18d7118fc2364d8dc203e8dd`,
MD5 `583fad7b9feb2c51d5f7167bc9083e10`).

## Baseline and delta

Copied from `~/workspace/brain-games/build-0.3.3/` after the
baseline verification recorded in BUILD-RECORD.md (src + tests
diff-identical after the copy; the copied jar re-hashed to the
0.3.3 jar of record, 135,705 B / 64e93e17…, which a same-day G1
Homebase pull proved is also the LIVE jar). Source delta vs
0.3.3: gui/Views.java (picker slot-26 Back to Server Menu; shared
boardBackItem() at slot 53; Sudoku 9x9 palette rework — slot 51
Back, Eraser on the high bank at slot 49; question-card slot-18
Back), gui/GuiListener.java (save on the Back path, servermenu +
qback branches), ExcelBrainGames.java (javadoc), core/BuildInfo.java
(the build ID), plugin.yml (version + one sentence); NEW test
BackNavTest.java; no carried test file modified; config.yml, the
word lists, the menu entry and the tile packs byte-identical;
the save format gains NO key (a 0.3.4 save is byte-for-byte a
0.3.3 save).

## Build inputs (hashes pinned in build.sh)

Paper API 26.3 build 231 (compile), Vault 2.0.0 (compile), and
the Floodgate API jar (compile-only) at
`build-0.3.4/libs/floodgate.jar` — pinned by SHA-256 in build.sh.
