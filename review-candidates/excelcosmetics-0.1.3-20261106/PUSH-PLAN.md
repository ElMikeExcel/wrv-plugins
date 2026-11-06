# PUSH PLAN — ExcelCosmetics 0.1.3 record PR (for the parent to execute via the browser route)

Prepared Nov 6, 2026 (parent-backlog-driver, GitHub leg staging).
Per Mike's standing GitHub routing rule (Oct 25, 2026), the parent
executes this push via the BROWSER route (Mike's availability unknown
to this background run). Before creating the branch in the browser,
re-check that `record/excelcosmetics-0.1.3-20261106` still does not
exist; if the branch unexpectedly exists at main's head with no
extra commits, use it as-is. The jar itself goes to Drive (Minecraft
WRV "Build files"), NOT to GitHub (text-only rule). Drive leg is
already DONE + byte-verified (see SOURCE-PROVENANCE.json).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelcosmetics-0.1.3-20261106`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-cosmetics013`
  (a byte-identical copy of this package under
  `review-candidates/excelcosmetics-0.1.3-20261106/`)
- Commit message: `Record ExcelCosmetics 0.1.3 (snapshot+compensate refund fix + atomic save) — built Nov 6, 2026; installed live Nov 6, 2026`
- PR title: `Draft source record: ExcelCosmetics 0.1.3 — snapshot+compensate refund fix + atomic save`
- PR body: Source record for ExcelCosmetics 0.1.3 (built Nov 6, 2026;
  INSTALLED LIVE Nov 6, 2026, 3:05–3:40 PM CST by the failsafe; boot
  Done 19.676s, SEVERE 0, 77 plugins). 0.1.3 fixes the confirmed
  refund-while-retaining defect (snapshot+compensate in
  grantAndPersist: false ⇒ nothing changed) and makes players.yml
  saves atomic (AtomicWrite). No pricing/catalog/content changes.
  Tests: 316 checks, 0 failures. Record package: 46 files under
  review-candidates/excelcosmetics-0.1.3-20261106/,
  SHA256SUMS-verified. Jar of record: ExcelCosmetics-0.1.3.jar,
  82,562 B, SHA-256
  e18d5af59657ca44f0d2747e62ffe09a71e2056e32225f069ccc9e0b0ad3162b
  (deposited in Drive Minecraft WRV > Build files, file ID
  1UluDvjud3rT9XxtYZyn0YknWOgN30yNc; source tarball
  ExcelCosmetics-0.1.3-source.tar.gz in Drive Minecraft WRV >
  Source, file ID 12zEqMXiAcid_onxVdiMmLAgDFolC48ud).
  Player proofs still owed: one real purchase per shape (rent +
  own). Draft record PR per standing rule: never merged.
- The browser UI may need several commits (one per directory batch);
  use the exact commit message above for every batch. PR stays an
  OPEN DRAFT, never merged (standing rule).

## Files (46)

- `review-candidates/excelcosmetics-0.1.3-20261106/BRIEF.md`
- `review-candidates/excelcosmetics-0.1.3-20261106/FILE-LIST.txt`
- `review-candidates/excelcosmetics-0.1.3-20261106/INSTALL-PACKET.md`
- `review-candidates/excelcosmetics-0.1.3-20261106/INSTALLED.txt`
- `review-candidates/excelcosmetics-0.1.3-20261106/PUSH-PLAN.md`
- `review-candidates/excelcosmetics-0.1.3-20261106/SHA256SUMS`
- `review-candidates/excelcosmetics-0.1.3-20261106/SOURCE-PROVENANCE.json`
- `review-candidates/excelcosmetics-0.1.3-20261106/SOURCE.md`
- `review-candidates/excelcosmetics-0.1.3-20261106/build.sh`
- `review-candidates/excelcosmetics-0.1.3-20261106/config.yml`
- `review-candidates/excelcosmetics-0.1.3-20261106/plugin.yml`
- `review-candidates/excelcosmetics-0.1.3-20261106/rebuild-on-box.sh`
- `review-candidates/excelcosmetics-0.1.3-20261106/rig-driver/build-driver.sh`
- `review-candidates/excelcosmetics-0.1.3-20261106/rig-driver/plugin.yml`
- `review-candidates/excelcosmetics-0.1.3-20261106/rig-driver/src/cosmeticrig/CosmeticRigDriver.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/run-tests.sh`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/AdminCommand.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/BedrockPicker.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/ChatListener.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/ConfigCatalog.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/CosmeticsCommand.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/EmoteCommand.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/ExcelCosmetics.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/PickerGui.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/PickerListener.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/PickerModel.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/StateStore.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/VaultEconomy.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/AtomicWrite.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/Catalog.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/ChatFormatter.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/ColorMath.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/EmoteDefs.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/Grant.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/GrantStore.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/Kind.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/PlayerState.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/PurchaseService.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/src/com/winkreyes/excelcosmetics/core/TimeText.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/Check.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestCatalog.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestCompensation.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestFormat.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestGrants.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestMain.java`
- `review-candidates/excelcosmetics-0.1.3-20261106/tests/TestPurchase.java`

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head = the
  new branch, 46 files.
- Drive deposit already DONE (jar + source tarball, byte-verified).
