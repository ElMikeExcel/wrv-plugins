# ExcelBrainGames 0.1.0 — SOURCE / provenance

Build folder: `~/workspace/brain-games/build-phase1/` (project paperwork in
`~/workspace/brain-games/`: SPEC.md — the build contract — and
OPEN-QUESTIONS.md, all seven questions DECIDED by Mike Nov 4, 2026; both are
included in this package).

## What 0.1.0 is

The first Brain Games release (WRV-196, Phase 1), commissioned Nov 4, 2026:
Mike lifted the Oct 24 hold, added **Word Search** as his own headline game,
approved the spec recommendations, hand-checked the kid word lists, directed
the Minecraft list expansion, and approved shipping the same evening.

- **Framework:** `/games` (aliases `/brain`, `/puzzles`) opens a tap-only
  inventory-GUI picker on both platforms (Bedrock rides Geyser's form
  translation — the WandBars-proven surface, because native Bedrock forms
  cannot do a 2D button grid). The compass **Brain Games button** ships in
  the same window via the ExcelMenu 2.0 registry (`menu-entry/braingames.yml`,
  registry 84 → 85) — the command is the twin of the button, never the only
  door, per Mike's menu standard.
- **Sessions:** one active round per game per player, auto-saved on every
  move; survives GUI close, logout and restart; a late completion pays
  against the completion day's cap. Discard is an armed double-tap.
- **Money:** Vault payouts through one shared ledger (`core/Ledger`):
  **$1,500/day cap per player across all games** (America/Chicago day
  boundary), each generated puzzle pays once, streak bonus $10 × streak
  days (max $50) on the first completion of a day, counts toward the cap.
- **Word Search:** a generator deals a fresh puzzle every round (the
  pays-once rule is per board, not per player). Two-tap circling (tap the
  first letter, tap the last); found words stay highlighted green and
  strike off the list. Tiers: Kid 6×6 (5 words, 3–5 letters, →/↓ only,
  $40), Standard 9×6 (8 words, $90), Challenge 9×6 (10 words, diagonals +
  reverse, $150). Six adult themes + three kid themes, all in config word
  lists (`wordlists/`).
- **Memory Match:** flip-two card grid; tiers $30 / $55 / $100 / $160
  (6 / 8 / 12 / 18 pairs).
- **Kid tiers + Q6 amendment (Mike, Nov 4, 2026):** kid tiers pay
  kid-flagged players FULL; adults earn exactly **half** the kid payout
  (odd cents round DOWN); the streak bonus is not halved; cap and
  pays-once unchanged. Kid tiers are ENABLED at launch — Mike approved
  the kid lists as-is Nov 4, 2026 — and Rylie (`.RylieDSunflower`) is
  kid-flagged in the shipped config.
- **Console admin (fleet rule):** `/games admin status|reset|resetday|
  setkid|reload` (plus `riddle`/`setriddle` answering with the Phase 3
  notice) — routed for any sender; player use needs `braingames.admin`
  (default op, declared in plugin.yml).
- **Phase 2/3 seams:** Sudoku and the text games show as "coming soon" in
  the picker (no dead buttons); `games/QuestionLoop.java` ships as the
  question-loop stub the Phase 3 games will share.

Two documented build deviations (SPEC.md + TEST-REPORT.md carry them):
Challenge Word Search deals **10 words, not 12** — saturation-proven: 12
words of 5–10 letters cannot fit a 9×6 board minus the 3 GUI control cells
on ~98% of deals; 10 words places at full count on 500/500 boards per
theme, payout unchanged at $150. And the board controls live in GUI slots
51–53, so those three logical cells are void in the generator.

## Candidate

    ExcelBrainGames-0.1.0.jar
    sha256  b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef
    size    69,650 bytes — build.sh assembles twice and hash-asserts; a
            second full build.sh run reproduced the same hash
            (ExcelBrainGames-0.1.0-repro.jar alongside in the build folder).

## Build inputs

JDK 25 at `~/workspace/.jdk25`, `javac --release 21`. Compile classpath:
the hash-pinned Paper API 26.3 build 231 jar (`f0df9e7f…`) plus its
runtime library set, and Vault 2.0.0 (`8b2ce431…`, compile-only, never
shipped) — full pins in DEPENDENCIES.md / DEPENDENCIES.sha256. build.sh
hash-asserts both primary inputs before compiling and descriptor-gates
plugin.yml / config.yml / every word list through a YAML parse before any
class is built. All ExcelBrainGames code is the project's own; there is no
upstream plugin source in this build (the Oct 24 third-party alternative,
Pocket Games, was rejected — see SPEC.md provenance).

## Companion deliverables in this folder

- `SPEC.md` — the design contract (six games, rendering limits, payouts,
  save/resume, console admin, phase order).
- `OPEN-QUESTIONS.md` — the seven pre-build questions, all DECIDED Nov 4,
  2026 (Q6 as amended by Mike: adults half price on kid tiers).
- `TEST-REPORT.md` — the build lane's full test report incl. the Nov 4
  ship-prep addendum (list expansion, the generator comparator bug it
  exposed and its fix, the final-content re-test, the Paper 26.3-235
  smoke boot). `TEST-RESULTS.txt` is the close-out summary.
- `BUILD-RECORD.md`, `CHANGELOG-0.1.0.md`, `INSTALL-PACKET.md` (FINAL, as
  installed), `MANIFEST-ENTRY.md` (the filed queue entry), `INSTALLED.txt`
  (the install record).
- `rig/` — the scratch Paper 26.3 rig (BrainRig driver source + the two
  gameplay boot logs and the final-jar smoke log in `rig/evidence/`).
  Rig-only; never shipped. Compiled driver classes are not recorded.
