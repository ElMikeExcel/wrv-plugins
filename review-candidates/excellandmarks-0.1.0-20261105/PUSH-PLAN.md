# PUSH PLAN — ExcelLandmarks 0.1.0 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (records close-out). Per the parent's hard rule for
this pass: NO connector writes were attempted at all (connector GitHub
writes stall on approval cards); this leg is STAGED ONLY for the
parent's browser route. Before creating the branch in the browser,
re-check that `record/excellandmarks-0.1.0-20261105` does not exist yet,
and that main's head is still `c4fca028f223c5104d9fb25c0a36f429c50a7879`
(if main has moved, branch from the new head and note it — the record
folder is new either way).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main` (head at preparation time: `c4fca028f223c5104d9fb25c0a36f429c50a7879`)
- New branch: `record/excellandmarks-0.1.0-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-landmarks`
  (a byte-identical copy of this package under
  `review-candidates/excellandmarks-0.1.0-20261105/`)
- Commit message: `Record ExcelLandmarks 0.1.0 (spawn landmarks: sign-tap rent/buy with GP subdivisions) — installed Nov 5, 2026`
- ONE commit with all 54 files. All files are UTF-8 text; no binaries,
  no deletions.

## Files (54)

- `review-candidates/excellandmarks-0.1.0-20261105/BUILD-RECORD.md`
- `review-candidates/excellandmarks-0.1.0-20261105/CHANGELOG-0.1.0.md`
- `review-candidates/excellandmarks-0.1.0-20261105/DEPENDENCIES.md`
- `review-candidates/excellandmarks-0.1.0-20261105/DEPENDENCIES.sha256`
- `review-candidates/excellandmarks-0.1.0-20261105/FILE-LIST.txt`
- `review-candidates/excellandmarks-0.1.0-20261105/INSTALL-PACKET.md`
- `review-candidates/excellandmarks-0.1.0-20261105/INSTALLED.txt`
- `review-candidates/excellandmarks-0.1.0-20261105/MANIFEST-ENTRY.md`
- `review-candidates/excellandmarks-0.1.0-20261105/PUSH-PLAN.md`
- `review-candidates/excellandmarks-0.1.0-20261105/README.md`
- `review-candidates/excellandmarks-0.1.0-20261105/SANITIZATION.diff`
- `review-candidates/excellandmarks-0.1.0-20261105/SHA256SUMS`
- `review-candidates/excellandmarks-0.1.0-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/excellandmarks-0.1.0-20261105/SOURCE.md`
- `review-candidates/excellandmarks-0.1.0-20261105/SPEC.md`
- `review-candidates/excellandmarks-0.1.0-20261105/TEST-REPORT.md`
- `review-candidates/excellandmarks-0.1.0-20261105/TEST-RESULTS.txt`
- `review-candidates/excellandmarks-0.1.0-20261105/build.sh`
- `review-candidates/excellandmarks-0.1.0-20261105/config.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/menu-entry/landmarks.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/plugin.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/drive.py`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/driver/plugin.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/driver/src/rig/GpRig.java`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/driver/src/rig/LmRig.java`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/evidence/boot-a.log`
- `review-candidates/excellandmarks-0.1.0-20261105/rig/evidence/boot-b.log`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/ExcelLandmarks.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/LandmarksCommand.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/SignListener.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/Attribution.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/Money.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/Registry.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/RentClock.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/Settings.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/Spot.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/SpotMenuModel.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/core/TapRouter.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/hook/GpAdapter.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/hook/GpHook.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/hook/ProtectAttribution.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/menu/FloodgateForms.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/menu/FormPort.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/menu/Menus.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/service/LandmarkService.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/com/winkreyes/excellandmarks/store/Store.java`
- `review-candidates/excellandmarks-0.1.0-20261105/src/config.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/src/plugin.yml`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/AttributionTest.java`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/Check.java`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/CoreTest.java`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/RentClockTest.java`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/SpotMenuModelTest.java`
- `review-candidates/excellandmarks-0.1.0-20261105/tests/com/winkreyes/excellandmarks/TapRouterTest.java`

## Pull request

- Title: `Draft source record: ExcelLandmarks 0.1.0 spawn landmarks`
- Base: `main` · Head: `record/excellandmarks-0.1.0-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelLandmarks 0.1.0 (installed live Nov 5, 2026; first release). The plugin (Mike's GO Nov 5, 2026 — "Yes to landmark"; design: village-identity/REMAP-DIRECTION-20261103.md): an admin writes [Landmark] on a sign at spawn and the spot registers (name + TYPE:TIER from the sign; prices all config, never hardcoded — rent $500/$1,500/$3,000 per month, buy $10,000/$25,000/$50,000). Players tap the sign and the spot menu pops (one core model: native Floodgate form on Bedrock, chest inventory on Java) showing only what the viewer can do — available → Rent/Buy, own rental → Renew/Buy Outright/Release, someone else's → view-only — with every charge behind a confirm step. PLOTs auto-create a GriefPrevention subdivision at purchase under the GP TRUST model (Build + manager for the buyer; GP forbids subdivision title transfer — NoTransferException, rig-proven). Rentals run 30 days + 7 days grace; lapse finalize packs container contents + loose items, tallies the renter's materials from ExcelProtect attribution, clears exactly those blocks, deletes the subdivision, and delivers a redemption box beside the sign that never expires. Record package: 54 files under review-candidates/excellandmarks-0.1.0-20261105/, SHA256SUMS-verified, text-only per record anatomy (dependency jars pinned by hash in DEPENDENCIES.sha256; the product jar and the two rig binaries are not included — the rig driver source is). Jar of record: ExcelLandmarks-0.1.0.jar, 77,996 B, SHA-256 466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc (deposited in Drive Minecraft WRV > Build files). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 54.
- Spot-check `review-candidates/excellandmarks-0.1.0-20261105/SHA256SUMS`
  and `SOURCE-PROVENANCE.json` on the branch match the local package
  (the package itself verifies clean: `sha256sum -c SHA256SUMS`).
- Main untouched.

## Already done in this pass (not part of the push)

- Drive 'Minecraft WRV' > Build files: `ExcelLandmarks-0.1.0.jar`
  (77,996 B, SHA-256 `466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc`,
  Drive ID `1NEN-Dhxv1lEq1JaJB71Y9PqMRWhACqtw`, MD5
  `36fba14ecac2865893ce120ff507f306` = local, read-back verified).
- Drive 'Minecraft WRV' > Source: `ExcelLandmarks-0.1.0-source.zip`
  (this package zipped; Drive ID `1GsCCEIiAeDSwkXfWqKU1xULnn4AiR2Pk`,
  read-back verified by size + Drive MD5 = local MD5 of the final zip).
