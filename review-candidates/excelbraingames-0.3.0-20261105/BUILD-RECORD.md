# ExcelBrainGames 0.3.0 — BUILD RECORD

- **Built:** Nov 5, 2026 (America/Chicago), by Iris (build lane), from
  the spec at `~/workspace/brain-games/SPEC.md`. Commissioned by Mike
  Nov 5, 2026, ~4:56 AM CST: "Go full blast with brain game. Then
  we'll fix and edit as we test later." Phase 2 and Phase 3 were built
  back-to-back in one lane under the Nov 4 spec + build-order approval;
  0.3.0 is the cumulative release and supersedes 0.2.0, which was
  never installed standalone.
- **Trees:** Phase 2 (Sudoku) built in
  `~/workspace/brain-games/build-phase2/` on the live Phase 1
  framework; Phase 3 (the text trio) built in
  `~/workspace/brain-games/build-phase3/` on the Phase 2 tree. This
  package carries BOTH trees: the cumulative Phase 3 tree at the root
  and the Phase 2 tree under `phase2/`, each with its own
  TEST-REPORT.md and INSTALL-PACKET.md.
- **Phase 2 candidate (superseded, not installed):**
  `ExcelBrainGames-0.2.0.jar`, sha256
  `36e9519345088c3dd34f75e68486a2b466cf90375debd671c37cf995a3d1b6b6`,
  84,155 B. Suite: **538,880 checks, 0 failures** (Phase 1 regression
  suites + SudokuTest 553 + SudokuPropertyTest 15,006 — 500 seeds ×
  all 5 Sudoku tiers, an independent solver proving exactly one
  solution per deal). Rig on Paper 26.3 + Vault + EssentialsX: ALL
  PASS, SEVERE 0 on both boots (adult-half Kid 4×4 paid $22.50 with
  streak; 9×9 Medium with one Check + par bonus paid $194.40; seeded
  6×6 resumed after restart and paid $60.00).
- **Phase 3 / 0.3.0 candidate (installed):**
  `ExcelBrainGames-0.3.0.jar`, sha256
  `acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63`,
  118,307 B. build.sh assembles the jar twice and hash-asserts; the
  repro jar alongside matches byte-for-byte. Suite: **622,227
  checks, 0 failures** (all Phase 1 + Phase 2 regression suites +
  TextTest 103 + TextPropertyTest 83,244 — 500 seeds × every text
  tier; 15,000 Quick Math questions re-parsed and re-evaluated by an
  independent evaluator). Rig: ALL PASS, SEVERE 0 on both boots
  (Quick Math Kid taps $22.50 adult-half + streak; Quick Math
  Standard via genuine chat events $50.00; Scramble Standard with a
  Hint $90.00; Riddle hint + fuzzy answer $75.00; seeded kid Scramble
  resumed after restart, paid the $25.00 adult half). Detail:
  TEST-REPORT.md (Phase 3, root) and phase2/TEST-REPORT.md, plus
  TEST-RESULTS.txt.
- **Rig note of record (Phase 3):** the rig pass ran the
  immediately-prior build (`5d3abe45…`, same byte size); a
  class-by-class diff shows the only difference from the shipped jar
  is LineNumberTable debug entries in the two QuestionLoop record
  classes (a javadoc edit shifted two source lines) — no instruction,
  constant, or resource differs, so the rig verdicts carry to the
  installed jar unchanged. Recorded in TEST-REPORT.md.
- **Deviations of record:** Phase 2 — the 9×9 Sudoku palette is
  banked (5 digits + bank switch per view; Close on 9×9 is the native
  GUI close, the round auto-saves) because the full tappable set
  cannot fit 54 slots; givens ship as bands bracketing the spec
  targets. Phase 3 — typed answers use chat on BOTH platforms, not a
  Bedrock form (the plugin's only dependency is Vault; the inventory
  question card is the shared surface; answers are captured, never
  broadcast; kid tiers stay tap-only everywhere); the Riddle hint is
  a single −$25 purchase per day (one hint text per riddle; the
  pricing code still implements −$25 per hint). All documented in
  SPEC.md, both TEST-REPORTs, and both INSTALL-PACKETs.
- **Install:** 0.3.0 filed PENDING in
  `~/workspace/wrv-install-queue/MANIFEST.md` Nov 5, 2026 and
  INSTALLED LIVE Nov 5, 2026, 5:34–5:53 AM CST by
  wrv-install-failsafe, directly over live 0.1.0 — see INSTALLED.txt
  (root). The 0.2.0 MANIFEST entry is marked
  SUPERSEDED-BY-CUMULATIVE; both entries are preserved in
  MANIFEST-ENTRY.md. Player proofs still owed: Mike (Bedrock) Quick
  Math Standard in chat + one Riddle attempt, Sudoku 9×9 paging/bank
  feel from Phase 2; Jester (Java) Scramble Standard incl. Hint +
  Quick Math Challenge, Sudoku 9×9 Medium/Hard; Rylie Quick Math Kid
  + Scramble Kid by taps and a Kid 4×4 picture Sudoku round.
