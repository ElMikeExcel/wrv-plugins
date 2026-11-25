# BUILD RECORD — ExcelEMFixes 0.19.0 (R2b: dialog beacon derivation)
Item: P-GLOW-BEACON-DIALOG-FIX-20261125
Built: Nov 25, 2026. Installed: Nov 25, 2026, 13:39 CT,
wrv-install-failsafe window wrv-install-failsafe-20261125-1333
(Install 1 of 1). Filing record:
~/workspace/excelemfixes-beacon-r2b-20261125/FILING-RECORD-0.19.0.md

## Why
Jester's Nov 25, 2026 report: carrying "The groves call!" — a quest
whose only objective was talking to King Nemian — the quest got no
beacon beam and no /questwhere answer. Audit:
~/workspace/glow-beacon-audit-20261125/AUDIT.md — 48/105 Primis quests
carry DIALOG objectives, and 46 of 48 dialog quests had no row in the
quest-destinations table, so a tracked dialog quest had no
destination at all. Built under Mike's Nov 25, 2026
guidance-every-step quest law.

## Base
ExcelEMFixes 0.18.0 entire (installed Nov 25, 2026,
wrv-install-failsafe-20261125-1134). Module count unchanged: 22.
Build tree of record:
~/workspace/excelemfixes-beacon-r2b-20261125/build-root/

## The one behavior change (quest-destinations module)
- DIALOG DESTINATION DERIVATION: when a player's tracked or carried
  quest has NO row in the quest-destinations table, its destination
  is derived from the quest's current incomplete DIALOG objective —
  the target NPC's spawn location, read from EliteMobs' own NPC
  registry. The quest beacon and /questwhere then lead to the person
  the quest sends you to talk to; because the current incomplete
  objective is read each time, the beam follows the current objective
  as dialogs complete.
- Table rows keep precedence whenever a row exists (rows encode
  instance entries EliteMobs' data cannot express).
- New config knob (quest-destinations section):
  derive-dialog-destinations (shipped default true). Setting it
  false restores the exact pre-0.19.0 behavior.
- Everything is derived from EliteMobs' own quest/NPC data — no
  quest, NPC or place is named in code — so any content pack using
  dialog objectives is covered.
- All other 21 modules are untouched; no data files change.

## Artifacts
- ExcelEMFixes-0.19.0.jar — 519,968 B / SHA-256
  a499891d3bef50937e8d41e41288c547495b33ddd08d501602f3c286cdb3ac13
  (packet copy and build-root candidate hash-identical, verified at
  filing and again at records time).
- deploy-config.yml — 76,306 B / SHA-256
  0bb8935bb9dfdbd4cd756532ac8887add9e90c7caf22d3787704421e65fa2dda.
  Strict delta of the 0.18.0 deploy config: the dialog-derivation
  comment block + derive-dialog-destinations key in
  quest-destinations, plus two quest-beacon comment updates; no
  other lines removed or changed (diff-verified at filing, 33 diff
  lines, all additions except one comment wording line).

## Tests
Suites: 1,192 passed / 1 failed. The single failure is "Jester-shaped:
armor named first on the tie" in the skill-nudge checks — the
documented pre-existing tie-break flake (salted Map.of iteration in
untouched code) carried in prior ExcelEMFixes packets, unrelated to
the quest-destinations change. Evidence:
~/workspace/excelemfixes-beacon-r2b-20261125/rig/test-rerun-finishonly.log

## Rig proof
Clean boot of the exact candidate jar on the local test rig —
see RIG-RESULTS.md in this package.

## Install of record
wrv-install-failsafe-20261125-1333, Nov 25, 2026: live gates on the
0.18.0 jar (511,858 B / dda8d7a3…) and config (74,747 B / 154815d8…)
MATCH fresh on PC; snapshot-11252026.1336 first; boot Done (34.161s)
13:39:43 CT, "ExcelEMFixes 0.19.0 ready" with 22 modules, emfixes
reload ZERO reload-failed, SEVERE 0, guild OPEN 5 of 5 unchanged;
in-game What's New entry 2026-11-25-quest-beam-people placed in the
same window (changelog 189 entries). Full record: INSTALLED.txt in
this package and
~/workspace/wrv-install-queue/runs/20261125-1333-failsafe/RUN-RECORD.md.

## Proof owed after install
- Iris live check: track a dialog-objective quest with no table row
  (King Nemian dialog quest) and confirm the beacon and /questwhere
  lead to the NPC.
- FEEDBACK-WATCH: Jester's next dialog-target quest.
- Upstream notice follows the standing rule (live-tested first) and
  fires separately; it is not part of this package.
