# PUSH PLAN — ExcelAutoSort 0.2.0 record PR (for the parent to execute via the browser route)

Prepared Nov 7, 2026 (board-manager run 5).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route (Mike on his day off, not
shepherding taps). Before creating the branch in the browser,
re-check that `record/excelautosort-0.2.0-20261107` still does not
exist; if the branch unexpectedly exists at main's head with no
extra commits, use it as-is. The jar itself goes to Drive
(Minecraft WRV "Build files"), NOT to GitHub (text-only rule).
Drive leg is DONE + byte-verified (Nov 7, 2026, board-manager
run 5); its file IDs are already filled into SOURCE-PROVENANCE.json
and the PR body below.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelautosort-0.2.0-20261107`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-autosort020`
  (a byte-identical copy of this package under
  `review-candidates/excelautosort-0.2.0-20261107/`)
- Commit message: `Record ExcelAutoSort 0.2.0 "Max Organization" — built Nov 7, 2026; installed live Nov 7, 2026`
- PR title: `Draft source record: ExcelAutoSort 0.2.0 — Max Organization`
- PR body: Source record for ExcelAutoSort 0.2.0 "Max Organization"
  (built Nov 7, 2026; INSTALLED LIVE Nov 7, 2026, 9:07–9:54 AM CST
  by wrv-install-failsafe; BACKUP FIRST "ExcelAutoSort pre-0.2.0
  20261107" with cap-at-6 prune of the oldest manual "ExcelAutoSort
  pre-0.1.0 20261107"; boot Done 19.865s, awake line build
  EXC-AUTOSORT-0.2.0-WRV-20261107-01-78a48a with protect sink BOUND,
  gp rights BOUND, max-org on (economy BOUND, entitlements 0), audit
  sink BOUND (stream: items); 76 plugins, guildgate LOCKED 3 of 5
  unchanged, SEVERE 0 attributable, TPS 19.9/20.0/20.0). 0.2.0 adds
  Max Organization (SPEC §15): rent $1,000/30 days OR buy $15,000
  permanent via Vault — rent never counts toward buy; a paged
  window onto every chest in radius 16 the player holds configure
  rights over; takes + filter-respecting smart deposits, fail-closed
  (items stay in their real chests); every purchase/take/deposit
  Protect-logged. Everything 0.1.0 does is unchanged. Tests: unit
  29/29; acceptance rig 76/76 (run13 — full 0.1.0 regression set +
  25 MaxOrg cases). Record package: 55 files under
  review-candidates/excelautosort-0.2.0-20261107/,
  SHA256SUMS-verified; carries BOTH the 0.2.0 and preserved 0.1.0
  editions of BUILD-RECORD / INSTALL-PACKET / PUSH-NOTES. Jar of
  record: ExcelAutoSort-0.2.0.jar, 154,773 B, SHA-256
  6164d943cfaeef498e8966978d9c3255d2352ab81ccc0b36875064d4d0013138
  (deposited in Drive Minecraft WRV > Build files, file ID
  1NiOeEV-9WxQtdV8tzmVhwqtLLJiOXLi-, MD5 8e0d1bf5fe4637442e1748f54645f96b
  matched; source zip ExcelAutoSort-0.2.0-source.zip in Drive
  Minecraft WRV > Source, file ID 131AIgLwBA-gZ27slHo6AKxNKtflFWpuh,
  MD5 55f83cfc6b177e2c129f52298e4406f4 matched).
  Player proofs still owed: Bedrock MaxOrg tap flow end-to-end,
  Java GUI flow, one real rent + one real buy verified against
  Protect purchase lines, live lapse observation. Draft record PR
  per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (55)

- `review-candidates/excelautosort-0.2.0-20261107/BUILD-RECORD-0.1.0.md`
- `review-candidates/excelautosort-0.2.0-20261107/BUILD-RECORD.md`
- `review-candidates/excelautosort-0.2.0-20261107/FILE-LIST.txt`
- `review-candidates/excelautosort-0.2.0-20261107/FINGERPRINTS.md`
- `review-candidates/excelautosort-0.2.0-20261107/INSTALL-PACKET-0.1.0.md`
- `review-candidates/excelautosort-0.2.0-20261107/INSTALL-PACKET.md`
- `review-candidates/excelautosort-0.2.0-20261107/INSTALLED.txt`
- `review-candidates/excelautosort-0.2.0-20261107/PUSH-NOTES-0.1.0.md`
- `review-candidates/excelautosort-0.2.0-20261107/PUSH-NOTES.md`
- `review-candidates/excelautosort-0.2.0-20261107/PUSH-PLAN.md`
- `review-candidates/excelautosort-0.2.0-20261107/SHA256SUMS`
- `review-candidates/excelautosort-0.2.0-20261107/SOURCE-PROVENANCE.json`
- `review-candidates/excelautosort-0.2.0-20261107/SOURCE.md`
- `review-candidates/excelautosort-0.2.0-20261107/SPEC.md`
- `review-candidates/excelautosort-0.2.0-20261107/config.yml`
- `review-candidates/excelautosort-0.2.0-20261107/local-build.sh`
- `review-candidates/excelautosort-0.2.0-20261107/packet/maxorg.yml`
- `review-candidates/excelautosort-0.2.0-20261107/plugin.yml`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/BuildInfo.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/ExcelAutoSort.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/Messages.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/Names.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/bedrock/BedrockFormsHook.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/cmd/AutoSortCommand.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/ActivesParse.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/BufferCore.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/ChestKeys.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/FilterCore.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/MaxOrgCore.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/Mode.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/ProtectEvents.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/SelectorModel.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/core/SortCore.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/economy/VaultGateway.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/BufferService.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/ChestListener.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/FilterEngine.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/MaxOrgService.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/SelectorFlow.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/engine/SortEngine.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/gui/MaxOrgGui.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/gui/MaxOrgSession.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/gui/SelectorGui.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/gui/SelectorSession.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/hook/AutoSortExpansion.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/hook/EconomyPort.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/hook/GpRights.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/hook/GpRightsHook.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/hook/ProtectSink.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/protect/ProtectGateway.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/store/BufferEntry.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/store/ChestRecord.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/store/DataStore.java`
- `review-candidates/excelautosort-0.2.0-20261107/src/com/winkreyes/excelautosort/store/ItemCodec.java`
- `review-candidates/excelautosort-0.2.0-20261107/tests/com/winkreyes/excelautosort/AutoSortTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 55 files.
- Drive deposit DONE before the push (jar + source zip,
  byte-verified, IDs in the PR body above and in
  SOURCE-PROVENANCE.json).
