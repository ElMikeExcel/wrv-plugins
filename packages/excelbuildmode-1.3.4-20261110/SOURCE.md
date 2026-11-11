# ExcelBuildMode 1.3.4 — SOURCE record

An Excel plugin by Argus for Wink Reyes Valley. November 10, 2026.
Commissioned by board-supervisor decision D5 (Nov 10, 2026): the
combined rebuild reuniting the live October "reviewed" delta (source
lost) with the Nov 10, 2026 console-admin delta.

## Provenance

- Project tree: `~/workspace/buildmode-134-build/ExcelBuildMode/`
  (directory name plain `ExcelBuildMode`; the tree builds 1.3.4 —
  plugin.yml version '1.3.4', main
  com.winkreyes.buildmode.paper.ExcelBuildMode, api-version '26.2').
- Base: the console-admin 1.3.3 tree
  `~/workspace/buildmode-133-build/ExcelBuildMode/` (itself verified
  against the live 1.3.2 tree: clean-room rebuild of 1.3.2
  byte-identical before its delta — see that folder's BUILD-RECORD),
  copied unchanged, then the recovered reviewed delta applied.
- The reviewed delta's provenance is the preserved live jar itself:
  `~/workspace/requal/menujoin143/preserved/ExcelBuildMode-1.3.3.jar`
  — 90,044 B — SHA-256
  3662dcb73ca08ac1677e4257f64eae489ab9486e484ecc447ea2779cedc3ee1c,
  diffed against the genuine 1.3.2 jar (89,718 B, SHA-256
  26d356b4c6266200a30ee891fec4727ba11456491264dfb76df9c8615d600116).
  Recovery method, the complete delta, and the control-build
  bytecode proof (recovered delta alone on the 1.3.2 tree reproduces
  the preserved jar's plugin.yml byte-identically and both differing
  classes with zero normalized-javap diff):
  `REVIEWED-DELTA-RECOVERY.md` + `recovery-evidence/` in this folder.
- Built against Paper 26.2 build 123 from the project runtime with
  the project JDK 25. Build: `bash build.sh` (compiles, runs all
  eight suites, jars with a fixed 2026-11-10 date). As in 1.3.2/1.3.3,
  the exact WorldEdit 7.4.5 jar is on the compile/test classpath
  only; nothing is shaded and WorldEdit stays a softdepend at
  runtime.
- Ownership: all code is ours (Argus/Moon lineage, Excel signature),
  including the recovered reviewed code, which was ours and is now
  source again. No upstream plugin code is bundled;
  WorldEdit/Jobs/mcMMO/EliteMobs are integration points only
  (softdepend/loadbefore), never shaded.

## What 1.3.4 changes (the whole delta, both directions)

- VS LIVE (reviewed 1.3.3): adds the console admin surface only —
  core/AdminPolicy (routing/gate/target classification/off
  decision), the admin branch + handlers + gated tab completion in
  ExcelBuildMode, `excelbuildmode.admin` (default op), the 1.3.3
  banner line (kept) — per docs/PILOT-1.3.3.md. The reviewed
  behavior is preserved instruction-for-instruction.
- VS THE CONSOLE-ADMIN 1.3.3 BUILD: adds the recovered reviewed
  delta only — BuildPolicy.REVIEWED_PERMISSION +
  refusesOutsideWorldEdit + probesResidualWorldEdit, the two call
  sites in ExcelBuildMode (enter refusal, removeAttachment probe),
  the plugin.yml reviewed block — per docs/PILOT-1.3.4.md.
- plugin.yml: version 1.3.4; BOTH nodes registered
  (excelbuildmode.reviewed default false, excelbuildmode.admin
  default op); usage/description name both surfaces. The startup
  banner gains the 1.3.4 combined-rebuild line. config.yml
  untouched since 1.3.2. ReviewedTests.java is new; AdminTests'
  only change is the version pin.

## Artifacts

- ExcelBuildMode-1.3.4.jar — 99,073 bytes
  SHA-256 39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3
  MD5 f175678bbae3eb6ae55416ea260c1ed0
  Clean-room rebuild (fresh copy of src + build.sh) reproduced this
  hash exactly, as did a rebuild from the source zip below.
- ExcelBuildMode-1.3.4-source.zip — full src/ + build.sh + README +
  docs (incl. PILOT-1.3.4.md) — 141,842 bytes
  SHA-256 da252ddce47aab7bc497a20bf69d6f8488c77aaeaa37c89f1180113818a548dc
  MD5 5dd928e42e8d09d618d5811049dde551

## Verification summary

Eight suites green, 11,685 assertions (CoreTests 5,710; RewardTests
5,283; RecoverySafetyTests 84; PlayerDataProofTests 145;
PilotGateTests 338; BuildReturnTests 18 — carried unmodified at
1.3.2 totals; AdminTests 82, version pin moved; ReviewedTests 25
new: recovered entry/exit truth tables, both-node plugin.yml pins,
probe-set exclusions). Control-build bytecode proof of the recovery
(zero normalized-javap diff on both recovered classes; plugin.yml
byte-identical). Rig boot on Paper 26.2-123 + WorldEdit 7.4.5
(rig-1.3.4 in the build folder): "Enabling ExcelBuildMode v1.3.4" +
1.3.4 PILOT banner with both delta lines, Done, zero SEVERE, clean
stop, 7/7 console checks. Rig limitation, stated plainly: the rig
cannot drive a real eligible-player Build Mode session, so the
reviewed entry/exit behavior and a live admin-off restore run in
the install packet's post-install player proofs, not in the rig.
NOT installed — built + staged only.
