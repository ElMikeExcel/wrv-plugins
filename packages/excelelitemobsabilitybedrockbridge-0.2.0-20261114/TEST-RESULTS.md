# TEST RESULTS — ExcelEliteMobsAbilityBedrockBridge 0.2.0

Extracted at records staging (Nov 14, 2026) from BUILD-RECORD.md,
test-run.log, RIG-R345.md, and the install window's
SCRIPT-OUTPUT.txt. All four source records are carried in this
package or the build folder named below.

## Desk test suite (run-tests.sh, against pinned EM 10.9.14 jar)

- Run A (no EM jar at runtime): TestMain 71 checks, 0 failures;
  StatusCommandTestMain 24 checks, 0 failures.
- Run B (pinned EM 10.9.14 jar present): TestMain 71, 0;
  EmMappingTestMain 26, 0; StatusCommandTestMain 24, 0.
- Totals: 192 distinct checks, 216 raw executions, 0 failures,
  0 FAIL lines. Exit code 0. Full output: test-run.log in
  ~/workspace/abilitybridge-rename-build-20261114/.
- Delta vs 0.1.1's 174 distinct is exactly the EM 10.9.14
  co-change (+18): +6 TestMain failure-line checks per mode
  (BridgeFailure loop over 17 values instead of 11) and +6
  EmMapping mapReason checks (16 EM constants instead of 10).
  No other check changed. Evidence lines include
  `PASS EM failure reason count pinned (16)` and 6 ×
  `PASS mapReason <NEW>`.

## Base gate (pre-rename discipline)

- The unmodified 0.1.1 tree, rebuilt in scratch, produced a jar
  byte-identical to the 0.1.1 record jar (30,606 B / SHA-256
  62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55;
  cmp clean). GATE PASS before any rename edit. Log: base-gate.log
  in the build folder.

## Deterministic build

- Two scratch builds + the shipped-path build: three builds, one
  hash — c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
  (32,339 B), cmp clean.

## Mirror rig (RIG-R345.md, this package)

- VERDICT: PASS — all 15 steps, including: candidate + EM
  10.9.14 hash pairs desk↔mirror; old-name jar not co-resident;
  boot line `Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0`
  (both boots); SEVERE 0 both boots; ACS standing-by → online,
  EM link UP on 10.9.14; plugin list new name once / old name
  zero; `abilities status` new-name first line verbatim; R1 data
  folder behavior + config migration; permission node
  exceleitemobsabilitybedrockbridge.admin resolves (step 12 PASS
  with a stated LuckPerms-RCON evidence limitation); mirror
  restore byte-exact (boot-churn caveat stated in the rig record).

## Live install window (Nov 14, 2026, 14:36–15:12 CST)

- Script bundle: 28 blocks, all PASS (SCRIPT-OUTPUT.txt, this
  package) — live-jar gate EXACT, census, emptiness 0/10 ×2 +
  pre-shutdown, placed pull-backs EXACT pre- and post-boot,
  backup cap-at-6 check PASS.
- Boot: Done (24.373s) at 14:55:16 CST; SEVERE 0 (whole log);
  Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0 = 1,
  Enabling ExcelAbilityBridge v0.1.1 = 0; guild LOCKED 4 of 5;
  RCON `abilities status` first line verbatim:
  "ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)".

## Still owed (player-class, not discharged by any of the above)

- A real Bedrock /abilities fire by Kailey (.coolgirlkail) —
  also discharges the bridge's long-owed first real Bedrock
  fire (R7). No record in this package claims it.
