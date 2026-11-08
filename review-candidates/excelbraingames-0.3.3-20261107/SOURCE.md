# ExcelBrainGames 0.3.3 — source of record

**Plugin:** ExcelBrainGames · **Version:** 0.3.3
**Build ID:** EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61
**Jar:** ExcelBrainGames-0.3.3.jar — 135,705 bytes — SHA-256
`64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1`
(the jar itself is NOT inside this package — it is deposited
separately per the publication route in PUSH-NOTES.md; its hash is
recorded here and in SOURCE-PROVENANCE.json)
**Authors:** Iris / Mike
**Build folder (complete, self-contained):** `~/workspace/brain-games/build-0.3.3/`

## What this package is

The complete 0.3.3 build record: BUILD-RECORD.md (what/why/proof),
SPEC-DELTA.md (the design), TEST-REPORT.md (the results),
INSTALL-PACKET.md (the gated install — jar swap only, no config
merge), FINGERPRINTS.md (the house fingerprint; markers persist
from 0.3.2, build id rotated), build.sh (the reproducible build),
src/ + tests/ (the full trees), config.yml + plugin.yml (the
shipped descriptors), wordlists/ (the shipped lists), packs/ (the
tile-pack generator + validator — unchanged from 0.3.2; the packs
themselves are byte-identical to 0.3.2's and hash-recorded in
SOURCE-PROVENANCE.json), menu-entry/braingames.yml (the compass
door — unchanged), and rig/ (both boot logs: no-Floodgate and
live-platform-pair).

## Baseline and delta

Copied from `~/workspace/brain-games/build-0.3.2/` after the
baseline re-verification recorded in BUILD-RECORD.md (full suite at
0.3.2's recorded counts, 1,869,695 checks / 0 failures; the rebuilt
jar's SHA-256 identical to the 0.3.2 jar of record). Source delta
vs 0.3.2: NEW games/sudoku/SudokuBoxView.java; changed Views,
GuiListener, BrainHolder, ExcelBrainGames, BedrockHook (javadoc),
BuildInfo (the id), plugin.yml (version + one sentence); NEW test
SudokuBoxViewTest; no carried test modified; config.yml,
SudokuRound, SudokuTextGrid, word lists, and the menu entry
byte-identical; the save format gains NO key.

## Build inputs (hashes in SOURCE-PROVENANCE.json)

Paper API 26.3 build 231 (compile), Vault 2.0.0 (compile), and the
Floodgate API jar (compile-only) at `build-0.3.3/libs/floodgate.jar`
— pinned by SHA-256 in build.sh, sourced from the fleet's pinned
copy at `~/workspace/autosort-plugin/libs/floodgate.jar`. Like the
other inputs it lives outside this package, per the 0.3.1 pattern.
