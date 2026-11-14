# BUILD RECORD — ExcelEliteMobsAbilityBedrockBridge 0.2.0 (rename of ExcelAbilityBridge 0.1.1)

Built Nov 14, 2026 by board supervisor run 344 worker W5
(desk/local build only). Governing scope:
`~/workspace/abilitybridge-rename-20261114/SCOPING-R344.md`
(22,380 B / SHA-256 prefix 28311566…) — its 18-row rename table,
risks R1–R7, and §6 step-1 build spec. Directive of record: Mike,
Nov 14, 2026, 10:07 AM CST (rename so the plugin's purpose is
clear). **Not installed.** No PC/Homebase route, no panel, no
MANIFEST / parent-backlog / board-state contact (a failsafe
install window was in flight). The mirror rig is a later worker
(SCOPING §6.2) and was NOT attempted here.

## Base gate (house discipline step 1 — PASS, before any rename edit)

- Base tree of record: `~/workspace/abilitybridge-011-build/`
  (0.1.1 jar 30,606 B / SHA-256
  `62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55`).
- The unmodified 0.1.1 project tree was copied to scratch
  (/tmp/basegate-011) and rebuilt with its own build.sh:
  output jar 30,606 B, SHA-256
  `62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55`,
  `cmp` against the record jar: **BYTE-IDENTICAL — GATE PASS**.
  Full log preserved as `base-gate.log` in this folder. The
  0.1.1 tree of record was never modified in place.

## Candidate

- `ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar` — **32,339 B** —
  SHA-256 `c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f`
- Deterministic double-build: two builds to scratch outputs
  (/tmp/build-a.jar, /tmp/build-b.jar) both hashed
  `c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f`;
  `cmp` clean (**BYTE-IDENTICAL**); the shipped-path build
  `cmp`-matches them as well (three builds, one hash).
- Jar inventory: 23 entries — 20 class files, ALL under
  `com/winkreyes/exceleitemobsabilitybedrockbridge/` (incl.
  `ExcelEliteMobsAbilityBedrockBridge.class`, inner classes
  `AcsProbe$Result`, `EmAcsGateway$1`, `FailureMessages$1`),
  plus plugin.yml (1,027 B in tree) + config.yml (297 B) +
  META-INF/MANIFEST.MF. In-jar plugin.yml verified verbatim:
  name ExcelEliteMobsAbilityBedrockBridge, version 0.2.0,
  main com.winkreyes.exceleitemobsabilitybedrockbridge.ExcelEliteMobsAbilityBedrockBridge,
  softdepend [EliteMobs, Geyser-Spigot, floodgate], command
  abilities (alias classabilities), permission
  excelelitemobsabilitybedrockbridge.admin (default: op).
- `ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip` —
  **32,510 B** — SHA-256
  `0cab9815167a0c0cc8f234f0cd8199be6fece9f35a63a1b195445d70a295c75a`
  (24 entries: the whole project tree, build/ excluded, 0.1.1
  zip layout, timestamps pinned 2026-11-14).

## Tests (full suite green)

`run-tests.sh` against the re-pinned EliteMobs 10.9.14 jar
(exit 0; full output in `test-run.log`):

- Run A (no EM jar at runtime): TestMain **71 checks, 0 failures**;
  StatusCommandTestMain **24 checks, 0 failures**.
- Run B (pinned EM 10.9.14 jar present): TestMain **71, 0**;
  EmMappingTestMain **26, 0**; StatusCommandTestMain **24, 0**.
- Totals: **192 distinct checks** (0.1.1 record method: TestMain
  per probe mode + mapping + status counted once), **216 raw
  executions, 0 failures, 0 FAIL lines**.
- Delta vs 0.1.1's 174 distinct is exactly the co-change (+18):
  +6 TestMain failure-line checks per mode (BridgeFailure loop
  over 17 values instead of 11) and +6 EmMapping mapReason
  checks (16 EM constants instead of 10). No other check changed.

## EM 10.9.14 co-change (SCOPING §4 — applied, NOT flagged off)

The scoping's detail was sufficient and the 10.9.14 jar was
available locally with a verified identity, so the co-change is
IN this build (no improvisation: the mapping is the bridge's
standing one-for-one, same-name rule):

