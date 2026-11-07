# PUSH PLAN — ExcelAutoSort 0.1.0 record PR (for the parent to execute via the browser route)

Prepared Nov 7, 2026 (parent-backlog-driver, GitHub leg).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route (Mike asleep/unknown at
2 AM). Before creating the branch in the browser, re-check that
`record/excelautosort-0.1.0-20261107` still does not exist; if the
branch unexpectedly exists at main's head with no extra commits,
use it as-is. The jar itself goes to Drive (Minecraft WRV "Build
files"), NOT to GitHub (text-only rule). Drive leg is already DONE
+ byte-verified (see SOURCE-PROVENANCE.json).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelautosort-0.1.0-20261107`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-autosort010`
  (a byte-identical copy of this package under
  `review-candidates/excelautosort-0.1.0-20261107/`)
- Commit message: `Record ExcelAutoSort 0.1.0 (house auto-sort + per-chest filter system) — built Nov 6, 2026; installed live Nov 7, 2026`
- PR title: `Draft source record: ExcelAutoSort 0.1.0 — house auto-sort + per-chest filter system`
- PR body: Source record for ExcelAutoSort 0.1.0 (built Nov 6, 2026;
  INSTALLED LIVE Nov 7, 2026, 2:05–2:25 AM CST by the failsafe; boot
  Done 20.352s, SEVERE 0 attributable, 76 plugins, guildgate LOCKED
  3 of 5 unchanged). First house auto-sort plugin: full incumbent
  parity (incl. the one-time import of 4 active states, N=4 EXACT)
  plus the per-chest filter-delete system (whitelist/blacklist,
  contents-seeded selector, itemized confirm sweep to a 7-day
  recovery buffer, hopper filtering, GP configure rights). Tests:
  unit 19/19, acceptance rig 50/50. Record package: 44 files
  under review-candidates/excelautosort-0.1.0-20261107/,
  SHA256SUMS-verified. Jar of record: ExcelAutoSort-0.1.0.jar,
  104,136 B, SHA-256
  00d01234b0a7d06b5c01dce3805f9f5736f0cf2f026512234e29c29c868dc36d
  (deposited in Drive Minecraft WRV > Build files, file ID
  12mDMBX3MuRbE6IrsAllu07ZbvYsESWKC; source zip
  ExcelAutoSort-0.1.0-source.zip in Drive Minecraft WRV > Source,
  file ID 17licvr_W589R8UJFO1VB94Q2mo8FlA8u). Player proofs still
  owed: Bedrock selector end-to-end, Java GUI flow, hopper feed
  into a marked chest, %autosort_status% against a live menu.
  Draft record PR per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (44)

- `review-candidates/excelautosort-0.1.0-20261107/BUILD-RECORD.md`
- `review-candidates/excelautosort-0.1.0-20261107/FILE-LIST.txt`
- `review-candidates/excelautosort-0.1.0-20261107/FINGERPRINTS.md`
- `review-candidates/excelautosort-0.1.0-20261107/INSTALL-PACKET.md`
- `review-candidates/excelautosort-0.1.0-20261107/INSTALLED.txt`
- `review-candidates/excelautosort-0.1.0-20261107/PUSH-PLAN.md`
- `review-candidates/excelautosort-0.1.0-20261107/SHA256SUMS`
- `review-candidates/excelautosort-0.1.0-20261107/SOURCE-PROVENANCE.json`
- `review-candidates/excelautosort-0.1.0-20261107/SOURCE.md`
- `review-candidates/excelautosort-0.1.0-20261107/SPEC.md`
- `review-candidates/excelautosort-0.1.0-20261107/config.yml`
- `review-candidates/excelautosort-0.1.0-20261107/local-build.sh`
- `review-candidates/excelautosort-0.1.0-20261107/plugin.yml`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/BuildInfo.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/ExcelAutoSort.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/Messages.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/Names.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/bedrock/BedrockFormsHook.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/cmd/AutoSortCommand.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/ActivesParse.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/BufferCore.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/ChestKeys.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/FilterCore.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/Mode.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/ProtectEvents.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/SelectorModel.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/core/SortCore.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/engine/BufferService.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/engine/ChestListener.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/engine/FilterEngine.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/engine/SelectorFlow.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/engine/SortEngine.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/gui/SelectorGui.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/gui/SelectorSession.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/hook/AutoSortExpansion.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/hook/GpRights.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/hook/GpRightsHook.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/hook/ProtectSink.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/protect/ProtectGateway.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/store/BufferEntry.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/store/ChestRecord.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/store/DataStore.java`
- `review-candidates/excelautosort-0.1.0-20261107/src/com/winkreyes/excelautosort/store/ItemCodec.java`
- `review-candidates/excelautosort-0.1.0-20261107/tests/com/winkreyes/excelautosort/AutoSortTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 44 files.
- Drive deposit already DONE (jar + source zip, byte-verified).
