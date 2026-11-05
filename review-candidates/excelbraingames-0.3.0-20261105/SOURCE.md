# ExcelBrainGames 0.3.0 — SOURCE / provenance

Build folders: `~/workspace/brain-games/build-phase3/` (the cumulative
0.3.0 tree — this package's root) and
`~/workspace/brain-games/build-phase2/` (the Phase 2 tree — this
package's `phase2/`). Project paperwork in `~/workspace/brain-games/`:
SPEC.md — the build contract — and OPEN-QUESTIONS.md, all seven
questions DECIDED by Mike Nov 4, 2026; both are included in this
package.

## What 0.3.0 is

The cumulative release completing Brain Games (WRV-196). Phase 1
(framework + Word Search + Memory Match) shipped as 0.1.0 on Nov 5,
2026 (record: PR #138). On Nov 5, 2026, ~4:56 AM CST, Mike directed
"Go full blast with brain game. Then we'll fix and edit as we test
later" — Phases 2 and 3 were built back-to-back in one lane and 0.3.0
installed directly over live 0.1.0; the intermediate 0.2.0 build was
marked SUPERSEDED-BY-CUMULATIVE in the install queue and never
installed standalone.

- **Framework (unchanged contract):** `/games` (aliases `/brain`,
  `/puzzles`) opens the tap-first picker on both platforms; the
  compass **Brain Games button** rides the ExcelMenu 2.0 registry
  (`menu-entry/braingames.yml`, registry 85). One active round per
  game per player, auto-saved on every move, survives GUI close,
  logout and restart; a late completion pays against the completion
  day's cap. Money runs through the shared ledger: **$1,500/day cap
  per player across all games**, each generated puzzle pays once,
  streak bonus $10 × streak days (max $50) on the first completion
  of a day. Kid tiers pay kid-flagged players FULL and adults exactly
  half (Mike's Q6 amendment); Rylie (`.RylieDSunflower`) is
  kid-flagged in the shipped config.
- **Sudoku (Phase 2, `games/sudoku/SudokuRound.java`):** a generator
  deals a fresh unique-solution puzzle every round (randomized
  backtracking + solver-checked removal; an independent solver in
  the property suite proves uniqueness on 500 seeds × all tiers).
  Tiers: Kid 4×4 picture mode $25, 6×6 $60, 9×9 Easy $120 / Medium
  $180 / Hard $250. Palette-first placement, eraser, Check button
  (adults 10% of payout per use, max 3, config
  `sudoku.check-cost-percent: 10`; kid tiers free/unlimited), +20%
  par bonus on 9×9 Medium/Hard, 9×9 two-view paging (rows 1–5 / 5–9,
  row 5 shared seam) with a banked digit palette.
- **The text trio (Phase 3, `games/text/` on ONE shared
  question-loop module):** Quick Math (10 questions; Kid $25
  tap-choice / Standard $50 / Challenge $90 typed; payout = tier ×
  correct/10 rounded DOWN), Word Scramble (Kid $50 tap-choice with
  retries / Standard $100 typed one-try, Hint halves a word,
  finish-now banks earned partial credit), Riddle of the Day ($100
  flat, −$25 hint with a $25 floor, once per day per player, same
  riddle server-wide drawn from `wordlists/riddles.yml` — 16
  shipped; fuzzy answers with accepted-answer lists). Typed answers
  ride chat capture (`TextInputListener.java`; captured, never
  broadcast) with an inventory question card on both platforms —
  see the deviation note below.
- **Console admin (fleet rule):** `/games admin status|reset|
  resetday|setkid|reload|riddle|setriddle` — `reset` accepts every
  game including `sudoku`, `quickmath`, `scramble`, `riddle`;
  `riddle`/`setriddle` are fully implemented in 0.3.0 (the Phase 1
  stub message is gone).

Documented deviations (SPEC.md + both TEST-REPORTs carry them):
Phase 2 — the 9×9 palette is banked (45 visible cells + 9 digits +
eraser + check + page toggle = 57 > 54 slots; Close on 9×9 is the
native GUI close and the round auto-saves); givens ship as bands
(38–44 / 30–35 / 24–29) bracketing the spec's ~40 / ~32 / ~26
targets. Phase 3 — typed answers use chat on both platforms instead
of a Bedrock form (a form needs the Floodgate API; this plugin's
only dependency is Vault, Phase 1's presentation decision — the
inventory GUI + question card is the one shared surface; kid tiers
are tap-only everywhere as specced); the Riddle hint is a single
−$25 purchase per day because each riddle stores one hint text
(the pricing code still implements −$25 per hint for future
multi-hint riddles).

## Candidates

    ExcelBrainGames-0.3.0.jar  (installed)
    sha256  acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63
    size    118,307 bytes — build.sh assembles twice and
            hash-asserts; the repro jar alongside matches
            (ExcelBrainGames-0.3.0-repro.jar in the build folder).

    ExcelBrainGames-0.2.0.jar  (Phase 2 intermediate — superseded,
    never installed, not deposited to Drive)
    sha256  36e9519345088c3dd34f75e68486a2b466cf90375debd671c37cf995a3d1b6b6
    size    84,155 bytes — same double-assembly discipline.

## Build inputs

JDK 25 at `~/workspace/.jdk25`, `javac --release 21`. Compile
classpath: the hash-pinned Paper API 26.3 build 231 jar
(`f0df9e7f…`) plus its runtime library set, and Vault 2.0.0
(`8b2ce431…`, compile-only, never shipped) — full pins in
DEPENDENCIES.md / DEPENDENCIES.sha256, unchanged from 0.1.0. Both
build.sh files hash-assert both primary inputs before compiling and
descriptor-gate plugin.yml / config.yml / every word list through a
YAML parse before any class is built. All ExcelBrainGames code is
the project's own; there is no upstream plugin source in this build.

## Companion deliverables in this folder

- `SPEC.md` — the design contract (six games, rendering limits,
  payouts, save/resume, console admin, phase order).
- `OPEN-QUESTIONS.md` — the seven pre-build questions, all DECIDED
  Nov 4, 2026 (Q6 as amended by Mike: adults half price on kid
  tiers).
- `TEST-REPORT.md` — the Phase 3 build lane's full report;
  `phase2/TEST-REPORT.md` — the Phase 2 report. `TEST-RESULTS.txt`
  is the close-out summary over both phases.
- `BUILD-RECORD.md`, `CHANGELOG-0.3.0.md`, `INSTALL-PACKET.md` (the
  0.3.0 packet, as installed; `phase2/INSTALL-PACKET.md` is the
  superseded 0.2.0 packet), `MANIFEST-ENTRY.md` (both filed queue
  entries), `INSTALLED.txt` (the 0.3.0 install record).
- `rig/` — the scratch Paper 26.3 rig (BrainRig driver source + the
  gameplay boot logs in `rig/evidence/`; `phase2/rig/` likewise).
  Rig-only; never shipped. Compiled driver classes are not recorded.