- EM jar used: this folder's `EliteMobs-10.9.14.jar` —
  14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`,
  MD5 `9e5f16298ac558e30197a1529b1ef83c` — byte-identical to the
  Modrinth-official file qualified in
  `~/workspace/em-update-qualification-20261114/RIG-CHECK.md` §1
  and to the jar the Nov 14 EM update window placed live
  (copied from `~/workspace/em10914-update-packet-20261114/window-1114/post/EliteMobs-placed.jar`).
- javap on that exact jar BEFORE editing: AbilityFailureReason
  carries exactly the 10 original constants + the 6 scoped ones
  (NOT_MOVING, NO_LOW_HEALTH_ALLY, NO_WOUNDED_TARGET,
  NO_ELITE_TARGET, NO_LARGE_TARGET, NO_BOSS_TARGET) = 16.
- Applied: BridgeFailure +6 same-named constants;
  EmAcsGateway.mapReason +6 one-for-one cases; FailureMessages
  +6 plain-words lines (NOT_MOVING "That ability needs you to be
  on the move."; NO_LOW_HEALTH_ALLY "No ally nearby is low on
  health for that ability."; NO_WOUNDED_TARGET "No wounded
  target nearby for that ability."; NO_ELITE_TARGET "That
  ability needs an Elite target nearby."; NO_LARGE_TARGET "That
  ability needs a large target nearby."; NO_BOSS_TARGET "That
  ability needs a boss target nearby."); EmMappingTestMain pin
  10 → 16. Test evidence: `PASS EM failure reason count pinned
  (16)` + 6 × `PASS mapReason <NEW>` in test-run.log.
- Pin re-base: build.sh / run-tests.sh now MD5-pin the 10.9.14
  jar (`9e5f16298ac558e30197a1529b1ef83c`, replacing the 10.9.8
  pin) and default to this folder's copy; EM_JAR env override
  retained in both scripts.

## Rename-table application checklist (SCOPING §4, rows 1–18)

1. plugin.yml name → ExcelEliteMobsAbilityBedrockBridge — APPLIED + VERIFIED (tree + in-jar plugin.yml).
2. plugin.yml main → com.winkreyes.exceleitemobsabilitybedrockbridge.ExcelEliteMobsAbilityBedrockBridge — APPLIED + VERIFIED (tree + in-jar).
3. Package → com.winkreyes.exceleitemobsabilitybedrockbridge — APPLIED + VERIFIED (src/tests dirs moved; all 20 jar classes under the new package; post-rename grep for old name/package strings in the project tree: 0 hits).
4. Main class file/class → ExcelEliteMobsAbilityBedrockBridge.java / ExcelEliteMobsAbilityBedrockBridge — APPLIED + VERIFIED.
5. Jar → ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar (32,339 B / c28f95a1…) — APPLIED + VERIFIED (double-build cmp clean).
6. Source zip → ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip (32,510 B / 0cab9815…) — APPLIED + VERIFIED.
7. Project folder → NEW build folder ~/workspace/abilitybridge-rename-build-20261114/ExcelEliteMobsAbilityBedrockBridge/ — APPLIED (supervisor's task named the build folder; the scoping's example folder name differs, the tree inside carries the new plugin name either way). 0.1.1 tree untouched.
8. Live data folder plugins/ExcelEliteMobsAbilityBedrockBridge/ — NOT A BUILD ITEM: install-packet step (filing worker carries the R1 config-copy + G1 receipts per SCOPING §6.3). Flagged as follow-on below.
9. Permission node → excelelitemobsabilitybedrockbridge.admin — APPLIED + VERIFIED (plugin.yml, StatusCommand.PERMISSION const, StatusCommandTestMain pins — suite green).
10. Console status line → "ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)" — APPLIED + VERIFIED (StatusCommand formatter via renamed class constants + updated test pins, incl. statusLines("0.2.0", …) expectations).
11. Log prefix / boot lines — APPLIED by construction (Bukkit prefix follows plugin name; the name-bearing log strings in EmAcsGateway/BridgeForms were renamed with the tree; standing-by / online / standing-down texts otherwise unchanged). Live/rig observation is the rig worker's leg.
12. config.yml header → "# ExcelEliteMobsAbilityBedrockBridge configuration." — APPLIED + VERIFIED (tree + in-jar config.yml; sole key debug-log-fires unchanged).
13. build.sh / run-tests.sh → new jar name, tree/class paths, EM 10.9.14 pin — APPLIED + VERIFIED (both scripts ran green end-to-end).
14. Commands /abilities + alias /classabilities — UNCHANGED, VERIFIED in plugin.yml (tree + in-jar).
15. softdepend [EliteMobs, Geyser-Spigot, floodgate] — UNCHANGED, VERIFIED in plugin.yml (tree + in-jar).
16. Records package + draft PR + Drive deposit — LATER WORKER (post-install records leg, SCOPING §6.5). Flagged follow-on.
17. Surfaces (new rename What's New BOTH surfaces, Guide regen, website pair, tips + menus checks) — LATER WORKER (surfaces pass, SCOPING §6.4). Flagged follow-on.
18. MANIFEST pointer-stub PENDING entry + packet — SUPERVISOR filing leg (SCOPING §6.3, G1 receipts on filing day). Flagged follow-on; this worker did not touch the MANIFEST.

## SHA256SUMS re-run

`SHA256SUMS` in this folder covers the whole build folder
(31 files). Generated after this record was finalized, then
re-run for verification — `sha256sum -c SHA256SUMS` output:

```
BUILD-RECORD.md: OK
EliteMobs-10.9.14.jar: OK
ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip: OK
ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar: OK
ExcelEliteMobsAbilityBedrockBridge/build.sh: OK
ExcelEliteMobsAbilityBedrockBridge/config.yml: OK
ExcelEliteMobsAbilityBedrockBridge/plugin.yml: OK
ExcelEliteMobsAbilityBedrockBridge/run-tests.sh: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/AbilitiesCommand.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/AcsGateway.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/AcsProbe.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/BridgeFailure.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/BridgeForms.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/BridgeSlot.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/ClassView.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/EmAcsGateway.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/ExcelEliteMobsAbilityBedrockBridge.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/FailureMessages.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/FireOutcome.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/FormModel.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/FormModelBuilder.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/Labels.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/SlotView.java: OK
ExcelEliteMobsAbilityBedrockBridge/src/com/winkreyes/exceleitemobsabilitybedrockbridge/StatusCommand.java: OK
ExcelEliteMobsAbilityBedrockBridge/tests/com/winkreyes/exceleitemobsabilitybedrockbridge/EmMappingTestMain.java: OK
ExcelEliteMobsAbilityBedrockBridge/tests/com/winkreyes/exceleitemobsabilitybedrockbridge/StatusCommandTestMain.java: OK
ExcelEliteMobsAbilityBedrockBridge/tests/com/winkreyes/exceleitemobsabilitybedrockbridge/StubGateway.java: OK
ExcelEliteMobsAbilityBedrockBridge/tests/com/winkreyes/exceleitemobsabilitybedrockbridge/TestMain.java: OK
SOURCE.md: OK
base-gate.log: OK
test-run.log: OK
```

(The project tree's build/ test-classes directory produced by
run-tests.sh was removed before summing, per the 0.1.1 sums
practice; run-tests.sh regenerates it on demand.)

## Flagged follow-ons (not this worker's legs)

- RIG (SCOPING §6.2): mirror-rig boot under the new name with
  the pinned EM 10.9.14 jar — clean enable, ACS standing-by →
  online, console `abilities status` verbatim (new name line),
  SEVERE 0, R1 data-folder checks. Later worker.
- FILING (SCOPING §6.3): G1 live-premise receipts (live 0.1.1
  jar, live config.yml value, LuckPerms grep for R2), packet +
  MANIFEST pointer stub; swap shape per R3 (old jar held aside,
  never co-resident). Supervisor serial core.
- SURFACES (SCOPING §6.4) and RECORDS (SCOPING §6.5): new rename
  What's New entry on BOTH surfaces, Guide regen, website pair,
  tips/menus checks; new records package + G2 duplicate guard +
  draft PR + Drive deposit. Later workers.
- PLAYER PROOF (SCOPING §7 / R7): a real Bedrock /abilities
  fire (Kailey/.coolgirlkail) still owed — it also discharges
  the bridge's long-owed first real Bedrock fire. Player-class,
  cannot be discharged by rig or desk work.
- NOTE for the filing worker: the build defaults its EM compile
  pin to this folder's EliteMobs-10.9.14.jar copy; that copy is
  a build dependency, NOT a plugin artifact — it must not be
  placed on the server (live EM 10.9.14 is already installed).
