# ExcelBrainGames 0.3.0 — TEST REPORT (Phase 3: the text trio)

Built Nov 5, 2026 on the live Phase 2 framework (0.2.0). Build folder:
`~/workspace/brain-games/build-phase3/`. Mike's direction (Nov 5, 2026,
~4:56 AM CST): "Go full blast with brain game. Then we'll fix and edit
as we test later." Phase 3 completes the six-game set from SPEC §1.

## Suites (build gate, all in build.sh)

| Suite | Checks | Failures |
|---|---|---|
| WordSearchTest (Phase 1, regression) | 20,416 | 0 |
| MemoryTest (Phase 1, regression) | 3,039 | 0 |
| SudokuTest (Phase 2, regression) | 553 | 0 |
| SudokuPropertyTest (Phase 2, regression) | 15,006 | 0 |
| TextTest (new) | 103 | 0 |
| TextPropertyTest (new) | 83,244 | 0 |
| LedgerTest (regression) | 55 | 0 |
| PropertyTest (Phase 1, regression) | 499,811 | 0 |
| **Total** | **622,227** | **0** |

Deterministic build: two assemblies of `ExcelBrainGames-0.3.0.jar`
byte-identical — 118,307 bytes, SHA-256
`acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63`.
SHA256SUMS + FILE-LIST regenerated and verified (`sha256sum -c` clean).
Note: the rig pass ran the immediately-prior build (`5d3abe45…`, same
byte size). A class-by-class diff of the two jars shows the ONLY
difference is LineNumberTable debug entries in the two QuestionLoop
record classes (a javadoc edit shifted two source lines) — no
instruction, constant, or resource differs, so the rig verdicts carry
to this jar unchanged.

## TextTest (unit/logic)

- Quick Math flow: 10 questions, one attempt each (wrong answers
  advance and show the correct one), kid choices are 3 distinct
  options containing the answer; answers after the round ends refused.
- §6.3 payout math: tier × correct/10 rounded DOWN to whole dollars —
  9/10 on $50 = $45; 7/10 on $25 = $17 (not $17.50); 1/10 on $25 = $2.
- Scramble: shuffles are letter permutations that never equal the
  word; Standard one-attempt advance; Hint halves the word ($20 →
  $10); finish-now banks exactly the earned partial credit; kid tier
  retries without advancing and pays full value; an undersized pool
  refuses cleanly (null) instead of dealing a broken round.
- Riddle matching: exact/case/article/punctuation/plural variants
  match; prefixes and different words never do; listed alternate
  answers match. Payout math: $100 − $25/hint with the $25 floor
  holding past 3 hints.
- Kid-tier rule on text values through the shared Phase 1 path:
  adult 10/10 on QM Kid = $12.50, kid-flagged = $25; adult full
  Scramble Kid round = $25 of $50; Standard/Riddle are not kid tiers.
- Serialization: mid-round QM (index/correct/questions) and Scramble
  (index/banked earnings/armed hint/words/shuffles) survive the
  DataStore map round-trip exactly.

## TextPropertyTest (500 seeds × every text tier, shipped content)

- Quick Math, all 3 tiers × 500 seeds × 10 questions: every prompt
  string is RE-PARSED and evaluated by an independent evaluator in the
  test (tokenizer + precedence parser sharing no code with the
  generator) and must equal the stored answer — 15,000 questions
  verified end-to-end. Tier invariants from the parsed tokens: Kid
  only + and −, operands/results within 20, never negative; Standard
  one operation, results within 100, division always exact; Challenge
  always two operations, results 1–999. Perfect play scores 10/10.
- Scramble, both tiers × 500 seeds against the SHIPPED wordlists:
  5 distinct words per round, all in the tier length band (kid 3–4,
  standard 5–8), every shuffle a true permutation ≠ the word, kid
  choices always 3 and always contain the word; perfect play banks
  the full round amount ($50 / $100).

## Rig pass (live Paper 26.3, Vault + EssentialsX 2.22.0)

Rig folder: `build-phase3/rig/` (same scratch server as Phase 2,
reused in place — /tmp is a 512 MB tmpfs; transcripts
`rig/evidence/boot-a.log` + `boot-b.log`). A fake NMS player plays
through the real GUI with genuine InventoryClickEvents, and typed
answers are genuine AsyncChatEvents fired from an async thread, so
the chat-capture path is exercised exactly as a player's is.

Verdicts (Nov 5, 2026): **ALL PASS, SEVERE 0 on both boots.**

Boot A — Quick Math Kid solved by choice taps as a non-kid pays
**$22.50** (adult half $12.50 + $10 day-1 streak); Quick Math
Standard answered through genuine chat events (answer mode armed on
start, messages captured not broadcast) pays **$50.00** for 10/10;
Scramble Standard with a Hint on word 1 pays **$90.00** ($10 hinted +
4 × $20); Riddle of the Day: hint bought from the card (−$25), one
wrong chat guess spends exactly one attempt, the correct fuzzy
answer pays **$75.00**; a kid Scramble is seeded with 2 words banked
($20). Console admin exercised: `admin riddle` shows the riddle +
answer, `setriddle 3` forces and re-shows, `reset … riddle` clears,
status + reload report the text games.

Boot B — the seeded kid Scramble resumes from disk intact (index 2,
$20 banked), the resume prompt opens the question card, and
finishing it pays **$25.00** — the adult half of the $50 kid round —
against the new boot, per SPEC §9.5.

Rig note: the first Boot A run scored 9/10 on typed Quick Math — a
driver race (the next chat answer fired before the previous one was
processed, so one answer landed on the next question). The driver
now waits for the round index to advance between answers; the clean
re-run is the record above. The plugin was never at fault.

## Documented deviations (Phase 1 style; also in SPEC §8.3)

1. **Typed answers use chat on both platforms, not a Bedrock form.**
   SPEC §6.2/§8.2 describe "form on Bedrock, chat on Java". A Bedrock
   form needs the Floodgate form API; this plugin's only dependency
   is Vault (Phase 1's presentation decision — the inventory GUI is
   the one shared surface). As built: an inventory question card
   shows the prompt + progress on both platforms, typed rounds take
   answers from chat (captured, never broadcast; "stop" leaves
   answer mode; non-numbers spend no attempt), and kid tiers are
   tap-only everywhere as specced. Mike's Bedrock phone test of the
   chat-answer flow is an owed player proof in the install packet.
2. **Riddle hint is a single purchase.** Each riddle stores ONE hint
   text, so the Hint control charges −$25 once per day (floor $25
   applies) and re-reading the bought hint is free. The pricing code
   still implements −$25 per hint for future multi-hint riddles.

## Console admin (Phase 3 delta)

`admin riddle` (today's riddle + accepted answers + hint, console /
support) and `admin setriddle <index>` ship per SPEC §10 — the Phase
1/2 stub message is gone. `admin reset` accepts `quickmath`,
`scramble`, and `riddle` (the last clears today's attempts/hints/
solved for one player); `admin status` shows all three text games;
`admin reload` reports the riddle count. No other commands changed.

## Picker completeness

After 0.3.0 the game picker has NO dead buttons: Word Search, Memory
Match, Sudoku, Quick Math, Word Scramble, Riddle of the Day are all
live doors, all tap-reachable from the compass Brain Games button.
The Phase 1/2 "coming soon" tile is gone.
