# PUSH PLAN — ExcelClaims 0.1.1 record PR (for the parent to execute via the browser route)

Prepared Nov 7, 2026 (board-manager run 5).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route (Mike on his day off, not
shepherding taps). Before creating the branch in the browser,
re-check that `record/excelclaims-0.1.1-20261107` still does not
exist; if the branch unexpectedly exists at main's head with no
extra commits, use it as-is. The jar itself goes to Drive
(Minecraft WRV "Build files"), NOT to GitHub (text-only rule).
Drive leg is DONE + byte-verified (Nov 7, 2026, board-manager
run 5, via the Drive connector); its file IDs are already filled
into SOURCE-PROVENANCE.json and the PR body below. SOURCE.md rode
the Drive deposit alongside, as ExcelClaims-SOURCE.md in Source.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelclaims-0.1.1-20261107`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-claims011`
  (a byte-identical copy of this package under
  `review-candidates/excelclaims-0.1.1-20261107/`)
- Commit message: `Record ExcelClaims 0.1.1 (expiration-incident fix) — built Nov 7, 2026; installed live Nov 7, 2026`
- PR title: `Draft source record: ExcelClaims 0.1.1 — expiration-incident fix`
- PR body: Source record for ExcelClaims 0.1.1 (built Nov 7, 2026;
  INSTALLED LIVE Nov 7, 2026, 10:04–10:37 AM CST by
  wrv-install-failsafe; gate census EXACT 10 claims (1 admin, 0
  subdivisions) / 11 accounts with live clocks 99999/99999/99999;
  BACKUP FIRST "ExcelClaims pre-0.1.1 20261107" with cap-at-6 prune
  of the oldest manual "ExcelClaims pre-0.1.0 20261107"; jar swap
  0.1.0 → 0.1.1 AND the expiration-clock restore 99999 → 7/14/60 in
  the same window, config backed up first and read-back verified;
  boot Done 19.390s, enable line EXACT 0.1.1 build
  EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c, 76 plugins unchanged,
  guildgate LOCKED 3 of 5 unchanged, SEVERE 0 attributable;
  POST-INSTALL PROOF PASS — status still 10/11 at 9m26s past enable,
  no "expiring claim" line for any live claim). 0.1.1 is THE
  EXPIRATION-INCIDENT FIX: 0.1.0's sweep expired all 9 imported
  player claims at the Nov 7 04:57 boot because imported
  claims/players carried all-zero dates; recovered the same morning
  (clocks frozen at 99999 + re-import, field-diff of all 10 claims
  vs the GriefPrevention originals matched exactly). 0.1.1 = importer
  date seeding (claim created/modified from GP file mtime, player
  last-seen from Bukkit last-played → file mtime) + sweep safety
  (basis = newest evidence across stored stamp / live Bukkit consult
  / claim stamps; unknown basis NEVER expires). Nothing else changes
  (facade, trust, config keys, export format). Tests: unit 55/55
  (45 carried + 10 new pinning the incident shape), rig A
  persistence-across-reboot + FACADE-SMOKE 14/14 + export
  byte-identical, rig B sweep soak (genuinely-absent fixture expired
  on schedule; active + recovered-store fixtures survived). Record
  package: 58 files under review-candidates/excelclaims-0.1.1-20261107/,
  SHA256SUMS-verified. Jar of record: ExcelClaims-0.1.1.jar,
  97,503 B, SHA-256
  ff5ea0810d1df5866a423ac126e3145354bca25d2600cfc42a5536362b540706
  (deposited in Drive Minecraft WRV > Build files, file ID
  1F8gwhkGJ9fpK21KNnQpBEZA4Ym7xfbjN, MD5 f1514b6935fc857448b446423fbcad87
  matched; source zip ExcelClaims-0.1.1-src.zip in Drive
  Minecraft WRV > Source, file ID 1hlp8ElqG3uf23v0EKC2E4vYq_d1bFmkJ,
  MD5 ce96d40ac9f8950cc75f4afe2d2c78b7 matched; SOURCE.md alongside
  as ExcelClaims-SOURCE.md, ID 1crh1RO_GqZQMO97bUExCmsUf03CJoH6d).
  Draft record PR per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (58)

- `review-candidates/excelclaims-0.1.1-20261107/BUILD-RECORD.md`
- `review-candidates/excelclaims-0.1.1-20261107/FILE-LIST.txt`
- `review-candidates/excelclaims-0.1.1-20261107/FINGERPRINTS.md`
- `review-candidates/excelclaims-0.1.1-20261107/INSTALL-PACKET-0.1.1.md`
- `review-candidates/excelclaims-0.1.1-20261107/INSTALLED-0.1.1.txt`
- `review-candidates/excelclaims-0.1.1-20261107/INVENTORY.md`
- `review-candidates/excelclaims-0.1.1-20261107/PUSH-NOTES-0.1.1.md`
- `review-candidates/excelclaims-0.1.1-20261107/PUSH-PLAN.md`
- `review-candidates/excelclaims-0.1.1-20261107/SHA256SUMS`
- `review-candidates/excelclaims-0.1.1-20261107/SOURCE-PROVENANCE.json`
- `review-candidates/excelclaims-0.1.1-20261107/SOURCE.md`
- `review-candidates/excelclaims-0.1.1-20261107/config.yml`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/auditlog/BlackBox.java`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/auditlog/EventLog.java`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/auditlog/Json.java`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelclaims-0.1.1-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelclaims-0.1.1-20261107/local-build-0.1.1.sh`
- `review-candidates/excelclaims-0.1.1-20261107/local-build.sh`
- `review-candidates/excelclaims-0.1.1-20261107/plugin.yml`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/Accrual.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/BuildInfo.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ClaimRecord.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ClaimService.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ClaimStore.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ClaimsConfig.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/Commands.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/Core.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/GpExporter.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/GpImporter.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/PlayerRecord.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ProtectGateway.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ProtectionListener.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/ToolListener.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/Trust.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/VaultBridge.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/com/winkreyes/excelclaims/Visualizer.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/Claim.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/ClaimPermission.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/CreateClaimResult.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/DataStore.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/GriefPrevention.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/PlayerData.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimChangeEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimCreatedEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimDeletedEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimModifiedEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimResizeEvent.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/AccrualTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/ClaimRulesTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/ExpirationSafetyTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/FacadeSignatureTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/ImportExportTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/StoreRoundTripTest.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/TestKit.java`
- `review-candidates/excelclaims-0.1.1-20261107/tests/com/winkreyes/excelclaims/TrustMatrixTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 58 files.
- Drive deposit DONE before the push (jar + source zip +
  SOURCE.md, byte-verified, IDs in the PR body above and in
  SOURCE-PROVENANCE.json).
