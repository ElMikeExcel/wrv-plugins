# PUSH PLAN — ExcelBrainGames 0.3.0 record PR (connector route)

Prepared Nov 5, 2026 (records close-out). Mike is AWAKE; per his
standing GitHub routing rule (Oct 25, 2026) writes go the CONNECTOR
route, batched: one `push_files` call for the whole package. If an
approval card goes unanswered ~1 minute, verify the change has not
landed and drop to the browser route (GitHub web UI) automatically —
do not sit stalled on a card. Before creating the branch, re-check
that `record/excelbraingames-0.3.0-20261105` does not already exist.

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelbraingames-0.3.0-20261105`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/gh-stage-braingames030`
  (a byte-identical copy of this package under
  `review-candidates/excelbraingames-0.3.0-20261105/`)
- Commit message: `Record ExcelBrainGames 0.3.0 (cumulative: Phase 2 Sudoku + Phase 3 text trio) — installed Nov 5, 2026`
- ONE commit with all 120 files (the established pattern: one batched
  push per package). All files are UTF-8 text; no binaries, no
  deletions.

## Files (120)

- `review-candidates/excelbraingames-0.3.0-20261105/BUILD-RECORD.md`
- `review-candidates/excelbraingames-0.3.0-20261105/CHANGELOG-0.3.0.md`
- `review-candidates/excelbraingames-0.3.0-20261105/DEPENDENCIES.md`
- `review-candidates/excelbraingames-0.3.0-20261105/DEPENDENCIES.sha256`
- `review-candidates/excelbraingames-0.3.0-20261105/FILE-LIST.txt`
- `review-candidates/excelbraingames-0.3.0-20261105/FUNCTIONAL-CHANGE.diff`
- `review-candidates/excelbraingames-0.3.0-20261105/INSTALL-PACKET.md`
- `review-candidates/excelbraingames-0.3.0-20261105/INSTALLED.txt`
- `review-candidates/excelbraingames-0.3.0-20261105/MANIFEST-ENTRY.md`
- `review-candidates/excelbraingames-0.3.0-20261105/OPEN-QUESTIONS.md`
- `review-candidates/excelbraingames-0.3.0-20261105/PUSH-PLAN.md`
- `review-candidates/excelbraingames-0.3.0-20261105/README.md`
- `review-candidates/excelbraingames-0.3.0-20261105/SANITIZATION.diff`
- `review-candidates/excelbraingames-0.3.0-20261105/SHA256SUMS`
- `review-candidates/excelbraingames-0.3.0-20261105/SOURCE-PROVENANCE.json`
- `review-candidates/excelbraingames-0.3.0-20261105/SOURCE.md`
- `review-candidates/excelbraingames-0.3.0-20261105/SPEC.md`
- `review-candidates/excelbraingames-0.3.0-20261105/TEST-REPORT.md`
- `review-candidates/excelbraingames-0.3.0-20261105/TEST-RESULTS.txt`
- `review-candidates/excelbraingames-0.3.0-20261105/build.sh`
- `review-candidates/excelbraingames-0.3.0-20261105/config.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/menu-entry/braingames.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/FILE-LIST.txt`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/INSTALL-PACKET.md`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/SHA256SUMS`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/TEST-REPORT.md`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/build.sh`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/config.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/menu-entry/braingames.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/plugin.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/rig/drive.py`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/rig/driver/plugin.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/rig/driver/src/rig/BrainRig.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/rig/evidence/boot-a.log`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/rig/evidence/boot-b.log`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/BrainGamesCommand.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/DataStore.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/ExcelBrainGames.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/GameConfig.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/GameService.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/JoinListener.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/PlayerData.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/WordLists.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/core/Dir.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/core/Ledger.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/core/Money.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/core/Payouts.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/games/QuestionLoop.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/games/memory/MemoryRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/games/sudoku/SudokuRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/games/wordsearch/WordSearchRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/gui/BrainHolder.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/gui/GuiListener.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/src/com/winkreyes/excelbraingames/gui/Views.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/Check.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/LedgerTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/MemoryTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/PropertyTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/SudokuPropertyTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/SudokuTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/tests/com/winkreyes/excelbraingames/WordSearchTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/animals.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/food.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/kid-animals.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/kid-food.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/kid-minecraft.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/minecraft.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/ocean.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/school.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/phase2/wordlists/space.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/plugin.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/rig/drive.py`
- `review-candidates/excelbraingames-0.3.0-20261105/rig/driver/plugin.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/rig/driver/src/rig/BrainRig.java`
- `review-candidates/excelbraingames-0.3.0-20261105/rig/evidence/boot-a.log`
- `review-candidates/excelbraingames-0.3.0-20261105/rig/evidence/boot-b.log`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/BrainGamesCommand.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/DataStore.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/ExcelBrainGames.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/GameConfig.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/GameService.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/JoinListener.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/PlayerData.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/TextInputListener.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/WordLists.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/core/Dir.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/core/Ledger.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/core/Money.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/core/Payouts.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/QuestionLoop.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/memory/MemoryRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/sudoku/SudokuRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/text/QuickMathRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/text/RiddleService.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/text/RiddleText.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/text/ScrambleRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/text/TextLoop.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/games/wordsearch/WordSearchRound.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/gui/BrainHolder.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/gui/GuiListener.java`
- `review-candidates/excelbraingames-0.3.0-20261105/src/com/winkreyes/excelbraingames/gui/Views.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/Check.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/LedgerTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/MemoryTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/PropertyTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/SudokuPropertyTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/SudokuTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/TextPropertyTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/TextTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/tests/com/winkreyes/excelbraingames/WordSearchTest.java`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/animals.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/food.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/kid-animals.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/kid-food.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/kid-minecraft.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/minecraft.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/ocean.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/riddles.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/school.yml`
- `review-candidates/excelbraingames-0.3.0-20261105/wordlists/space.yml`

