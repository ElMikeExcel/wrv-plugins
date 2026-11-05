# ExcelBrainGames 0.1.0 — INSTALL PACKET (FINAL)

**Status: FINAL — filed in the install MANIFEST Nov 4, 2026; ship at the
next verified-empty window.** Mike's ship approval Nov 4, 2026 ("yes ship
it"). Build + test evidence: TEST-REPORT.md in this folder (incl. the
Nov 4 ship-prep addendum).

## What installs

| Piece | Destination | Notes |
|---|---|---|
| `ExcelBrainGames-0.1.0.jar` (69,650 B, SHA-256 `b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef`) | `plugins/` | New plugin; nothing live to replace |
| `menu-entry/braingames.yml` | `plugins/ExcelMenu/entries/braingames.yml` | The Brain Games button in the compass / Server Menu; then `/excelmenu reload`. ExcelMenu registry 84 → 85 |

First enable creates `plugins/ExcelBrainGames/` with the shipped
`config.yml`, extracts the bundled word lists to `wordlists/` (9 files:
6 adult theme lists + the 3 approved kid lists), and starts writing
per-player data under `data/`. Dependencies already live: Vault (+ its
economy provider). No other plugin changes. **Plugin count 77 → 78;
ExcelMenu registry 84 → 85.**

## Menu standard (Mike's rule — almost all commands live in the menu)

The compass / Server Menu **Brain Games button ships in this same
window** (the `menu-entry/braingames.yml` row above — not a follow-up).
From that one button, EVERY player-facing Brain Games action is
reachable by taps, on Java and Bedrock alike: the button runs `/games`,
which opens the picker — Word Search → theme picker → tier picker →
board; Memory Match → tier picker → board; a saved round shows "Round
in progress — click to resume" on its picker button and leads to the
resume view (Resume / Start-a-new-round with the discard double-tap /
Back); in-round controls and close-to-save live on the board views.
`/games` (aliases `/brain`, `/puzzles`) is the command twin of the
button, never the only door. Sudoku and the Phase 3 games appear in the
picker as "coming soon" notices only — no dead buttons.

## Gates

1. **Kid-list hand check — CLEARED Nov 4, 2026.** Mike approved the
   draft kid lists as-is ("that list is fine"). The lists were expanded
   the same day at his direction (kid Minecraft 14 → 28 words; adult
   Minecraft 24 → 72), promoted from `wordlists/draft/` into
   `wordlists/`, and the shipped config carries
   `games.kid-tiers-enabled: true` — **kid tiers are LIVE at launch**,
   Rylie (`.RylieDSunflower`) already kid-flagged in the config.
2. **Phone test — POST-INSTALL proof (Mike's accepted flow).** Mike
   accepted install-then-test Nov 4, 2026: the phone test is owed by
   Mike on Bedrock right after install (one Word Search round + one
   Memory Match round — see Player proofs). It does NOT gate the
   window; the pre-install Shockbyte backup is the rollback of record
   if the feel is wrong.
3. **Normal empty-server window** (verified empty, or a Mike-directed
   window), with the fleet's BACKUP FIRST + cap-at-6 rules applied
   before the swap, exactly like any other install.

Economy values are as specced and Mike-approved Nov 4, 2026 (cap
$1,500/day, streak $10/day to $50, tier payouts per config, **adults
earn half on kid tiers** per his Q6 amendment). Any change to those
numbers at install time is a Mike decision, not an installer's.

## Install steps (when the window comes)

1. Pre-flight: verify the candidate jar on disk hashes to `b23f916f…`
   (69,650 B). Verify the live server has NO `plugins/ExcelBrainGames*.jar`
   and no `plugins/ExcelBrainGames/` folder (first install; if either
   exists, STOP — the state is not what this packet assumes).
2. BACKUP FIRST per the fleet rule (fresh Shockbyte backup, cap-at-6
   prune first if needed, pruned backups named in the install report).
3. Upload the jar to `plugins/`. Upload `braingames.yml` to
   `plugins/ExcelMenu/entries/`.
4. Panel Shut Down → Start (never console `stop`).
5. Verification boot — all must hold:
   - Boot `Done` line present, **SEVERE 0** attributable to the window.
   - Enable line reads: `ExcelBrainGames 0.1.0 enabled: games=[word-search, memory-match], kid-tiers=ENABLED, daily-cap=$1,500.00, wordlists=9 files / 6 themes, economy=<provider>`.
   - Plugin count 78; ExcelMenu registry 85 after the reload below;
     ExcelGuildGate line still shows guild LOCKED with the seal count
     unchanged (3 of 5).
   - Console smoke: `games` prints the console usage; `games admin status <anyone>` answers in words.
   - `/excelmenu reload` runs clean and the Brain Games button appears
     in the compass / Server Menu (server_menu, group "me").
6. Bookkeeping: INSTALLED marker in this folder, MANIFEST entry moved
   to INSTALLED with the boot facts, daily-log line, Route Sheet
   WRV-196 updated.

## Same-pass surfaces owed at install (same window, fleet rule)

- **ExcelMenu entry** — in the install itself (above): registry 84 → 85.
- **Guide knowledge + intents** teaching Brain Games from the website
  canon: `/games` and the compass button, how to play Word Search
  (two-tap circling, themes, Kid 6×6 / Standard / Challenge tiers) and
  Memory Match (pairs tiers), payouts per tier, the $1,500/day shared
  cap, the streak bonus, kid tiers (kid-flagged players full price,
  **adults earn half on kid tiers**), save/resume (a round survives
  logout and restart).
- **Website**: plugin table row (ExcelBrainGames 0.1.0) + change-log
  entry in the Nov 4 group, and the ai-knowledge.json regen that feeds
  the Guide — two artifacts, index.html AND ai-knowledge.json.
- **In-game What's New** (ExcelWhatsNew): a self-contained how-to
  entry — open Brain Games from the compass or /games, the two games,
  tiers and payouts, the daily cap, kid tiers live for kids (grown-ups
  earn half there).
- **Broadcaster tips check**: candidate tip "Brain Games are in the
  compass — fresh Word Search and Memory Match puzzles pay real money
  (daily cap applies)"; land it if the rotation has room, note the
  check either way.

## Player proofs owed after install

- **Mike (Bedrock) — the phone test**: one Word Search round + one
  Memory Match round from the compass button: two-tap selection lands
  on the intended letters, found highlighting readable at phone size,
  Memory Match miss-pause feels right, controls (slots 51–53)
  comfortable.
- Any Java player (Jester): one round of each from the menu button
  (rendering parity spot-check).
- Rylie: her first Kid Word Search + Kid Memory rounds (kid tiers are
  live at launch; these are the kid-audience acceptance proof).
- Console help proof: `games admin status` for a player with an active
  round (support path works live).

## Rollback

New plugin, no data migration: remove `plugins/ExcelBrainGames-0.1.0.jar`,
`plugins/ExcelBrainGames/` (player data + earned-money records live in
the ledger YAMLs — keep a copy before deleting), and
`plugins/ExcelMenu/entries/braingames.yml`; `/excelmenu reload`; restart.
Money already paid out stays paid (Vault transactions are not unwound);
the pre-install Shockbyte backup is the full-state rollback of record if
anything else about the window misbehaves.
