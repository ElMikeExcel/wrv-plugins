# RIG RESULTS — ExcelTreeCleanup 0.1.1 (console admin)

Rig: local Paper 26.2-123 server (the unpacked rig tree from
the Quartermaster 0.3.1 rig, copied to a scratch `rig-work/`
folder that was deleted after evidence capture) with
ExcelTreeCleanup-0.1.1.jar (SHA-256 verified 66e5f4a4…21a7 in
the rig plugins folder before boot), GriefPrevention 16.18.7,
and the 0.1.0 scratch rig's TcRigDriver (fake players
RigChopper / RigOwner — reused unchanged from
`~/workspace/exceltreecleanup/scratch/`). Offline mode, flat
world. Full log: `boot.log` in this folder; the exact console
commands: `cmdfile.txt`.

The toggle state was seeded before boot with ONE record
(`toggles-seed.txt`): UUID 11111111-… = off. Every admin
command was typed on the server's stdin — the REAL console
sender (`CONSOLE`), exactly the caller this packet exists for.
The self-toggle proof reuses the 0.1.0 driver's own R6 case,
which drives a fake player's real `/treecleanup off|on`
commands through actual staged tree breaks.

## Run 1 — FAILED, and why this file exists in this shape

The first boot of the first 0.1.1 jar FAILED: Paper rejected
the plugin with `Invalid plugin.yml` (the new long description
was an unquoted plain scalar containing `": "`, which YAML
forbids), so no admin command existed and the driver's R6
self-toggle case failed with it. The description was
double-quoted (the Quartermaster shape), a harness check now
pins the quoting, and the jar was rebuilt (the shipped hash
66e5f4a4… supersedes run 1's 0d814e19…). Everything below is
run 2, against the shipped jar. Run 1's log was discarded with
its rig folder; the failure and the fix are recorded here and
in BUILD-RECORD.md.

## Boot (run 2)

- "Enabling ExcelTreeCleanup v0.1.1" present; enable line:
  `ExcelTreeCleanup enabled: ON, remnant max 3 logs, flood cap
  512, 1 saved player toggle(s), GriefPrevention claim checks
  ON` — the seeded toggle loaded and the GP seam linked, both
  exactly as live 0.1.0 behaves.
- SEVERE: 0 in the whole boot + exercise + shutdown. The 5
  ERROR lines are Paper/environment network generics
  (yggdrasil key fetch, Paper version fetch, world-gen
  "No key layers") — none attributable to ExcelTreeCleanup.
- Clean stop: "Disabling ExcelTreeCleanup v0.1.1".

## Behavior exercise — 20/20 PASS (verbatim lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Boot reaches Done with v0.1.1 enabled | PASS |
| 2 | SEVERE count for the whole run | PASS — 0 |
| 3 | Enable line: seeded state loaded (1 saved toggle), GP claim checks ON | PASS |
| 4 | `treecleanup admin status <uuid>` seeded OFF → "OFF (explicit choice stored)." | PASS |
| 5 | `treecleanup admin status <unknown uuid>` → "ON (default - no explicit choice stored)." | PASS |
| 6 | `treecleanup admin set <uuid> on` → "set ON." + the audit log line naming CONSOLE | PASS |
| 7 | `treecleanup admin status <uuid>` after the set → "ON (explicit choice stored)." | PASS |
| 8 | `treecleanup admin status NobodyHereXYZ` → "No online player named 'NobodyHereXYZ', and it is not a UUID. Nothing was looked up." | PASS |
| 9 | Bare `treecleanup admin` → the combined admin usage line | PASS |
| 10 | `treecleanup admin set <uuid> maybe` → the set usage line; the follow-up status (#11 in cmdfile order) still reads ON (explicit) — nothing changed | PASS |
| 11 | `treecleanup admin bogus x` → the combined admin usage line | PASS |
| 12 | Bare `treecleanup` from console → the exact 0.1.0 player-only guidance line ("treecleanup is a per-player toggle - run it in game as /treecleanup on|off.") — self path unchanged for console | PASS |
| 13 | `tcrig boot` → RIGRESULT BOOT PASS (fake players up) | PASS |
| 14 | `treecleanup admin status RigChopper` → resolves the ONLINE player by name: "ON (default - no explicit choice stored)." with RigChopper's real UUID | PASS |
| 15 | `treecleanup admin set RigChopper off` → "set OFF." by name + audit log line | PASS |
| 16 | `treecleanup admin status RigChopper` → "OFF (explicit choice stored)." | PASS |
| 17 | Self-toggle proof, `tcrig r6` → RIGRESULT R6 PASS: as a real player sender, RigChopper's own `/treecleanup off` left a staged remnant standing and `/treecleanup on` cleaned the identical staging — the 0.1.0 R6 case re-proven against 0.1.1 | PASS |
| 18 | `treecleanup admin status RigChopper` after R6 → "OFF (explicit choice stored)." (R6 leaves it off; the admin view agrees with the self-toggle's result) | PASS |
| 19 | On-disk proof after shutdown (`toggles-after.txt`): exactly two records — the seeded UUID flipped to `=on` by the admin set, RigChopper `=off` from R6 — and NO record for the unknown UUID from check 5 (status reads create nothing) | PASS |
| 20 | Clean stop with "Disabling ExcelTreeCleanup v0.1.1" | PASS |

## Limitations, stated plainly

- Offline targets were addressed by UUID, because that is the
  only offline resolution the plugin supports (online exact
  name → raw UUID; there is no name→UUID record, and 0.1.1 does
  not invent one). The online-name path is proven live in this
  rig by checks 14–16 against the fake player.
- The non-admin denial was NOT exercised in this rig: the only
  player senders available are the driver's fake players, whose
  received chat is not visible in the server log, so a denial
  could not be observed here. The gate arithmetic (console
  allowed / node-holder allowed / non-admin denied) is pinned
  by the AdminCommandTest harness checks, and the node itself
  (default: op) is pinned there against the shipped
  plugin.yml. A live in-game denial check by a non-op family
  member is a fair post-install proof if Mike wants one.
- Gameplay cleanup behaviour beyond the toggle (R1–R5, R7, R8
  of the 0.1.0 rig) was not re-run: 0.1.1 changes no cleanup
  code path (the file diff is command-surface only), and R6 —
  the one case that exercises cleanup through the toggle —
  was re-run and passed.