## Pull request

- Title: `Draft source record: ExcelBrainGames 0.3.0 brain games complete set (Sudoku + text trio, cumulative)`
- Base: `main` · Head: `record/excelbraingames-0.3.0-20261105`
- **draft = true** — record PRs stay OPEN DRAFTS, never merged.
- Body:

Source record for ExcelBrainGames 0.3.0 (installed live Nov 5, 2026, directly over 0.1.0). The cumulative release completing WRV-196 Brain Games: Phase 2 adds Sudoku (fresh unique-solution puzzles, palette-first placement, picture mode for kids, 9x9 two-view paging, Check button, par bonus) and Phase 3 adds Quick Math, Word Scramble and Riddle of the Day on one shared question-loop module — all six games now live in the /games picker, riding the Phase 1 framework (save/resume sessions, shared $1,500/day cap with streak bonuses, Vault payouts, full console admin; adults earn half on kid tiers). The intermediate 0.2.0 build was superseded by this install and never installed standalone; its full build tree is carried in the package under phase2/. Record package: 120 files under review-candidates/excelbraingames-0.3.0-20261105/, SHA256SUMS-verified, text-only per record anatomy (dependency jars pinned by hash in DEPENDENCIES.sha256). Jar of record: ExcelBrainGames-0.3.0.jar, 118,307 B, SHA-256 acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63 (deposited in Drive Minecraft WRV > Build files). Draft record PR per standing rule: never merged.

## Verify after creation

- PR state: open, draft, unmerged; file count exactly 120.
- Spot-check `review-candidates/excelbraingames-0.3.0-20261105/SHA256SUMS`
  and `SOURCE-PROVENANCE.json` on the branch match the local package
  (the package itself verifies clean: `sha256sum -c SHA256SUMS`).
- Main untouched.

## Drive deposits (executed in the same pass)

- Drive 'Minecraft WRV' > Build files: `ExcelBrainGames-0.3.0.jar`
  (118,307 B, SHA-256 `acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63`).
- Drive 'Minecraft WRV' > Source: `ExcelBrainGames-0.3.0-source.zip`
  (this package zipped). Both read-back verified (size + Drive MD5 =
  local MD5); IDs and MD5s are recorded in the daily log.
