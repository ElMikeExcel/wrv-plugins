# INSTALL PACKET — ExcelEMFixes 0.17.0 (quest-giver-glow: objective-target glow for dialog objectives)

Lint: 5190 B / max line 102 / non-ASCII 12
Filed: Nov 25, 2026 (recovery lane — the original Nov 25 ~2:19 AM
build lane died at 2:53 AM with nothing filed; rebuilt, proven and
filed by the recovery lane the same day). Backlog: P-GLOW-R1-BUILD /
P-GLOW-BEACON-DIALOG-FIX-20261125. Design source:
~/workspace/glow-beacon-audit-20261125/AUDIT.md section 5, fix R1,
under Mike's Nov 25 guidance-every-step quest law.

## What ships

ExcelEMFixes 0.16.0 -> 0.17.0. ONE module changes:
quest-giver-glow (module #18 of 22; module count unchanged).

- A third per-player glow state, OBJECTIVE: while a player
  carries a quest with an unfinished DIALOG objective, that
  objective's target NPC lights for that player only, in the
  new objective color (shipped default aqua).
- WHY (player feedback): Jester, Nov 25, 2026 — carrying a
  quest whose only objective was to talk to King Nemian,
  Nemian did not glow: every quest Nemian offers was
  chain-locked for him (no accept light), and the turn-in
  light for that very quest can only switch on AFTER the
  talk it was meant to guide him to. The audit counted 71
  dialog objectives across 48 of 105 Primis quests, pointing
  at 64 NPCs, 29 of them outside the glow's world entirely.
- The module's NPC universe now includes dialog targets
  (ordinary EliteMobs NPCs, resolved the same way as givers
  and turn-ins). Precedence at one NPC: turn-in (green) >
  objective (aqua) > accept (gold). Completed dialog
  objectives fall back to the quest's normal states.
- New config knobs (quest-giver-glow section):
  objective-enabled (shipped default true) and
  objective-color (shipped default aqua). WRV runs the
  shipped defaults for both — no WRV delta.
- Everything else is byte-identical behavior to 0.16.0:
  the other 21 modules untouched, no data files change,
  no other plugin changes.

## Artifacts (in this packet folder)

- ExcelEMFixes-0.17.0.jar — 510,196 B / SHA-256
  599a978c0b2fd083554a030e36fbd75fc6d10bee1633cd955b7bb0ceed51d6e7
  Deterministic: two consecutive builds byte-identical.
  Suites: 1,153 passed / 0 failed (0.16.0's 1,142 + 11 new
  glow checks incl. the Jester/Nemian specimen).
- deploy-config.yml — 73,626 B / SHA-256
  3980a61473150c4f4723bf7f80380e6b7b535be8d44d12e6532a6b99a9f0a7d2
  Strict delta of the live config: +23 lines (the objective
  comment block + objective-enabled + objective-color in the
  quest-giver-glow section, after turn-in-color), 0 lines
  removed or changed (diff-verified at filing).
- SHA256SUMS — per-file hashes for this folder.
- RECEIPTS.md — live-premise receipts + rig proof evidence.
- SURFACES.md — same-pass surfaces owed at install.

## Live-premise gate (STOP on mismatch)

Live jar plugins/ExcelEMFixes-0.16.0.jar must equal
506,738 B / SHA-256
ae6b53a6a76c3a208797242c9474dc14951868f2213c6a2432e889af4d734d54
and live plugins/ExcelEMFixes/config.yml must equal
72,329 B / SHA-256
ded17ff994b031ab524ce81a5730580b9f6fd82796e69f2bc8559bc84752fcb3
— both fresh-pulled Nov 25, 2026, 11:30 AM CT (receipts in
RECEIPTS.md). If either differs at install time, STOP,
re-pull, and re-derive deploy-config.yml as the same
insertion on the fresh bytes; never force the swap.

## Ordering

Standalone. (ExcelEMFixes 0.18.0 chains on THIS packet —
it must not install before 0.17.0 is live.)

## Steps

1. Marker discipline per FAILSAFE-FULL-SPEC v2.1 (own v2
   marker; re-check for a foreign marker before Shut Down
   and again before Start).
2. Emptiness: panel/console `list` = 0 players, twice.
3. BACKUP FIRST: snapshot before any change; cap 6, prune
   by CreationTime, never the newest.
4. Stop the server (panel power stop).
5. Replace plugins/ExcelEMFixes-0.16.0.jar with
   ExcelEMFixes-0.17.0.jar (hash-verify the placed bytes).
   Keep the removed 0.16.0 jar in the window backup.
6. Replace plugins/ExcelEMFixes/config.yml with
   deploy-config.yml (hash-verify). Keep the old config in
   the window backup.
7. Start the server.
8. Boot verify (below). Write INSTALLED.txt in this folder.
9. Same-pass surfaces per SURFACES.md.

## Boot verify

- Console: "ExcelEMFixes 0.17.0 ready" with 22 modules.
- Console: a "Quest-Giver Glow census" line whose
  glow-capable NPC count is well above 0.16.0's 76
  (reference rig with full Primis content: 73 quest-giver
  NPCs and 118 glow-capable NPCs — dialog targets joined).
- SEVERE 0 attributable to the swap; guild state
  unchanged; TPS normal after warm-up.

## Rollback

Stop, restore the backed-up ExcelEMFixes-0.16.0.jar and
config.yml, start, verify "ExcelEMFixes 0.16.0 ready".
No data files are touched by this install, so rollback is
a pure jar+config restore.

## Proof owed after install

- Iris live check (staging doctrine): as MikeExcel on the
  PC client, carry a dialog-objective quest and confirm
  the target NPC lights aqua in natural rendering.
- FEEDBACK-WATCH: Jester's next dialog-target quest in
  natural play (the original reporter's case).
- Upstream notice to MagmaGuy is NOT part of this packet;
  it is gated on the live proof above and fires separately.
