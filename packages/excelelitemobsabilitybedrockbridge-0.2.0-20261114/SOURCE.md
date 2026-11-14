# ExcelEliteMobsAbilityBedrockBridge 0.2.0 — SOURCE record (records-package final)

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 14, 2026.
Commission: Mike's rename directive, Nov 14, 2026, 10:07 AM CST
(rename ExcelAbilityBridge → ExcelEliteMobsAbilityBedrockBridge
"so it's clear what it does"), scoped by
`~/workspace/abilitybridge-rename-20261114/SCOPING-R344.md`
(board supervisor run 344, W4; this build = run 344, W5).

This is the records-package copy of the build folder's SOURCE.md,
finalized at publication (Nov 14, 2026) with the install state.
The build-folder draft states built-only and is superseded by this
copy for install facts.

## Install state (finalized at publication)

- INSTALLED LIVE Nov 14, 2026 by wrv-install-failsafe (run
  scheduled 14:33 CST; window 14:36–15:12 CST; server verified
  empty). Boot: Done (24.373s) at 14:55:16 CST, SEVERE 0,
  ExcelGuildGate guild LOCKED 4 of 5. Evidence: INSTALLED.txt +
  OUTCOME.txt + SCRIPT-OUTPUT.txt in this package.
- Placed jar: plugins/ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar
  = 32,339 B / SHA-256
  c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
  (pull-back verified post-placement, pre- and post-boot).
- Placed config: plugins/ExcelEliteMobsAbilityBedrockBridge/config.yml
  = 297 B / SHA-256
  a99d7fbe87fdc0f20979d38b6213ef784d357f74c45a84d912637ffa3d3a78e1
  (live config carried over, header line only changed;
  debug-log-fires false preserved).
- Old jar held aside, never deleted:
  plugins/ExcelAbilityBridge-0.1.1.jar.held-20261114; the old data
  folder plugins/ExcelAbilityBridge/ was left in place untouched.
- Window backup: "Bridge rename pre-install 1433" (Shockbyte,
  cap-at-6 observed; prunee "ProtectRefresh pre-install 1114").
- The jar itself is NOT committed to wrv-plugins (house rule);
  it is referenced here by hash and deposited to Drive on the
  records leg (see RECORDS-PUBLISH-R353.md in the build folder).
- Player proof still owed at publication: a real Bedrock
  /abilities fire by Kailey (.coolgirlkail) — also discharges the
  bridge's long-owed first real Bedrock fire (R7). Not claimed
  anywhere in this package.

## Provenance

- Project tree: `~/workspace/abilitybridge-rename-build-20261114/ExcelEliteMobsAbilityBedrockBridge/`
  (plugin.yml version 0.2.0, name ExcelEliteMobsAbilityBedrockBridge,
  main com.winkreyes.exceleitemobsabilitybedrockbridge.ExcelEliteMobsAbilityBedrockBridge,
  api-version "1.13").
- Base: the 0.1.1 tree of record
  `~/workspace/abilitybridge-011-build/ExcelAbilityBridge/`,
  copied — never modified in place. BASE GATE: the unmodified
  0.1.1 tree, rebuilt in scratch (/tmp/basegate-011), produced a
  jar byte-identical to the 0.1.1 record jar (30,606 B /
  SHA-256 62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55;
  cmp clean — see base-gate.log in the build folder and
  BUILD-RECORD.md in this package).
- Dependencies: Paper API 26.2 build 123 (compile classpath only,
  house build-libs); EliteMobs 10.9.14 jar COMPILE-ONLY, re-pinned
  by MD5 in build.sh / run-tests.sh
  (`9e5f16298ac558e30197a1529b1ef83c`; the jar itself is held in
  the build folder as `EliteMobs-10.9.14.jar` — 14,563,211 B /
  SHA-256 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd,
  byte-identical to the Modrinth-official 10.9.14 file qualified in
  `~/workspace/em-update-qualification-20261114/RIG-CHECK.md` §1 and
  to the jar placed live by the Nov 14 EM update window); no
  EliteMobs classes packaged; Geyser/Floodgate touched only by
  reflection at runtime, never linked.
