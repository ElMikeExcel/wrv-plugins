# INSTALL PACKET — ExcelEMFixes 0.20.0 (caster-xp-sharing)

Filed: Nov 25, 2026 (build lane). Backlog:
P-JESTER-GLADIUS-EJECT-XPPAUSE-20261125. Design source:
Jester's Nov 25, 2026 report (Scrubs group + DM) under
Mike's ruling D242 (Nov 25, 2026, 4:45 PM CT): build a
per-player toggle for caster XP sharing, with a one-time
catch-up for the existing gap.

## What ships

ExcelEMFixes 0.19.0 -> 0.20.0. ONE new module,
caster-xp-sharing (module count 22 -> 23); all other
modules untouched; no other plugin changes.

- WHY: EliteMobs' Spellcaster class level is
  min(WANDS, STAVES) skill levels. The two tracks earn
  XP separately and class XP past the cap is discarded;
  EM has no config lever for this (verified against the
  EM jar, file-named 10.9.10 / internal 10.9.14). Jester
  hit it live: WANDS 240,760 XP (Lv 18) vs STAVES 41,791
  (Lv 12), class paused at 12, and he had to fight a
  level-25 elite at effective level 10.
- The module cannot edit EM's min() formula, so it
  works by granting offsetting skill XP into the
  lagging track through EM's own API
  (PlayerData.addSkillXP) — no DB writes, no console
  commands. Gains are observed on EM's
  EliteSkillXpGainEvent (MONITOR priority).
- Per-player mode, three settings, changeable any time
  with no relog, from a "Caster XP Sharing" row on the
  Wands and Staves pages of /skillmenu (Java + Bedrock
  share the same Views/MenuRenderer surface) or the
  /casterxp command:
  - STOCK: no intervention; EM default behavior.
  - SHARED: each WANDS/STAVES gain is split across
    both tracks (gained track keeps ceil(g/2), the
    other is granted floor(g/2); XP-neutral).
  - CARRY: gains on the player's leading track also
    grant offsetting XP into the lagging track, so
    the class minimum rises with their main weapon.
- ONE-TIME CATCH-UP: the first time a player picks a
  non-STOCK mode, if their two caster tracks differ,
  the lagging track is raised to the leading track's
  XP. Recorded per player (with an audit trail) in
  plugins/ExcelEMFixes/caster-xp-sharing.yml; it can
  never fire twice, and the done-flag is only written
  after the grant is re-read and verified landed.
- Shipped defaults: default-mode STOCK, catch-up on.
  WRV runs the shipped defaults — no WRV delta.
  Setting the module off in config restores exact
  pre-0.20.0 behavior.
- Scope: the WANDS/STAVES pair only (the reported
  case). EM's catalog gives every class the same
  two-track min() shape (adventurer, paladin,
  berserker, ranger, cleric pairs noted in the build
  record); they are NOT covered by this module.

## Artifacts (in this packet folder)

- ExcelEMFixes-0.20.0.jar — 543,319 B / SHA-256
  a4999341b2238d2f26a827a411e0e2d19cdba24ee29daa64e21cc5b685697cd2
  Deterministic build (two builds byte-identical).
  Suites: 1,259 passed / 0 failed on clean runs. One
  intermittent single failure is possible: the
  documented pre-existing skill-nudge tie-break flake
  ("armor named first on the tie"), reproduced on the
  pristine 0.19.0 tree as well; see RECEIPTS.md.
- deploy-config.yml — 80,048 B / SHA-256
  40f3ed445c43ec3dff32bbc51677fe1144d7ea580d8f39075242df53adf8839f
  Strict delta of the 0.19.0 deploy config: the
  caster-xp-sharing section appended (73 added lines,
  0 removed, diff-verified at filing).
- SHA256SUMS — per-file hashes for this folder.
- RECEIPTS.md — live-premise gate source + rig proof.
- SURFACES.md — same-pass surfaces owed at install.

## Live-premise gate (STOP on mismatch)

At install time, live must be ExcelEMFixes 0.19.0:
plugins/ExcelEMFixes-0.19.0.jar = 519,968 B / SHA-256
a499891d3bef50937e8d41e41288c547495b33ddd08d501602f3c286cdb3ac13
and plugins/ExcelEMFixes/config.yml = the 0.19.0 deploy
config, 76,306 B / SHA-256
0bb8935bb9dfdbd4cd756532ac8887add9e90c7caf22d3787704421e65fa2dda
(the 0.19.0 packet's own post-install state — verify
from that packet's INSTALLED.txt, and re-pull fresh at
install; if anything is ambiguous or mismatched, STOP
and re-base). If 0.19.0 is not live, STOP: this packet
never installs ahead of it. Verification command:
sha256sum on the live jar + config after a same-day
pull; expected outputs are the hashes above.

## Ordering

CHAINS ON ExcelEMFixes 0.19.0 (installed Nov 25, 2026,
wrv-install-failsafe-20261125-1333). Independent of
every other pending entry. This is a player fix: it
installs at the first verified-empty moment.

## Steps

1. Marker discipline per FAILSAFE-FULL-SPEC v2.1 (own
   v2 marker; re-check for a foreign marker before
   Shut Down and again before Start).
2. Emptiness: panel/console `list` = 0 players, twice.
3. BACKUP FIRST: snapshot before any change; cap 6,
   prune by CreationTime, never the newest.
4. Stop the server (panel power stop).
5. Replace plugins/ExcelEMFixes-0.19.0.jar with
   ExcelEMFixes-0.20.0.jar (hash-verify the placed
   bytes). Keep the removed 0.19.0 jar in the window
   backup.
6. Replace plugins/ExcelEMFixes/config.yml with
   deploy-config.yml (hash-verify). Keep the old
   config in the window backup.
7. Start the server.
8. Boot verify (below). Write INSTALLED.txt in this
   folder.
9. Same-pass surfaces per SURFACES.md.

## Boot verify

- Console: "ExcelEMFixes 0.20.0 ready" with 23 modules.
- Console: "Caster XP Sharing ready" module line.
- Console: `emfixes reload` with ZERO "reload failed"
  lines.
- SEVERE 0 attributable to the swap; guild state
  unchanged; TPS normal after warm-up.
- Marker: ~/workspace/excelemfixes-caster-xp-toggle-
  20261125/packet-0.20.0/INSTALLED.txt

## Rollback

Stop, restore the backed-up ExcelEMFixes-0.19.0.jar
and config.yml, start, verify "ExcelEMFixes 0.19.0
ready". The module's runtime state file
(plugins/ExcelEMFixes/caster-xp-sharing.yml) is inert
under 0.19.0 — leave it in place; if 0.20.0 is later
reinstalled, catch-up records in it stay honored, so
no player can be caught up twice.

## Proof owed after install

- JESTER LIVE PROOF (the real one): Jester picks any
  non-STOCK mode (menu row or /casterxp). His one-time
  catch-up fires at that moment: at Nov 25 values
  (WANDS 240,760 / STAVES 41,791) STAVES rises to the
  WANDS total and his Spellcaster class un-pauses
  from 12 toward 18. Verify from EM's player data
  after his first toggle, then the report-back to
  Jester fires (see the lane's D228 follow-on drop).
- FEEDBACK-WATCH: Jester's next caster fights — class
  level tracking his chosen mode.
- Upstream notice is NOT part of this packet; it
  follows the standing rule (live-tested first) and
  fires separately.
