# PUSH PLAN — ExcelProtect 0.1.2 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (records prep). Per Mike's standing GitHub routing
rule (Oct 25, 2026), the parent executes this push via the BROWSER route.
Before creating the branch in the browser, re-check that
`record/excelprotect-0.1.2-20261105` still does not exist; if the branch
unexpectedly exists at main's head with no extra commits, use it as-is.
No push of any kind has been made for this package yet.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelprotect-0.1.2-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-protect012`
  (a byte-identical copy of this package under
  `review-candidates/excelprotect-0.1.2-20261105/`)
- Commit message: `Record ExcelProtect 0.1.2 (damage incident logging) — built Nov 5, 2026; install pending gated window`
- ONE commit with all 49 files (the Nov 4 pattern: one batched
  push per package). All files are UTF-8 text; no binaries, no deletions.

## Files (49)

- `review-candidates/excelprotect-0.1.2-20261105/BUILD-RECORD.md`
- `review-candidates/excelprotect-0.1.2-20261105/CHANGELOG-0.1.2.md`
- `review-candidates/excelprotect-0.1.2-20261105/DEPENDENCIES.md`
- `review-candidates/excelprotect-0.1.2-20261105/DEPENDENCIES.sha256`
- `review-candidates/excelprotect-0.1.2-20261105/FILE-LIST.txt`
- `review-candidates/excelprotect-0.1.2-20261105/INSTALL-PACKET.md`
- `review-candidates/excelprotect-0.1.2-20261105/PUSH-PLAN.md`
- `review-candidates/excelprotect-0.1.2-20261105/SHA256SUMS`
- `review-candidates/excelprotect-0.1.2-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/excelprotect-0.1.2-20261105/SOURCE.md`
- `review-candidates/excelprotect-0.1.2-20261105/SPEC.md`
- `review-candidates/excelprotect-0.1.2-20261105/TEST-REPORT.md`
- `review-candidates/excelprotect-0.1.2-20261105/config.yml`
- `review-candidates/excelprotect-0.1.2-20261105/local-build.sh`
- `review-candidates/excelprotect-0.1.2-20261105/plugin.yml`
- `review-candidates/excelprotect-0.1.2-20261105/rig-evidence/boot-0.1.2-run4.log`
- `review-candidates/excelprotect-0.1.2-20261105/rig-evidence/damage-2026-11-05.jsonl`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Adapters.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Blocks.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/CombatCore.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Containers.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/DamageIncidents.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/DamageLedger.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/DamageReport.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/DamageWatch.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/DurableWriter.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/EconomyWatch.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/EliteProbe.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Entities.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/ExcelProtect.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Exporter.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/GpAdapter.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Importer.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/InspectReport.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/ItemEvents.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/ItemFact.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/McmmoAdapter.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Players.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Recovery.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Rollback.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/Watch.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/api/EconomyEvent.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/api/ExcelProtectAPI.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/api/FightTableEvent.java`
- `review-candidates/excelprotect-0.1.2-20261105/src/com/winkreyes/excelprotect/api/RecordEvent.java`
- `review-candidates/excelprotect-0.1.2-20261105/tests/com/winkreyes/excelprotect/ProtectTest.java`

## Pull request

- Title: `Draft source record: ExcelProtect 0.1.2 — damage incident logging`
- Base: `main` · Head: `record/excelprotect-0.1.2-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelProtect 0.1.2 (built Nov 5, 2026; install pending — the gated MANIFEST entry was filed Nov 5, 2026 and the jar installs through the failsafe window). 0.1.2 adds damage incident logging on top of 0.1.1: a pure incident coalescer (DamageIncidents) merges raw damage events into per-incident records with close reasons and death/near-death flags; DamageWatch hooks Bukkit damage/death/quit events at MONITOR and writes the records through the existing ledger (`damage` stream); EliteProbe identifies EliteMobs elites reflectively via EntityTracker (mechanism verified against the real EliteMobs 10.9.12 jar — no PDC/metadata guessing). Console damage report included. Tests: 26/26 pass; deterministic double-build byte-identical. Record package: 49 files under review-candidates/excelprotect-0.1.2-20261105/, SHA256SUMS-verified, text-only per record anatomy (dependency jars pinned by hash in DEPENDENCIES.sha256). Jar of record: ExcelProtect-0.1.2.jar, 138,039 B, SHA-256 26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393 (deposited in Drive Minecraft WRV > Build files, file ID 1ix9QM8I-yqO3VUoJr__tDa0u-SFCjyiA; source zip in Drive > Source, file ID 1HlR05n1xVgaE_K7i6jUGm20Pn4I7qei0). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 49 (check via the GitHub API: state, draft, merged, changed_files).
- Spot-check `review-candidates/excelprotect-0.1.2-20261105/SHA256SUMS`
  and `SOURCE-PROVENANCE.json` on the branch match the local package
  (the package itself verifies clean: `sha256sum -c SHA256SUMS`).
- Main untouched.

## Already done in this pass (not part of the push)

- Drive 'Minecraft WRV' > Build files: `ExcelProtect-0.1.2.jar`
  (138,039 B, SHA-256 `26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`),
  file ID `1ix9QM8I-yqO3VUoJr__tDa0u-SFCjyiA`.
- Drive 'Minecraft WRV' > Source: `excelprotect-0.1.2-source.zip`
  (114,191 B, MD5 `0317e6bc5bdfb851798934ef9f7484aa`),
  file ID `1HlR05n1xVgaE_K7i6jUGm20Pn4I7qei0`.
- Install state: NOT installed at record time. Gated MANIFEST entry
  filed Nov 5, 2026 (PENDING); installation runs through the
  wrv-install-failsafe window.
