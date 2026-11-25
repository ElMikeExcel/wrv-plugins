# INSTALL PACKET — ExcelEMFixes 0.19.0 (R2b: dialog beacon derivation)

Lint: 5374 B / max line 70 / non-ASCII 12
Filed: Nov 25, 2026 (filing-only lane; jar built earlier the
same day, never rebuilt by this lane). Backlog:
P-GLOW-BEACON-DIALOG-FIX-20261125. Design source: Jester's
Nov 25, 2026 report (carried quest whose only objective was
talking to King Nemian got no beam and no /questwhere
answer), under Mike's Nov 25 guidance-every-step quest law.

## What ships

ExcelEMFixes 0.18.0 -> 0.19.0. ONE behavior changes, in the
quest-destinations module (module count unchanged, 22):

- DIALOG DESTINATION DERIVATION: when a player's tracked or
  carried quest has NO row in the quest-destinations table,
  its destination is derived from the quest's current
  incomplete DIALOG objective — the target NPC's spawn
  location, read from EliteMobs' own NPC registry. The
  quest beacon and /questwhere then lead to the person the
  quest sends you to talk to, and because the current
  incomplete objective is read each time, the beam follows
  the current objective as dialogs complete.
- Table rows keep precedence whenever a row exists (rows
  encode instance entries EliteMobs' data cannot express).
- WHY: the destinations table only covered tracker-blank
  kill quests; 46 of 48 dialog quests had no row, so a
  tracked dialog quest got no beam and no directions.
- New config knob (quest-destinations section):
  derive-dialog-destinations (shipped default true). WRV
  runs the shipped default — no WRV delta. Setting it
  false restores the exact pre-0.19.0 behavior.
- Everything is derived from EliteMobs' own quest/NPC
  data — no quest, NPC or place is named in code — so any
  content pack using dialog objectives is covered.
- All other 21 modules are untouched; no data files
  change; no other plugin changes.

## Artifacts (in this packet folder)

- ExcelEMFixes-0.19.0.jar — 519,968 B / SHA-256
  a499891d3bef50937e8d41e41288c547495b33ddd08d501602f3c286cdb3ac13
  Suites: 1,192 passed / 1 failed (the single failure is
  the documented pre-existing skill-nudge tie-break check,
  "armor named first on the tie" — the same flake carried
  in prior ExcelEMFixes packets; see RECEIPTS.md).
- deploy-config.yml — 76,306 B / SHA-256
  0bb8935bb9dfdbd4cd756532ac8887add9e90c7caf22d3787704421e65fa2dda
  Strict delta of the 0.18.0 deploy config: the dialog
  derivation comment block + derive-dialog-destinations
  key in quest-destinations, plus two quest-beacon comment
  updates; no other lines removed or changed
  (diff-verified at filing, 33 diff lines, all additions
  except one comment wording line).
- SHA256SUMS — per-file hashes for this folder.
- RECEIPTS.md — live-premise gate source + rig proof.
- SURFACES.md — same-pass surfaces owed at install.

## Live-premise gate (STOP on mismatch)

At install time, live must be ExcelEMFixes 0.18.0:
plugins/ExcelEMFixes-0.18.0.jar = 511,858 B / SHA-256
dda8d7a3ffca9ae78befe257a201fabca9b40a71b3eef38336abbfeef5f001c6
and plugins/ExcelEMFixes/config.yml = the 0.18.0 deploy
config, 74,747 B / SHA-256
154815d8a21e3eece3248cd9fd0fdcff33be211f18fb0b0f3409d3a7328b135e
(the 0.18.0 packet's own post-install state — verify from
that packet's INSTALLED.txt / the MANIFEST-ARCHIVE
close-out, and re-pull fresh at install; if anything is
ambiguous or mismatched, STOP and re-base). If 0.18.0 is
not live, STOP: this packet never installs ahead of it.
Verification command: sha256sum on the live jar + config
after a same-day pull; expected outputs are the hashes
above.

## Ordering

CHAINS ON ExcelEMFixes 0.18.0 (installed Nov 25, 2026,
wrv-install-failsafe-20261125-1134). Independent of every
other pending entry.

## Steps

1. Marker discipline per FAILSAFE-FULL-SPEC v2.1 (own v2
   marker; re-check for a foreign marker before Shut Down
   and again before Start).
2. Emptiness: panel/console `list` = 0 players, twice.
3. BACKUP FIRST: snapshot before any change; cap 6, prune
   by CreationTime, never the newest.
4. Stop the server (panel power stop).
5. Replace plugins/ExcelEMFixes-0.18.0.jar with
   ExcelEMFixes-0.19.0.jar (hash-verify the placed bytes).
   Keep the removed 0.18.0 jar in the window backup.
6. Replace plugins/ExcelEMFixes/config.yml with
   deploy-config.yml (hash-verify). Keep the old config
   in the window backup.
7. Start the server.
8. Boot verify (below). Write INSTALLED.txt in this folder.
9. Same-pass surfaces per SURFACES.md.

## Boot verify

- Console: "ExcelEMFixes 0.19.0 ready" with 22 modules.
- Console: `emfixes reload` with ZERO "reload failed"
  lines.
- SEVERE 0 attributable to the swap; guild state
  unchanged; TPS normal after warm-up.
- Marker: ~/workspace/excelemfixes-beacon-r2b-20261125/
  packet-0.19.0/INSTALLED.txt

## Rollback

Stop, restore the backed-up ExcelEMFixes-0.18.0.jar and
config.yml, start, verify "ExcelEMFixes 0.18.0 ready".
No data files are touched by this install, so rollback
is a pure jar+config restore.

## Proof owed after install

- Iris live check (staging doctrine): as MikeExcel,
  track a dialog-objective quest with no table row
  (King Nemian dialog quest) and confirm the beacon and
  /questwhere lead to the NPC.
- FEEDBACK-WATCH: Jester's next dialog-target quest.
- Upstream notice is NOT part of this packet; it follows
  the standing rule (live-tested first) and fires
  separately.
