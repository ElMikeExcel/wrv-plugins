# BUILD RECORD — ExcelEMFixes 0.20.0 (caster-xp-sharing)
Item: P-JESTER-GLADIUS-EJECT-XPPAUSE-20261125
Built: Nov 25, 2026, for Mike's ruling D242 (Nov 25, 2026,
4:45 PM CT). Installed: Nov 25, 2026, 17:57 CT,
wrv-install-failsafe window wrv-install-failsafe-20261125-1752
(Install 1 of 1). Build lane record:
~/workspace/excelemfixes-caster-xp-toggle-20261125/BUILD-RECORD.md

## Why
Jester_Turtle reported Nov 25, 2026: his Spellcaster class was
paused at level 12 because EliteMobs sets the class level to
min(WANDS, STAVES) skill levels, the two tracks earn XP
separately, and his had drifted far apart — SkillXP_WANDS
240,760 (Lv 18) vs SkillXP_STAVES 41,791 (Lv 12), a 198,969 XP
gap. Class XP past the min() cap is discarded, not banked, and
EM exposes no config lever (verified in the EM jar; no such
setting in AdvancedCombatSystem.yml). He had to fight a
level-25 elite at effective level 10.

## Base
ExcelEMFixes 0.19.0 entire (installed Nov 25, 2026,
wrv-install-failsafe-20261125-1333). One module added:
22 -> 23 modules. Build tree of record:
~/workspace/excelemfixes-caster-xp-toggle-20261125/build-root/

## The new module (caster-xp-sharing, the 23rd module)
- Id: caster-xp-sharing. Display: "Caster XP Sharing".
  Package excelemfixes.casterxp. Individually toggleable and
  config-driven like every pack module; ZERO WRV hardcoding;
  shipped defaults = WRV values (default-mode STOCK,
  catch-up-enabled true).
- Per-player mode (persisted per UUID in
  plugins/ExcelEMFixes/caster-xp-sharing.yml):
  - STOCK — no intervention, EM default.
  - SHARED — each WANDS/STAVES gain is split across both
    tracks: gained track keeps ceil(g/2), the other track is
    granted floor(g/2). XP-neutral.
  - CARRY — gains on the player's leading track also grant
    offsetting XP into the lagging track, so the class
    minimum rises with their main weapon; lagging-track
    gains mirror only overshoot past a tie.
- One-time catch-up: fires only on a player's explicit first
  non-STOCK selection (menu set action or /casterxp), never
  in STOCK, never on login, never twice. Raises the lagging
  track to the leading track's CURRENT total; the done-flag
  is recorded only after re-reading and verifying the raise;
  audit stored per player (timestamp, track raised, amount,
  both before-totals).
- Player surface: a "Caster XP Sharing" row
  (EXPERIENCE_BOTTLE) on the Wands and Staves pages of
  /skillmenu (Java chest + Bedrock form parity via the shared
  MenuRenderer), opening the module's view with the three
  mode buttons and a Back button; plus the /casterxp command
  fallback (/casterxp [stock|shared|carry|status]). Mode
  changes take effect immediately, no relog.
- XP route: EliteSkillXpGainEvent (MONITOR) + PlayerData
  addSkillXP grants (fires no event, so grants cannot
  recurse); EM isolation per the pack pattern — the module
  idles cleanly when EM is absent.
- Scope: the WANDS/STAVES pair ONLY, per the brief's scope
  guard; EM's other two-track classes are noted in the build
  record, not silently included.

## Artifacts
- ExcelEMFixes-0.20.0.jar — 543,319 B / SHA-256
  a4999341b2238d2f26a827a411e0e2d19cdba24ee29daa64e21cc5b685697cd2
  (packet copy and build-root candidate hash-identical; two
  consecutive builds byte-identical — deterministic).
- deploy-config.yml — 80,048 B / SHA-256
  40f3ed445c43ec3dff32bbc51677fe1144d7ea580d8f39075242df53adf8839f.
  Delta vs the 0.19.0 deploy config: 73 lines added, 0
  removed — the caster-xp-sharing section only
  (diff-verified at filing).

## Tests
Baseline (pristine 0.19.0 tree, build lane): 1,193 passed /
0 failed. Final: 1,259 checks (66 new in CasterXpTest:
split/carry/catch-up math incl. Jester's exact numbers —
240,760 / 41,791 / gap 198,969 — state round-trip, view
building, mode parsing, skills-row visibility), full-green
runs achieved repeatedly ("TOTAL: 1259 passed, 0 failed").
Known flake (pre-existing, NOT from this module):
SkillNudgeMathTest "Jester-shaped: armor named first on the
tie" fails intermittently on BOTH trees — it iterates a
java.util.Map.of whose order is salted per JVM launch;
carried in prior packets.

## Rig proof
Paper 26.1.2 rig, CasterBot staged Jester-shaped — see
RIG-RESULTS.md in this package. VERDICT: PASS on every leg.

## Install of record
wrv-install-failsafe-20261125-1752, Nov 25, 2026: live gates
on the 0.19.0 jar (519,968 B / a499891d…) and config
(76,306 B / 0bb8935b…) MATCH fresh on PC; emptiness gate
0/10 twice; snapshot-11252026.1754 first; boot Done (37.536s)
17:57:23 CT, "ExcelEMFixes 0.20.0 ready" with 23 modules
incl. caster-xp-sharing, "Caster XP Sharing ready" line
(default STOCK, catch-up on, 0 stored records), emfixes
reload ZERO reload-failed, SEVERE 0, guild OPEN 5 of 5
unchanged; in-game What's New entry
2026-11-25-caster-xp-sharing placed in the same window
(changelog 190 entries) and an AutoBroadcaster caster
sharing tip appended + reloaded. Full record: INSTALLED.txt
in this package and
~/workspace/wrv-install-queue/runs/20261125-1752-failsafe/RUN-RECORD.md.

## Proof owed after install
- JESTER LIVE PROOF: Jester picks any non-STOCK mode; his
  one-time catch-up fires at that moment (at Nov 25 values
  WANDS 240,760 / STAVES 41,791 -> STAVES rises to the WANDS
  total); verify from EM player data after his first toggle,
  then the report-back fires (separate D228 drop — NOT part
  of this package).
- FEEDBACK-WATCH: Jester's next caster fights.
- Upstream notice follows the standing rule (live-tested
  first) and fires separately; it is not part of this
  package.
