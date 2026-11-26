# INSTALL PACKET — ExcelEMFixes 0.21.0 (class-xp-sharing)

Filed: Nov 25, 2026 (build lane). Design source: the
Nov 25, 2026 hidden-mechanics audit (EM's min() +
discard rule is universal across all 76 class forms)
under Mike's ruling D243 (Nov 25, 2026, 6:14 PM CT):
extend the 0.20.0 XP-sharing toggle to all classes.
Extends D242 / 0.20.0 (installed Nov 25, 2026).

## What ships

ExcelEMFixes 0.20.0 -> 0.21.0. ONE existing module
generalized: caster-xp-sharing (module count stays
23); all other modules untouched; no other plugin
changes. The module id stays caster-xp-sharing so
its config section and data file carry over; its
display name becomes "Class XP Sharing".

- WHY: the audit proved every EliteMobs class form
  caps its class level at the LOWER of two foundation
  skills, the tracks earn XP separately, and class XP
  past the cap is discarded — no config lever exists.
  0.20.0 covered only Spellcaster (Jester's report,
  D242). Second live case: Marvin's Paladin is capped
  at 10 (ARMOR 24,853 XP vs SWORDS 54,766 XP).
- WHAT CHANGES: at XP-gain time the module resolves
  the player's ACTIVE form and applies their mode to
  THAT FORM's skill pair (pairs change per form —
  e.g. Berserker root AXES+MACES, Cleric root
  MACES+TRIDENTS; the pair table is the audit's
  probed 76-form catalog). STOCK / SHARED / CARRY
  semantics are exactly 0.20.0's; same EM-API route
  (EliteSkillXpGainEvent MONITOR + PlayerData
  .addSkillXP grants); no DB writes; no recursion.
- SURFACE: one global per-player mode; a "Class XP
  Sharing" row on EVERY skill page of /skillmenu
  (0.20.0: Wands/Staves pages only); command /classxp
  (new) with /casterxp kept as a working alias.
- CATCH-UP: one-time PER CLASS TREE (six root
  lineages). First non-STOCK pick in an uncaught tree
  raises the lagging track of the current form's pair
  to the leading track's total. Grant first, re-read
  + verify, only then write the done-flag; full audit
  record per tree in plugins/ExcelEMFixes/
  caster-xp-sharing.yml.
- MIGRATION (in code, no installer data step): the
  0.21.0 loader converts live 0.20.0 rows in memory —
  modes preserved; a fired 0.20.0 spellcaster
  catch-up becomes the spellcaster tree record and
  can NEVER refire; an owed one stays owed. Saves
  also mirror the spellcaster record back into the
  legacy flat fields, so a rollback to 0.20.0 keeps
  honoring it. The installer touches NO data files.
- Shipped defaults unchanged: default-mode STOCK,
  catch-up on. WRV runs the shipped defaults.
  Setting the module off in config restores exact
  pre-module behavior.

## Artifacts (in this packet folder)

- ExcelEMFixes-0.21.0.jar — 552,041 B / SHA-256
  4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a
  Deterministic build (two builds byte-identical).
  Suites: 1,843 passed / 0 failed on clean runs. One
  intermittent single failure is possible: the
  documented pre-existing skill-nudge tie-break flake
  ("armor named first on the tie"), reproduced on the
  pristine 0.20.0 tree as well; see RECEIPTS.md.
- deploy-config.yml — 81,482 B / SHA-256
  949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f
  Delta of the live 0.20.0 config: the entire diff
  sits inside the caster-xp-sharing section —
  comments rewritten (reason updated) + the two
  player-facing message strings; every functional
  key/value unchanged (diff-verified at filing).
- SHA256SUMS — per-file hashes for this folder.
- RECEIPTS.md — live-premise gate source + rig proof.
- SURFACES.md — same-pass surfaces owed at install.

## Live-premise gate (STOP on mismatch)

