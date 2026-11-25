# INSTALL PACKET — ExcelEMFixes 0.7.0 (D206 melee mitigation, 17th module)
Filed: Nov 23, 2026 ~19:59 CT by parent hand window (Mike asked why it was held; it was built+proven 13:48 and never filed — this packet + window close that gap the same evening).
Lint: packet ~2.4 KB, lines < 300 chars, non-ASCII minimal.

## Candidates
- Jar: ~/workspace/melee-mitigation-20261123/ExcelEMFixes-0.7.0.jar — 389,940 B — sha256 a063e90942ebb689dfdfc81cd3b6974caf71926128c55d25552a888dac1780c1
- Config: ~/workspace/melee-mitigation-20261123/build-root/deploy/config.yml — 51,553 B — sha256 29bd05dc1efedb0a5680cd9f565a2832a6a00b2a0a755e97490d13a9ef2cb7b7

## Live-premise receipts (fresh Panel pulls, Nov 23, 2026 19:59 CT)
- Live jar plugins/ExcelEMFixes-0.6.0.jar — 376,523 B — sha256 845d010be47281b142e7309735be14ef6931a809024acfb66ac4de3afcbc20f4 (= the 0.7.0 build's provenance base, byte-identical per BUILD-RECORD.md)
- Live config plugins/ExcelEMFixes/config.yml — 47,219 B — sha256 ee968ff8656ffb8eb4e00b2a6f49599407d8244d40612b7a86e3b5627335dff1
- Diff live->deploy config: pure insertion of the melee-mitigation section (87 diff lines, additions only) — no other key changes.

## Procedure (gated window; Panel API route)
1. Marker v2 (hand window). Emptiness: console list = 0 twice.
2. Snapshot backup first (Panel). Cap-at-6: prune oldest manual if a 7th appears; never the 02:30 Daily or the two newest manuals.
3. Stop (Panel power stop). Rename live jar -> ExcelEMFixes-0.6.0.jar.bak-meleemitigation-070; rename live config -> config.yml.bak-0.6.0-20261123.
4. Upload jar as plugins/ExcelEMFixes-0.7.0.jar; upload deploy config as plugins/ExcelEMFixes/config.yml. Verify both by fresh download hash before Start.
5. Start (Panel power start).

## Verification (script output)
- Boot log: "Done (" line; SEVERE count 0.
- "ExcelEMFixes 0.7.0" ready line listing 17 modules incl. melee-mitigation; "Melee Mitigation ready" line with 15% / shield +7% / totem +2%.
- Console: list = 0/10 after boot.

## Rollback
Server stopped: restore ExcelEMFixes-0.6.0.jar.bak-meleemitigation-070 -> ExcelEMFixes-0.6.0.jar (remove the 0.7.0 jar), restore config.yml.bak-0.6.0-20261123 -> config.yml, start. Pre-window snapshot also exists.

## Proof owed (post-install, never gates)
- P-MELEE-MITIGATION-LIVE-1: first natural EM-world fight — melee holder's taken-damage ~x0.85 (x0.78 sword+shield / x0.83 sword+totem); off-hand indicator line appears on the off-hand item while active.
- Surfaces owed after install (parent lane): What's New both surfaces + website pair + canon for 0.7.0/17 modules (folds with the still-owed 0.6.0 canon follow-on).
