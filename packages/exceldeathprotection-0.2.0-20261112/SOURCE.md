# ExcelDeathProtection 0.2.0 — SOURCE record

Excel — Wink Reyes Valley. Base lineage: Argus (0.1.2/0.1.3 line);
0.2.0 delta built by the desk build lane. November 12, 2026.
Design of record: `~/workspace/deathprotection-premium-seam-20261110/DESIGN.md`
+ `BUILD-BRIEF.md` in the same folder; decision of record **D9**
(banked credit) in
`~/workspace/goals/family-minecraft-server-build/hidden_files/decision-log.md`.

## Provenance

- Project tree: `~/workspace/deathprotection-020-build-20261112/build-0.2.0/ExcelDeathProtection/`
  (plugin.yml version 0.2.0, main
  com.winkreyes.exceldeathprotection.ExcelDeathProtection, api-version "1.21").
- **Base (exact, verified before any edit):** the recovered
  ExcelDeathProtection **0.1.3** source from wrv-plugins PR #13,
  commit `18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd`, at
  `~/workspace/deathprotection-source-recovery-20261112/recovered-0.1.3/ExcelDeathProtection/`.
  The copy into this build tree was proven two ways: (1) all 13
  implementation/build/test files hash-match
  `docs/SOURCE-MANIFEST-0.1.3.json`
  (`exact_tested_implementation_build_resources_and_tests`) —
  **13/13 OK**; (2) a full-tree hash comparison of copy vs. base
  showed **no differing files**. The 0.1.1 single-file tree was
  NOT used (different lineage; it would have dropped the two-tier
  purchase flow, journal, and review menu).
- The 0.2.0 delta is confined to: new `ExcelDeathProtectionAPI.java`,
  new `PremiumCredits.java` (Bukkit-free credit arithmetic), new
  `BuildInfo.java`, edits to `ExcelDeathProtection.java`
  (singleton, `premium-credits` PDC store, credit/revoke internals,
  respawn re-arm, status/credit/uncredit/version commands),
  `plugin.yml` (version + usage + permission text), `config.yml`
  (added `premium:` block; prices untouched), `pom.xml` (version),
  docs (CHANGELOG 0.2.0 entry, README API section, PLAYER-GUIDE
  banked-credit note), and tests (one version pin updated;
  `PremiumCreditsTest` + `PremiumApiProbeTest` added). The 0.1.3
  Maven build script is preserved as `build-maven.sh`; `build.sh`
  is the house deterministic javac recipe (see BUILD-RECORD for
  why, and for the test-runtime Mockito note).

## Artifacts

- ExcelDeathProtection-0.2.0.jar — 59,390 bytes
  SHA-256 ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719
  Build EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348 (fingerprint gate
  PASS; in-tree double-build byte-identical).
- ExcelDeathProtection-0.2.0-source.zip — 266,425 bytes
  SHA-256 8540c301912aa9de67382d96787fae9fc66e94ecd0596caf3c76f61910ca842e
  (full 0.2.0 tree, 33 files: src/ + test/ + plugin.yml + config.yml
  + pom.xml + build.sh + build-maven.sh + docs/integration;
  zip entry timestamps pinned 2026-11-12.)

## Verification summary

Unit suite 118/118 (99 carried from 0.1.3 incl. the updated version
pin, + 19 new: 16 PremiumCredits semantics/config checks, 3
BossTrial probe-replica checks). Desk rig on Paper 26.3
(`rig-0.2.0/` in the build folder): phase 1 behaviour 14/14,
phase 2 restart persistence 3/3, SEVERE 0 on both boots — probe
invoke in both shapes, credit-activates, credit-while-active banks,
console grant/revoke, three real death/respawn cycles (kept
inventory, re-arm from bank twice, bank-empty stays off), and the
PDC bank read back after a full restart. NOT installed — built +
staged only.
