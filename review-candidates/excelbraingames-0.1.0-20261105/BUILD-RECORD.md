# ExcelBrainGames 0.1.0 — BUILD RECORD

- **Built:** Nov 4, 2026 (America/Chicago), by Iris (build lane), from the
  spec at `~/workspace/brain-games/SPEC.md`. Commissioned by Mike Nov 4,
  2026: hold on WRV-196 lifted, Word Search added at his direction, all
  seven open-question recommendations approved, Phase 1 GO; Q6 amended by
  Mike the same day (adults earn half on kid tiers).
- **Tree:** `~/workspace/brain-games/build-phase1/` — fresh tree (first
  release; no base version). Contents: src/ (21 classes incl.
  `games/QuestionLoop` Phase 3 stub), tests/ (5 harnesses), build.sh,
  plugin.yml, config.yml, wordlists/ (9 shipped lists), menu-entry/
  (the ExcelMenu registry entry), rig/ (scratch Paper 26.3 rig).
- **Candidate:** `ExcelBrainGames-0.1.0.jar`, sha256
  `b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef`,
  69,650 B. build.sh assembles the jar twice and hash-asserts; a second
  full build.sh run reproduced the identical hash (repro jar alongside).
- **Tests on the final content: 523,321 checks, 0 failures** —
  WordSearchTest 20,416 + MemoryTest 3,039 + LedgerTest 55 + PropertyTest
  499,811 (500 seeds × all 15 tier/theme combos). The Q6 amendment is
  proven end-to-end (rig: adult on kid Word Search paid $30.00 = $20 half
  + $10 streak), pays-once holds, and a half-finished Challenge round
  resumed intact after a full restart. Detail: TEST-REPORT.md (build
  lane's report incl. the ship-prep addendum) and TEST-RESULTS.txt.
- **Ship-prep (Nov 4, 2026, evening, Mike-directed):** kid Minecraft list
  14 → 28 words; adult Minecraft list 24 → 72 words; all three kid lists
  promoted from `wordlists/draft/` to `wordlists/` after Mike approved
  them as-is; shipped config carries `games.kid-tiers-enabled: true`
  (kid tiers LIVE at launch; Rylie kid-flagged). The expansion exposed a
  latent generator bug — `dealSubset`/`dealSubsetBudget` sorted with a
  comparator that re-rolled `rng.nextInt(3)` per comparison, a TimSort
  contract violation silent under ~32 candidates — fixed by drawing
  jitter keys once per word before sorting; no other generator logic
  changed. The re-test totals above are on the final content.
- **Smoke boot of the final jar:** Paper 26.3-235 (the exact live build),
  fresh data folder — enable line verbatim with kid-tiers=ENABLED,
  wordlists=9 files / 6 themes; extraction produced exactly 9 lists with
  no `draft/` folder; console `games` usage + `games admin status`
  answered in words; SEVERE 0 (`rig/evidence/smoke-20261104.log`).
- **Deviations of record:** Challenge Word Search deals 10 words (spec
  said 12 — saturation-proven impossible on the 9×6 board minus the 3
  GUI control cells; payout unchanged at $150); board controls occupy GUI
  slots 51–53, void in the generator. Both documented in SPEC.md and
  TEST-REPORT.md.
- **Install:** filed PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
  Nov 4, 2026 (INSTALL-PACKET.md FINAL); INSTALLED LIVE Nov 5, 2026,
  12:05–12:23 AM CST by wrv-install-failsafe — see INSTALLED.txt.
  Player proofs still owed: Mike (Bedrock) one Word Search + one Memory
  Match round from the compass button; Jester (Java) one round each;
  Rylie's first kid-tier rounds.
