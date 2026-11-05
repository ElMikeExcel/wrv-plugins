# PUSH PLAN — ExcelBrainGames 0.1.0 record PR (for the parent to execute via the browser route)

Prepared Nov 5, 2026 (records close-out). Mike is asleep; per his standing
GitHub routing rule (Oct 25, 2026) overnight writes go the BROWSER route
with zero approval dialogs. The connector route was abandoned mid-flight:
a connector `create_branch` call was started and aborted, its approval
card (if one was raised) expires unanswered on its own — do not shepherd
it. Before creating the branch in the browser, re-check that
`record/excelbraingames-0.1.0-20261105` still does not exist (it was
confirmed absent by a 404 ref lookup in this pass, and main's head was
`c4fca028f223c5104d9fb25c0a36f429c50a7879`); if the branch unexpectedly
exists at main's head with no extra commits, use it as-is.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main` (head at preparation time: `c4fca028f223c5104d9fb25c0a36f429c50a7879`)
- New branch: `record/excelbraingames-0.1.0-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-braingames`
  (a byte-identical copy of this package under
  `review-candidates/excelbraingames-0.1.0-20261105/`)
- Commit message: `Record ExcelBrainGames 0.1.0 (Phase 1: framework + Word Search + Memory Match) — installed Nov 5, 2026`
- ONE commit with all 61 files (the Nov 4 pattern: one batched
  push per package). All files are UTF-8 text; no binaries, no deletions.

## Files (61)

- `review-candidates/excelbraingames-0.1.0-20261105/BUILD-RECORD.md`
- `review-candidates/excelbraingames-0.1.0-20261105/CHANGELOG-0.1.0.md`
- `review-candidates/excelbraingames-0.1.0-20261105/DEPENDENCIES.md`
- `review-candidates/excelbraingames-0.1.0-20261105/DEPENDENCIES.sha256`
- `review-candidates/excelbraingames-0.1.0-20261105/FILE-LIST.txt`
- `review-candidates/excelbraingames-0.1.0-20261105/FUNCTIONAL-CHANGE.diff`
- `review-candidates/excelbraingames-0.1.0-20261105/INSTALL-PACKET.md`
- `review-candidates/excelbraingames-0.1.0-20261105/INSTALLED.txt`
- `review-candidates/excelbraingames-0.1.0-20261105/MANIFEST-ENTRY.md`
- `review-candidates/excelbraingames-0.1.0-20261105/OPEN-QUESTIONS.md`
- `review-candidates/excelbraingames-0.1.0-20261105/PUSH-PLAN.md`
- `review-candidates/excelbraingames-0.1.0-20261105/README.md`
- `review-candidates/excelbraingames-0.1.0-20261105/SANITIZATION.diff`
- `review-candidates/excelbraingames-0.1.0-20261105/SHA256SUMS`
- `review-candidates/excelbraingames-0.1.0-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/excelbraingames-0.1.0-20261105/SOURCE.md`
- `review-candidates/excelbraingames-0.1.0-20261105/SPEC.md`
- `review-candidates/excelbraingames-0.1.0-20261105/TEST-REPORT.md`
- `review-candidates/excelbraingames-0.1.0-20261105/TEST-RESULTS.txt`
- `review-candidates/excelbraingames-0.1.0-20261105/build.sh`
- `review-candidates/excelbraingames-0.1.0-20261105/config.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/menu-entry/braingames.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/plugin.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/drive.py`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/driver/plugin.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/driver/src/rig/BrainRig.java`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/evidence/boot-a.log`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/evidence/boot-b.log`
- `review-candidates/excelbraingames-0.1.0-20261105/rig/evidence/smoke-20261104.log`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/BrainGamesCommand.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/DataStore.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/ExcelBrainGames.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/GameConfig.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/GameService.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/JoinListener.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/PlayerData.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/WordLists.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/core/Dir.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/core/Ledger.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/core/Money.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/core/Payouts.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/games/QuestionLoop.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/games/memory/MemoryRound.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/games/wordsearch/WordSearchRound.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/gui/BrainHolder.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/gui/GuiListener.java`
- `review-candidates/excelbraingames-0.1.0-20261105/src/com/winkreyes/excelbraingames/gui/Views.java`
- `review-candidates/excelbraingames-0.1.0-20261105/tests/com/winkreyes/excelbraingames/Check.java`
- `review-candidates/excelbraingames-0.1.0-20261105/tests/com/winkreyes/excelbraingames/LedgerTest.java`
- `review-candidates/excelbraingames-0.1.0-20261105/tests/com/winkreyes/excelbraingames/MemoryTest.java`
- `review-candidates/excelbraingames-0.1.0-20261105/tests/com/winkreyes/excelbraingames/PropertyTest.java`
- `review-candidates/excelbraingames-0.1.0-20261105/tests/com/winkreyes/excelbraingames/WordSearchTest.java`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/animals.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/food.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/kid-animals.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/kid-food.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/kid-minecraft.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/minecraft.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/ocean.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/school.yml`
- `review-candidates/excelbraingames-0.1.0-20261105/wordlists/space.yml`

## Pull request

- Title: `Draft source record: ExcelBrainGames 0.1.0 brain games phase 1 (Word Search + Memory Match)`
- Base: `main` · Head: `record/excelbraingames-0.1.0-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelBrainGames 0.1.0 (installed live Nov 5, 2026). Phase 1 of WRV-196 Brain Games: framework (/games + compass Brain Games door, save/resume sessions, shared $1,500/day cap with streak bonuses, Vault payouts, console admin) plus Word Search (fresh generated grid every round, two-tap circling) and Memory Match; kid tiers enabled at launch, adults earn half on kid tiers (Mike's Q6 amendment). Record package: 61 files under review-candidates/excelbraingames-0.1.0-20261105/, SHA256SUMS-verified, text-only per record anatomy (dependency jars pinned by hash in DEPENDENCIES.sha256). Jar of record: ExcelBrainGames-0.1.0.jar, 69,650 B, SHA-256 b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef (deposited in Drive Minecraft WRV > Build files). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 61.
- Spot-check `review-candidates/excelbraingames-0.1.0-20261105/SHA256SUMS`
  and `SOURCE-PROVENANCE.json` on the branch match the local package
  (the package itself verifies clean: `sha256sum -c SHA256SUMS`).
- Main untouched.

## Already done in this pass (not part of the push)

- Drive 'Minecraft WRV' > Build files: `ExcelBrainGames-0.1.0.jar`
  (69,650 B, SHA-256 `b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef`).
- Drive 'Minecraft WRV' > Source: `ExcelBrainGames-0.1.0-source.zip`
  (this package zipped). Both read-back verified (size + Drive MD5 =
  local MD5).
