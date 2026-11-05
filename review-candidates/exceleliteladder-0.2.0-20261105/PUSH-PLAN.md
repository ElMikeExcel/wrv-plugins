# PUSH PLAN — ExcelEliteLadder 0.2.0 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (records close-out). Per Mike's standing GitHub
routing rule (Oct 25, 2026), a connector approval card that goes
unanswered drops the write to the BROWSER route: a connector
`create_branch` call was started in this pass, its card was not answered,
and the call was abandoned — do not shepherd it (if a card is still
pending, it expires on its own). Before creating the branch in the
browser, re-check that `record/exceleliteladder-0.2.0-20261105` still does
not exist (it was confirmed absent by a 404 ref lookup in this pass, and
main's head was `c4fca028f223c5104d9fb25c0a36f429c50a7879`); if the branch
unexpectedly exists at main's head with no extra commits, use it as-is.
No connector `push_files` was ever issued — nothing is partially pushed.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main` (head at preparation time: `c4fca028f223c5104d9fb25c0a36f429c50a7879`)
- New branch: `record/exceleliteladder-0.2.0-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-eliteladder`
  (a byte-identical copy of this package under
  `review-candidates/exceleliteladder-0.2.0-20261105/`)
- Commit message: `Record ExcelEliteLadder 0.2.0 (ladder table moves to ladder.yml config) — installed Nov 5, 2026`
- ONE commit with all 45 files. All files are UTF-8 text; no binaries,
  no deletions.

## Files (45)

- `review-candidates/exceleliteladder-0.2.0-20261105/BUILD-RECORD.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/CHANGELOG-0.2.0.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/DEPENDENCIES.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/DEPENDENCIES.sha256`
- `review-candidates/exceleliteladder-0.2.0-20261105/FILE-LIST.txt`
- `review-candidates/exceleliteladder-0.2.0-20261105/FUNCTIONAL-CHANGE.diff`
- `review-candidates/exceleliteladder-0.2.0-20261105/INSTALL-PACKET.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/INSTALLED.txt`
- `review-candidates/exceleliteladder-0.2.0-20261105/MANIFEST-ENTRY.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/PUSH-PLAN.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/README.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/SANITIZATION.diff`
- `review-candidates/exceleliteladder-0.2.0-20261105/SHA256SUMS`
- `review-candidates/exceleliteladder-0.2.0-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/exceleliteladder-0.2.0-20261105/SOURCE.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/TEST-REPORT.md`
- `review-candidates/exceleliteladder-0.2.0-20261105/TEST-RESULTS.txt`
- `review-candidates/exceleliteladder-0.2.0-20261105/base-proof/build-base.sh`
- `review-candidates/exceleliteladder-0.2.0-20261105/build.sh`
- `review-candidates/exceleliteladder-0.2.0-20261105/config.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/ladder.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/plugin.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/rig/evidence/r1-r2-console.log`
- `review-candidates/exceleliteladder-0.2.0-20261105/rig/evidence/r2b-apply-console.log`
- `review-candidates/exceleliteladder-0.2.0-20261105/rig/evidence/r3-broken-boot-latest.log`
- `review-candidates/exceleliteladder-0.2.0-20261105/run-tests.sh`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/ExcelEliteLadder.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/FileApplier.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/LadderConfig.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/LadderConfigException.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/LadderException.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/LadderStep.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/LadderTable.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/MobCombatFile.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/src/com/winkreyes/exceleliteladder/core/StepPlan.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/Check.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/LadderConfigSuite.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/TestMain.java`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/S1-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/S2-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/S3-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/S4-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/S5-norm-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/old-S0-MobCombatSettings.yml`
- `review-candidates/exceleliteladder-0.2.0-20261105/tests/fixtures/old-S1-MobCombatSettings.yml`

## Pull request

- Title: `Draft source record: ExcelEliteLadder 0.2.0 ladder table to config`
- Base: `main` · Head: `record/exceleliteladder-0.2.0-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelEliteLadder 0.2.0 (installed live Nov 5, 2026). The 2.0 change (Mike's approval, 2.0 decision sheet item 3): the mob ladder table leaves the jar for plugins/ExcelEliteLadder/ladder.yml under strict validation — any defect refuses the whole table with the defect named and the last-good table keeps running; the shipped ladder.yml equals the 0.1.2 hardcoded table value-for-value (migration equality suite), so behavior is unchanged. Record package: 45 files under review-candidates/exceleliteladder-0.2.0-20261105/, SHA256SUMS-verified, text-only per record anatomy (dependency jars pinned by hash in DEPENDENCIES.sha256; the base-proof rebuilt 0.1.2 jar is a binary and is not included — its build script is). Jar of record: ExcelEliteLadder-0.2.0.jar, 40,475 B, SHA-256 781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0 (deposited in Drive Minecraft WRV > Build files). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 45.
- Spot-check `review-candidates/exceleliteladder-0.2.0-20261105/SHA256SUMS`
  and `SOURCE-PROVENANCE.json` on the branch match the local package
  (the package itself verifies clean: `sha256sum -c SHA256SUMS`).
- Main untouched.

## Already done in this pass (not part of the push)

- Drive 'Minecraft WRV' > Build files: `ExcelEliteLadder-0.2.0.jar`
  (40,475 B, SHA-256 `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`,
  Drive ID `1t8MuoK5uWQTsPdASDnlkjPEUfyCDjZ1T`, MD5
  `5859a4e2cff69d2f73e4d06830b99cba` = local, read-back verified).
- Drive 'Minecraft WRV' > Source: `ExcelEliteLadder-0.2.0-source.zip`
  (this package zipped; Drive ID `1LGB-HfacRXjmhWVtkhlYNWRDNrfat7hd`,
  read-back verified by size + Drive MD5 = local MD5 of the final zip).
