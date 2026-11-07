# PUSH PLAN — ExcelProtect 0.1.7 record PR

Prepared Nov 6, 2026 (parent-backlog-driver, GitHub leg staging unit).
Per the backlog Item 17 instructions and Mike's standing GitHub routing
(Oct 25, 2026): publish via the BROWSER route in the next driver unit
(Mike's availability unknown to this background run). Before creating
the branch in the browser, re-check that
`record/excelprotect-0.1.7-20261106` still does not exist. The jar
itself is in Drive (Minecraft WRV "Build files"), NOT on GitHub.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelprotect-0.1.7-20261106`
- Local source dir: `/home/hatch/workspace/records-catchup-20261103/gh-stage-protect017`
  (byte-identical copy of this package under `review-candidates/excelprotect-0.1.7-20261106/`)
- Commit message: `Record ExcelProtect 0.1.7 (Parity) — built Nov 6, 2026; installed live Nov 6, 2026`
- PR title: `Draft source record: ExcelProtect 0.1.7 — Parity`
- PR body: Source record for ExcelProtect 0.1.7 "Parity" (built
  Nov 6, 2026; installed live Nov 6, 2026 by the failsafe, boot
  Done 20.161s, SEVERE 0). Tests 86/86. Two decided features:
  the DeathRecord importer fix (restored marks honored, Lost &
  Found state migration — the live import ran Nov 6, 2026 and
  reconciled 2,274 events exactly, 0 errors) and Telemetry
  kill-field parity on entity_death. Record package: 81 files
  under review-candidates/excelprotect-0.1.7-20261106/, SHA256SUMS-verified. Jar of record:
  ExcelProtect-0.1.7.jar, 246,038 B, SHA-256
  d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124
  (Drive Build files ID 1ASIo7y5urW_5a7ce3NQ_8d401TyYpOKL; source zip
  Drive Source ID 1nDtWxYdBCdpjCV02MFDY20xccay_cUtu). Records
  correction: the Nov 1 audit's claim that RewardGuard/EliteMobs
  read ExcelTelemetry's files was backwards — no in-server plugin
  reads Telemetry's files; the only file consumer is the offline
  progress_report.py (Item 13b audit). Draft record
  PR per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged.

## Files (81)

- `review-candidates/excelprotect-0.1.7-20261106/BUILD-RECORD.md`
- `review-candidates/excelprotect-0.1.7-20261106/CHANGELOG-0.1.7.md`
- `review-candidates/excelprotect-0.1.7-20261106/DEPENDENCIES.md`
- `review-candidates/excelprotect-0.1.7-20261106/FILE-LIST.txt`
- `review-candidates/excelprotect-0.1.7-20261106/INSTALL-PACKET.md`
- `review-candidates/excelprotect-0.1.7-20261106/INSTALLED.txt`
- `review-candidates/excelprotect-0.1.7-20261106/PUSH-PLAN.md`
- `review-candidates/excelprotect-0.1.7-20261106/SHA256SUMS`
- `review-candidates/excelprotect-0.1.7-20261106/SOURCE-PROVENANCE.json`
- `review-candidates/excelprotect-0.1.7-20261106/SOURCE.md`
- `review-candidates/excelprotect-0.1.7-20261106/SPEC.md`
- `review-candidates/excelprotect-0.1.7-20261106/config.yml`
- `review-candidates/excelprotect-0.1.7-20261106/local-build.sh`
- `review-candidates/excelprotect-0.1.7-20261106/plugin.yml`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/admin-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/blocks-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/boot-recovery.log`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/containers-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/deaths-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/entities-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/export-export-20261106T1903073880600.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/imported-lf-state.json`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/imported-sources.json`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/lostfound-players.json`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/restore-marks.json`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/system-2026-11-06.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/undo-op-1794013337139.jsonl.done`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/undo-op-1794013364872.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/undo-op-1794013371838.jsonl.done`
- `review-candidates/excelprotect-0.1.7-20261106/rig-evidence/undo-op-1794013472838.jsonl`
- `review-candidates/excelprotect-0.1.7-20261106/rig/drive.py`
- `review-candidates/excelprotect-0.1.7-20261106/rig/driver-src/plugin.yml`
- `review-candidates/excelprotect-0.1.7-20261106/rig/driver-src/rig/RigElites.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Adapters.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Blocks.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Chains.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/CombatCore.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Containers.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DamageIncidents.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DamageLedger.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DamageReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DamageWatch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DealtIncidents.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DealtReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DealtWatch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/DurableWriter.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/EconomyWatch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/EliteProbe.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Entities.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/ExcelProtect.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Exporter.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/GpAdapter.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/HealingIncidents.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/HealingReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/HealingWatch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/ImportedLfState.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Importer.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/InspectReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/ItemChainReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/ItemEvents.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/ItemFact.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/LiveInventory.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/LocationWatch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/McmmoAdapter.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Players.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/PlaytimeReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Recovery.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/RgOrigin.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Rollback.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Timelines.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/Watch.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/WhereReport.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/WorldEditHook.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/api/EconomyEvent.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/api/ExcelProtectAPI.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/api/FightTableEvent.java`
- `review-candidates/excelprotect-0.1.7-20261106/src/com/winkreyes/excelprotect/api/RecordEvent.java`
- `review-candidates/excelprotect-0.1.7-20261106/tests/com/winkreyes/excelprotect/ProtectTest.java`

## After the push

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 81 files.
- Drive deposit already DONE (jar + source zip, byte-verified).
- Apply the upkeep B12/B14 correction for 0.1.7 in that same pass
  (B12: add this PR; B14: add the 0.1.7 jar + source zip Drive IDs
  from SOURCE-PROVENANCE.json).
