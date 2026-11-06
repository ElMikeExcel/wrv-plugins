# PUSH PLAN — ExcelProtect 0.1.3 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (0.1.3 build lane, staging only — no push made).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route. Before creating the branch in
the browser, re-check that `record/excelprotect-0.1.3-20261105` still
does not exist; if the branch unexpectedly exists at main's head with no
extra commits, use it as-is. The jar itself goes to Drive (Minecraft WRV
"Build files"), NOT to GitHub (connector/browser text-only rule).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelprotect-0.1.3-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-protect013`
  (a byte-identical copy of this package under
  `review-candidates/excelprotect-0.1.3-20261105/`)
- Commit message: `Record ExcelProtect 0.1.3 (damage dealt + healing incidents) — built Nov 5, 2026; install pending gated window`
- ONE commit with all 56 files. All files are UTF-8 text; no binaries,
  no deletions. PR stays an OPEN DRAFT, never merged (standing rule).

## Files (56)

- `review-candidates/excelprotect-0.1.3-20261105/BUILD-RECORD.md`
- `review-candidates/excelprotect-0.1.3-20261105/CHANGELOG-0.1.3.md`
- `review-candidates/excelprotect-0.1.3-20261105/DEPENDENCIES.md`
- `review-candidates/excelprotect-0.1.3-20261105/DEPENDENCIES.sha256`
- `review-candidates/excelprotect-0.1.3-20261105/FILE-LIST.txt`
- `review-candidates/excelprotect-0.1.3-20261105/INSTALL-PACKET.md`
- `review-candidates/excelprotect-0.1.3-20261105/PUSH-PLAN.md`
- `review-candidates/excelprotect-0.1.3-20261105/SHA256SUMS`
- `review-candidates/excelprotect-0.1.3-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/excelprotect-0.1.3-20261105/SOURCE.md`
- `review-candidates/excelprotect-0.1.3-20261105/SPEC.md`
- `review-candidates/excelprotect-0.1.3-20261105/TEST-REPORT.md`
- `review-candidates/excelprotect-0.1.3-20261105/config.yml`
- `review-candidates/excelprotect-0.1.3-20261105/local-build.sh`
- `review-candidates/excelprotect-0.1.3-20261105/plugin.yml`
- `review-candidates/excelprotect-0.1.3-20261105/rig-evidence/boot-0.1.3.log`
- `review-candidates/excelprotect-0.1.3-20261105/rig-evidence/combat-2026-11-05.jsonl`
- `review-candidates/excelprotect-0.1.3-20261105/rig-evidence/dealt-2026-11-05.jsonl`
- `review-candidates/excelprotect-0.1.3-20261105/rig-evidence/healing-2026-11-05.jsonl`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Adapters.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Blocks.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/CombatCore.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Containers.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DamageIncidents.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DamageLedger.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DamageReport.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DamageWatch.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DealtIncidents.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DealtReport.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DealtWatch.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/DurableWriter.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/EconomyWatch.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/EliteProbe.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Entities.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/ExcelProtect.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Exporter.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/GpAdapter.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/HealingIncidents.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/HealingReport.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/HealingWatch.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Importer.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/InspectReport.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/ItemEvents.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/ItemFact.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/McmmoAdapter.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Players.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Recovery.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Rollback.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/Watch.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/api/EconomyEvent.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/api/ExcelProtectAPI.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/api/FightTableEvent.java`
- `review-candidates/excelprotect-0.1.3-20261105/src/com/winkreyes/excelprotect/api/RecordEvent.java`
- `review-candidates/excelprotect-0.1.3-20261105/tests/com/winkreyes/excelprotect/ProtectTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the new
  branch, 56 files.
- Drive deposit: `ExcelProtect-0.1.3.jar` (165,366 B, SHA-256
  `f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`)
  to the Minecraft WRV "Build files" folder; source folder per the
  standing convention. Byte-verify after upload.
