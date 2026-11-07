# PUSH PLAN — ExcelClaims 0.1.2 record PR (for the parent to execute via the browser route)

Prepared Nov 7, 2026 (board-manager, parent-backlog Item 24 —
desk staging only; no push, Drive deposit, or browser work was
done in this pass).
Per Mike's standing GitHub routing rule (Oct 25, 2026) and
PUSH-NOTES-0.1.2.md, the parent executes this push batched in
ONE `push_files` call on the connector route while Mike is
awake, or via the BROWSER route if he is asleep/busy. Before
creating the branch, re-check that
`record/excelclaims-0.1.2-20261107` still does not exist; if the
branch unexpectedly exists at main's head with no extra
commits, use it as-is. The jar itself goes to Drive
(Minecraft WRV "Build files"), NOT to GitHub (text-only rule).
Drive leg is PENDING at desk-staging time; its file IDs are
left as "pending" placeholders in SOURCE-PROVENANCE.json and
in the PR body below, to be filled by the parent after the
Drive deposit + byte verification. SOURCE.md rides the Drive
deposit alongside, as ExcelClaims-SOURCE.md in Source.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelclaims-0.1.2-20261107`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-claims012`
  (a byte-identical copy of this package under
  `review-candidates/excelclaims-0.1.2-20261107/`)
