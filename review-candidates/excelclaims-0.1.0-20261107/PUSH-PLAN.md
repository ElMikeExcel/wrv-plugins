# PUSH PLAN — ExcelClaims 0.1.0 record PR (for the parent to execute via the browser route)

Prepared Nov 7, 2026 (parent-backlog-driver, GitHub leg).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route (Mike asleep/unknown at
3 AM). Before creating the branch in the browser, re-check that
`record/excelclaims-0.1.0-20261107` still does not exist; if the
branch unexpectedly exists at main's head with no extra commits,
use it as-is. The jar itself goes to Drive (Minecraft WRV "Build
files"), NOT to GitHub (text-only rule). Drive leg is already DONE
+ byte-verified (see SOURCE-PROVENANCE.json).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelclaims-0.1.0-20261107`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-claims010`
  (a byte-identical copy of this package under
  `review-candidates/excelclaims-0.1.0-20261107/`)
- Commit message: `Record ExcelClaims 0.1.0 (clean-room house claims, GriefPrevention facade) — built Nov 7, 2026; installed live Nov 7, 2026`
- PR title: `Draft source record: ExcelClaims 0.1.0 — clean-room house claims, GriefPrevention facade`
- PR body: Source record for ExcelClaims 0.1.0 (built Nov 7, 2026; INSTALLED LIVE Nov 7, 2026, 2:34–3:00 AM CST by the failsafe; boot Done 19.882s, SEVERE 0 attributable, 76 plugins, guildgate LOCKED 3 of 5 unchanged; live census 10 claims / 11 accounts reconciled EXACT, import 10 + 11, orphans 0). First house claims plugin: clean-room reimplementation of GriefPrevention-as-WRV-uses-it (36 parity items) with an original me.ryanhamshire.GriefPrevention facade so all 11 consumers (35 touchpoints) keep working with zero rebuilds. Tests: unit 45/45, link-check PASS, rig FACADE-SMOKE 14/14. Record package: 55 files under review-candidates/excelclaims-0.1.0-20261107/, SHA256SUMS-verified. Jar of record: ExcelClaims-0.1.0.jar, 96,545 B, SHA-256 838a85b717e5a5617d3dbd48f151efd03ded3269ce4d458dec070c4a2edfbee8 (deposited in Drive Minecraft WRV > Build files, file ID 1mmsccRQ4vNTh0EhcAdSoDP6KvySpkM3p; source zip ExcelClaims-0.1.0-src.zip in Drive Minecraft WRV > Source, file ID 1pZiQNSIJBfCTmrdUZbZ4WqL5xQglYuPg). Player proofs still owed: packet Step 8 (own-claim build/container + neighbor denial, /claimslist parity, shovel resize, spawn admin-claim protection, Landmark renter build + AutoSort filter-chest configure case). Draft record PR per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (55)

- `review-candidates/excelclaims-0.1.0-20261107/BUILD-RECORD.md`
- `review-candidates/excelclaims-0.1.0-20261107/FILE-LIST.txt`
- `review-candidates/excelclaims-0.1.0-20261107/FINGERPRINTS.md`
- `review-candidates/excelclaims-0.1.0-20261107/INSTALL-PACKET.md`
- `review-candidates/excelclaims-0.1.0-20261107/INSTALLED.txt`
- `review-candidates/excelclaims-0.1.0-20261107/INVENTORY.md`
- `review-candidates/excelclaims-0.1.0-20261107/PUSH-PLAN.md`
- `review-candidates/excelclaims-0.1.0-20261107/SHA256SUMS`
- `review-candidates/excelclaims-0.1.0-20261107/SOURCE-PROVENANCE.json`
- `review-candidates/excelclaims-0.1.0-20261107/SOURCE.md`
- `review-candidates/excelclaims-0.1.0-20261107/config.yml`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/auditlog/BlackBox.java`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/auditlog/EventLog.java`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/auditlog/Json.java`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelclaims-0.1.0-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelclaims-0.1.0-20261107/local-build.sh`
- `review-candidates/excelclaims-0.1.0-20261107/plugin.yml`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/Accrual.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/BuildInfo.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ClaimRecord.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ClaimService.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ClaimStore.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ClaimsConfig.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/Commands.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/Core.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/GpExporter.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/GpImporter.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/PlayerRecord.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ProtectGateway.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ProtectionListener.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/ToolListener.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/Trust.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/VaultBridge.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/com/winkreyes/excelclaims/Visualizer.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/Claim.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/ClaimPermission.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/CreateClaimResult.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/DataStore.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/GriefPrevention.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/PlayerData.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimChangeEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimCreatedEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimDeletedEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimModifiedEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimResizeEvent.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/AccrualTest.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/ClaimRulesTest.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/FacadeSignatureTest.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/ImportExportTest.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/StoreRoundTripTest.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/TestKit.java`
- `review-candidates/excelclaims-0.1.0-20261107/tests/com/winkreyes/excelclaims/TrustMatrixTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 55 files.
- Drive deposit already DONE (jar + source zip, byte-verified).
