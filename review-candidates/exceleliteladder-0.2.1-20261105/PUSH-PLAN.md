# PUSH PLAN — ExcelEliteLadder 0.2.1 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (records prep). Per Mike's standing GitHub routing
rule (Oct 25, 2026), the parent executes this push via the BROWSER route.
Before creating the branch in the browser, re-check that
`record/exceleliteladder-0.2.1-20261105` still does not exist; if the
branch unexpectedly exists at main's head with no extra commits, use it
as-is. No push of any kind has been made for this package yet.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/exceleliteladder-0.2.1-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-eliteladder021`
  (a byte-identical copy of this package under
  `review-candidates/exceleliteladder-0.2.1-20261105/`)
- Commit message: `Record ExcelEliteLadder 0.2.1 (elite speed buff + retuned default ladder table) — built Nov 5, 2026; install pending gated window`
- ONE commit with all 41 files (the Nov 4 pattern: one batched
  push per package). All files are UTF-8 text; no binaries, no deletions.

## Files (41)

- `review-candidates/exceleliteladder-0.2.1-20261105/BUILD-RECORD.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/CHANGELOG-0.2.0.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/CHANGELOG-0.2.1.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/DEPENDENCIES.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/INSTALL-PACKET.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/PUSH-PLAN.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/README.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/SOURCE.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/TEST-REPORT.md`
- `review-candidates/exceleliteladder-0.2.1-20261105/TEST-RESULTS.txt`
- `review-candidates/exceleliteladder-0.2.1-20261105/build.sh`
- `review-candidates/exceleliteladder-0.2.1-20261105/config.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/ladder.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/plugin.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/rig/drive-0.2.1.py`
- `review-candidates/exceleliteladder-0.2.1-20261105/rig/evidence/r1-0.2.1-fresh-boot.log`
- `review-candidates/exceleliteladder-0.2.1-20261105/rig/evidence/r4-0.2.1-speedproof.log`
- `review-candidates/exceleliteladder-0.2.1-20261105/rig/evidence/r5-0.2.1-speedoff.log`
- `review-candidates/exceleliteladder-0.2.1-20261105/run-tests.sh`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/EliteSpeedListener.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/ExcelEliteLadder.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/EliteSpeedSettings.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/FileApplier.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/LadderConfig.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/LadderConfigException.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/LadderException.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/LadderStep.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/LadderTable.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/MobCombatFile.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/src/com/winkreyes/exceleliteladder/core/StepPlan.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/Check.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/EliteSpeedSuite.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/LadderConfigSuite.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/TestMain.java`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/S1-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/S2-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/S3-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/S4-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/S5-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/old-S0-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.1-20261105/tests/fixtures/old-S1-MobCombatSettings.yml`

## Pull request

- Title: `Draft source record: ExcelEliteLadder 0.2.1 — elite speed buff + retuned ladder table`
- Base: `main` · Head: `record/exceleliteladder-0.2.1-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelEliteLadder 0.2.1 (built Nov 5, 2026; install pending — the bundle MANIFEST entry was filed Nov 5, 2026 and the combined window installs through the failsafe). 0.2.1 carries the Nov 5, 2026 retune bundle in one jar: (1) natural elites gain a hidden Speed potion buff (config `elite-speed:` in config.yml; default Speed I, permanent, naturals only, re-asserted on spawn and combat — Mike's knob: a speed potion effect, not an attribute edit); (2) the built-in default ladder table (bundled ladder.yml resource + in-code fallback) becomes the retuned values (damage stepped down ~10%, spawn rates bumped), so the fallback can never resurrect pre-retune tuning. Tests: 272 checks, 0 failed; deterministic double-build byte-identical; rig on Paper 26.3-235 + real EliteMobs 10.9.12 + GuildGate 0.4.0 (fresh-install generation, baseline adoption at the new S3 row, natural-only gate proven by exclusion; positive spawn application is the install packet's post-install player proof). Record package: 41 files under review-candidates/exceleliteladder-0.2.1-20261105/, text-only per record anatomy. Jar of record: ExcelEliteLadder-0.2.1.jar, 47,232 B, SHA-256 d15c5df2cbb2e8c7b6b15ba6b7543b1b0bac59394463416f4a43cc2593f03dd4. Drive deposit confirmed Nov 5, 2026: jar in Drive 'Minecraft WRV' > Build files (file ID 1CrtZbyUgpUYV48KXxLk6Zzr5i2E5iN4A), source zip in Drive 'Minecraft WRV' > Source (file ID 1oY6gMJP5B9_9rETjYNtPW_xc1VKbs5U3). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 41 (check via the GitHub API: state, draft, merged, changed_files).
- Spot-check `review-candidates/exceleliteladder-0.2.1-20261105/ladder.yml`
  and `BUILD-RECORD.md` on the branch match the local package.
- Main untouched.

## Already done / pending (not part of the push)

- Drive 'Minecraft WRV' deposits CONFIRMED Nov 5, 2026: jar
  `ExcelEliteLadder-0.2.1.jar` in Build files (47,232 B, SHA-256
  `d15c5df2cbb2e8c7b6b15ba6b7543b1b0bac59394463416f4a43cc2593f03dd4`,
  file ID `1CrtZbyUgpUYV48KXxLk6Zzr5i2E5iN4A`); source zip
  `exceleliteladder-0.2.1-source.zip` in Source (118,299 B, MD5
  `6ebb2b2e0503cb8de140ef47b308966f`,
  file ID `1oY6gMJP5B9_9rETjYNtPW_xc1VKbs5U3`).
- Install state: NOT installed at record time. The bundle MANIFEST
  entry (jar + ladder.yml + MobCombatSettings hand-alignment +
  `em reload`) was filed Nov 5, 2026; installation runs through the
  wrv-install-failsafe window.