At install time, live must be ExcelEMFixes 0.20.0:
plugins/ExcelEMFixes-0.20.0.jar = 543,319 B / SHA-256
a4999341b2238d2f26a827a411e0e2d19cdba24ee29daa64e21cc5b685697cd2
and plugins/ExcelEMFixes/config.yml = the 0.20.0
deploy config, 80,048 B / SHA-256
40f3ed445c43ec3dff32bbc51677fe1144d7ea580d8f39075242df53adf8839f
(the 0.20.0 packet's own post-install state — verify
from that packet's INSTALLED.txt, and re-pull fresh
at install; if anything is ambiguous or mismatched,
STOP and re-base). If 0.20.0 is not live, STOP: this
packet never installs ahead of it. Verification:
sha256sum on the live jar + config after a same-day
pull; expected outputs are the hashes above.

## Ordering

CHAINS ON ExcelEMFixes 0.20.0 (installed Nov 25,
2026, wrv-install-failsafe-20261125-1752).
Independent of every other pending entry. This is a
player fix: it installs at the first verified-empty
moment.

## Steps

1. Marker discipline per FAILSAFE-FULL-SPEC v2.1
   (own v2 marker; re-check for a foreign marker
   before Shut Down and again before Start).
2. Emptiness: panel/console `list` = 0 players,
   twice.
3. BACKUP FIRST: snapshot before any change; cap 6,
   prune by CreationTime, never the newest.
4. Stop the server (panel power stop).
5. Replace plugins/ExcelEMFixes-0.20.0.jar with
   ExcelEMFixes-0.21.0.jar (hash-verify the placed
   bytes). Keep the removed 0.20.0 jar in the window
   backup.
6. Replace plugins/ExcelEMFixes/config.yml with
   deploy-config.yml (hash-verify). Keep the old
   config in the window backup.
7. Start the server.
8. Boot verify (below). Write INSTALLED.txt in this
   folder.
9. Same-pass surfaces per SURFACES.md.

## Boot verify

- Console: "ExcelEMFixes 0.21.0 ready" with 23
  modules.
- Console: "Class XP Sharing ready" module line,
  reporting the stored player record count carried
  over from 0.20.0's data file.
- Console: `emfixes reload` with ZERO "reload
  failed" lines.
- SEVERE 0 attributable to the swap; guild state
  unchanged; TPS normal after warm-up.
- Marker: ~/workspace/excelemfixes-class-xp-sharing-
  20261125/packet-0.21.0/INSTALLED.txt

## Rollback

Stop, restore the backed-up ExcelEMFixes-0.20.0.jar
and config.yml, start, verify "ExcelEMFixes 0.20.0
ready". The runtime state file (plugins/ExcelEMFixes/
caster-xp-sharing.yml) stays valid under 0.20.0:
0.21.0 mirrors every spellcaster catch-up into the
legacy flat fields 0.20.0 reads, and its per-tree
records are inert extra keys — leave the file in
place; no player can be caught up twice in either
direction.

## Proof owed after install

- MARVIN LIVE PROOF: Marvin picks any non-STOCK mode
  (menu row on any skill page, or /classxp). His
  one-time Paladin-tree catch-up fires at that
  moment: at Nov 25 values (ARMOR 24,853 / SWORDS
  54,766) ARMOR rises to the SWORDS total and his
  Paladin cap lifts from 10. Verify from EM's player
  data after his first toggle, then the report-back
  to Marvin fires (see the lane's D228 follow-on
  drop).
- JESTER: his spellcaster catch-up state carries
  over from 0.20.0 untouched — if it already fired,
  nothing refires; if still owed, it fires on his
  first non-STOCK pick under either version. His
  report-back is wired from the 0.20.0 lane; do NOT
  duplicate it from this packet.
- FEEDBACK-WATCH: players' class levels tracking
  their chosen modes in normal play.
- Upstream notice is NOT part of this packet; it
  follows the standing rule (live-tested first) and
  fires separately.