- Commit message: `Record ExcelClaims 0.1.2 (per-player expiration) — built Nov 7, 2026; installed live Nov 7, 2026`
- PR title: `Draft source record: ExcelClaims 0.1.2 — per-player expiration`
- PR body: Source record for ExcelClaims 0.1.2 (built Nov 7, 2026;
  INSTALLED LIVE Nov 7, 2026, 12:34–1:09 PM CST by
  wrv-install-failsafe; emptiness list 0/10 at 12:35:26 PM and
  12:36:46 PM CST, pre-shutdown list 0/10 at 12:49:16 PM;
  pre-window census EXACT 10 claims (1 admin, 0 subdivisions) /
  11 accounts on ExcelClaims 0.1.1 build
  EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c; BACKUP FIRST
  "ExcelClaims pre-0.1.2 20261107" with cap-at-6 prune of the
  oldest manual "ExcelClaims recovery 20261107"; jar swap
  0.1.1 → 0.1.2 AND the expiration-config rewrite to
  `default-days: never` in the same window, config backed up
  first (config.yml.bak-20261107-pre-0.1.2) and read back
  verified (check-interval-minutes: 60, default-days: never,
  empty players:); boot Done 20.678s, enable line EXACT 0.1.2
  build EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d, 76 plugins
  unchanged, guildgate LOCKED 3 of 5 unchanged, SEVERE 0
  attributable; POST-INSTALL PROOF PASS — status still 10/11
  at 1:03:39 PM, no "expiring claim" line, no "expiration
  check skipped" line, spot-reports Kailey + Marvin each
  "never (server default)"). 0.1.2 is the PER-PLAYER
  EXPIRATION build (Mike's ruling, Nov 7, 2026): the three
  global expiration clocks (7/14/60) and the block-count
  exemptions are replaced by ONE inactivity threshold settable
  per player — `expiration.default-days` (`never` = infinite)
  plus `expiration.players` overrides — with the new staff
  command `/excelclaims expire <player> [days|never]`; the
  0.1.1 safety floor is retained verbatim (a claim whose
  owner's last-seen basis is unknown NEVER expires under any
  setting). Tests: unit 66/66 (55 carried + ExpirationSafetyTest
  adapted to 12 + PerPlayerExpirationTest 9 new), link-check
  PASS, rig A persistence-across-reboot + FACADE-SMOKE 14/14 +
  export byte-identical, rig B mixed-fixture soak (Eric's
  finite-expired claim expired alone; Nina's `never` claim and
  Amy's active claim survived; a console set Amy 30 → 45
  persisted through reboot; Ghost unknown-basis claim survived
  with skipped-unknown count exactly 1). Record package: 61
  files under review-candidates/excelclaims-0.1.2-20261107/,
  SHA256SUMS-verified. Jar of record: ExcelClaims-0.1.2.jar,
  99,370 B, SHA-256
  b4663fa3d377bafa06803f8b4d21294ac1ea6b720614a263cc5fb3d7ff120c5d
  (Drive deposit PENDING — parent leg; local MD5
  757e6132861a941d07f6961b72e3fba6; source zip
  ExcelClaims-0.1.2-src.zip, 113,714 B, SHA-256
  2f3d8c474c85e42328ba848599018d7f5cdf432b1c5e94cf8da77036792dab4a,
  local MD5 8211ac28828f61864a14a4d4436a583b; SOURCE.md alongside
  as ExcelClaims-SOURCE.md, pending). Draft record PR per
  standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (61)

- `review-candidates/excelclaims-0.1.2-20261107/BUILD-RECORD.md`
- `review-candidates/excelclaims-0.1.2-20261107/DESIGN-0.1.2-per-player-expiration.md`
- `review-candidates/excelclaims-0.1.2-20261107/FILE-LIST.txt`
- `review-candidates/excelclaims-0.1.2-20261107/FINGERPRINTS.md`
- `review-candidates/excelclaims-0.1.2-20261107/INSTALL-PACKET-0.1.2.md`
- `review-candidates/excelclaims-0.1.2-20261107/INSTALLED-0.1.2.txt`
- `review-candidates/excelclaims-0.1.2-20261107/INVENTORY.md`
- `review-candidates/excelclaims-0.1.2-20261107/PUSH-NOTES-0.1.2.md`
- `review-candidates/excelclaims-0.1.2-20261107/PUSH-PLAN.md`
- `review-candidates/excelclaims-0.1.2-20261107/SHA256SUMS`
- `review-candidates/excelclaims-0.1.2-20261107/SOURCE-PROVENANCE.json`
- `review-candidates/excelclaims-0.1.2-20261107/SOURCE.md`
- `review-candidates/excelclaims-0.1.2-20261107/config.yml`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/auditlog/BlackBox.java`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/auditlog/EventLog.java`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/auditlog/Json.java`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Json.java`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Ledger.java`
- `review-candidates/excelclaims-0.1.2-20261107/linkcheck/stubs/com/winkreyes/excelprotect/Settings.java`
- `review-candidates/excelclaims-0.1.2-20261107/local-build-0.1.1.sh`
- `review-candidates/excelclaims-0.1.2-20261107/local-build-0.1.2.sh`
- `review-candidates/excelclaims-0.1.2-20261107/local-build.sh`
- `review-candidates/excelclaims-0.1.2-20261107/plugin.yml`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/Accrual.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/BuildInfo.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ClaimRecord.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ClaimService.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ClaimStore.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ClaimsConfig.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/Commands.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/Core.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/GpExporter.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/GpImporter.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/PlayerRecord.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ProtectGateway.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ProtectionListener.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/ToolListener.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/Trust.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/VaultBridge.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/com/winkreyes/excelclaims/Visualizer.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/Claim.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/ClaimPermission.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/CreateClaimResult.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/DataStore.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/GriefPrevention.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/PlayerData.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimChangeEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimCreatedEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimDeletedEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimModifiedEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/src/me/ryanhamshire/GriefPrevention/events/ClaimResizeEvent.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/AccrualTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/ClaimRulesTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/ExpirationSafetyTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/FacadeSignatureTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/ImportExportTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/PerPlayerExpirationTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/StoreRoundTripTest.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/TestKit.java`
- `review-candidates/excelclaims-0.1.2-20261107/tests/com/winkreyes/excelclaims/TrustMatrixTest.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 61 files.
- Drive deposit is a parent leg, PENDING at desk-staging time
  (jar + source zip + SOURCE.md, byte-verified on completion;
  fill the pending IDs / MD5s in SOURCE-PROVENANCE.json and the
  PR body above once it lands).