- Built by `build.sh` (manual javac, JDK 25, `--release 21`,
  sorted entry list + pinned zip timestamps 2026-11-14 for
  byte-identical rebuilds).
- Ownership: all code is ours (Iris / Mike lineage). NO new
  runtime dependencies.

## What 0.2.0 changes vs 0.1.1 (the whole delta)

IDENTITY RENAME + one ordered co-change. Player-facing behaviour
is otherwise unchanged: the /abilities command, its alias
/classabilities, the softdepend set, the form, the fire path and
the fail-soft shapes are exactly the 0.1.1 ones.

- Rename (SCOPING-R344 §4 rows 1–13): plugin name, main class,
  package com.winkreyes.excelabilitybridge →
  com.winkreyes.exceleitemobsabilitybedrockbridge, jar/source-zip
  names, permission node (see spelling note below), console status
  line name/version, config.yml header comment, build/test scripts.
  Post-rename grep of the project tree for the old name/package
  strings: ZERO hits.
- SPELLING NOTE (records leg, per OUTCOME.txt): the permission
  node exactly as declared in the placed jar's plugin.yml — and
  in this package's plugin.yml copy — is
  `exceleitemobsabilitybedrockbridge.admin` (the package and node
  strings use the "exceleitemobs" spelling throughout the tree
  and jar). A few prose lines in the build/install records spell
  the node with an extra "l" ("excelelitemobs…"); the jar
  spelling governs and live state is internally consistent
  with it.
- Co-change (SCOPING-R344 §4, ordered Nov 14): EliteMobs 10.9.14
  grew AbilityFailureReason 10 → 16 constants. BridgeFailure gains
  the 6 same-named constants (NOT_MOVING, NO_LOW_HEALTH_ALLY,
  NO_WOUNDED_TARGET, NO_ELITE_TARGET, NO_LARGE_TARGET,
  NO_BOSS_TARGET), EmAcsGateway.mapReason maps them one-for-one,
  FailureMessages gains a plain-words line for each (house style:
  an always-true gloss that can never contradict EliteMobs' own
  message), and EmMappingTestMain's count pin moves 10 → 16. The
  compile/test pin re-bases from the EM 10.9.8 jar to the EM
  10.9.14 jar above; the 16-constant enum was verified by javap
  against that exact jar before the co-change was applied.
- Historical "0.1.1:" feature-introduction comments in
  AbilitiesCommand / StatusCommand are kept as history (they
  record when the status surface was added); every current-fact
  statement (names, package, permission, version, EM pin) is 0.2.0.

## Artifacts

- ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar — 32,339 bytes
  SHA-256 c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
  Deterministic double-build byte-identical. (No in-jar build-ID
  fingerprint exists in this lineage; version + hash identify it.)
  NOT in this GitHub package — Drive deposit only.
- ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip — 32,510 bytes
  SHA-256 0cab9815167a0c0cc8f234f0cd8199be6fece9f35a63a1b195445d70a295c75a
  (the whole ExcelEliteMobsAbilityBedrockBridge/ tree, build/
  excluded, following the 0.1.1 zip layout; timestamps pinned
  2026-11-14.) Carried in this package.

## Verification summary

Desk: 192 distinct checks, all passing (TestMain 71 in each of
the two probe modes + EmMappingTestMain 26 + StatusCommandTestMain
24 counted once; raw executions 216) via `run-tests.sh` against
the pinned EM 10.9.14 jar — full output in test-run.log (build
folder); summary in TEST-RESULTS.md (this package). Mirror rig:
PASS, all 15 steps (RIG-R345.md, this package). Install window:
script bundle 28 blocks, all PASS (SCRIPT-OUTPUT.txt, this
package); boot Done (24.373s), SEVERE 0, guild LOCKED 4 of 5,
`/abilities status` first line "ExcelEliteMobsAbilityBedrockBridge
status: enabled (v0.2.0)".
