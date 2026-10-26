# ExcelEliteLadder 0.1.0: Elite Seal Ladder auto-apply (NEW PLUGIN, STAGED, not live)

Built Sun Oct 25, 2026, about 2:00 to 2:20 PM CDT on the box. Deterministic build: two clean
builds give the same jar sha (see SHA256SUMS.txt in the release folder).

## Why
ExcelMagicGate switches Magic configs on seal breaks, but nothing switched the EliteMobs
ladder. ExcelRewardGuard only steps reward multipliers and ExcelTrainingWheels only reads the
live EliteMobs values. Every ladder step was a manual whole-file swap.

## What it does
- Watches ExcelGuildGate's seal count (GuildGateAPI.sealsBrokenCount, by reflection) every
  `poll-seconds` (default 30) and one second after any player quits.
- When the seal count goes up, the matching ladder step becomes pending.
- Default: the step waits for an empty server (0 players). Then, in one tick:
  1. Drift guard: the live MobCombatSettings.yml must equal the applied step (or already
     equal the target). Anything else is refused, with the reason logged, and nothing changes.
  2. Dated backup `backups/MobCombatSettings.yml.<yyyyMMdd-HHmmss>-from-S<n>.bak`
     (created new, never overwritten, read back and compared).
  3. Only the six ladder lines are rewritten (spawn L6, cap L9, dungeon L91/L94,
     scaled L109/L112). Every other byte stays the same, including CRLF if present.
  4. Written to a temp file, then an atomic move, then read back and checked: same line
     count, only ladder lines changed, values equal the step, full YAML parse with the same
     top-level keys.
  5. Any failure restores the original bytes from the backup, skips the reload, and sets a
     failure latch (kept across restarts) until `/eliteladder retry`.
  6. Runs `em reload` (configurable) from the console. About 20 s later it re-reads the file
     and checks the values and sha (post-reload check).
- Up only. A GuildGate reset or revoke logs one warning and never steps down.
- A seal jump (for example 1 to 4) goes straight to the target step.
- Baseline: on first start it adopts the live file if it equals a ladder step. The old S0 file
  and the old S1 file (dungeon lines 1.0/1.0) are named in the refusal and never touched.
- Log and console line on each apply: "EliteLadder applied S<n> (from S<m>, seals k, p online):
  <values>. Backup <name>. File sha <sha>. Running 'em reload'." The same line plus the
  post-reload check go to `plugins/ExcelEliteLadder/apply-log.txt`.
- `/eliteladder status | check | retry` (permission exceleliteladder.admin, op only).
  status shows the current seals, applied step, which step the live file equals, anything
  pending (or why it is refused or blocked), the last result, the last apply and backup, and
  the reload flag.

## The ladder (built into the jar, not editable in config)
| step | spawn | cap | toElite (L109) | toPlayer (L112) | dungeon toElite/toPlayer (L91/L94) |
|---|---|---|---|---|---|
| S1 | 0.07 | 120 | 0.55 | 1.75 | 0.5 / 2.0 |
| S2 | 0.08 | 120 | 0.50 | 1.90 | 0.5 / 2.0 |
| S3 | 0.09 | 120 | 0.50 | 2.05 | 0.5 / 2.0 |
| S4 | 0.10 | 120 | 0.45 | 2.20 | 0.5 / 2.0 |
| S5 | 0.11 | 120 | 0.45 | 2.20 | 0.5 / 2.0 |

## EliteMobs rewrites the file on load (found on the rig, handled)
EliteMobs 10.9.10 saves MobCombatSettings.yml back on every boot and `em reload` in its own
number form: 0.10 -> 0.1, 2.20 -> 2.2, 0.50 -> 0.5, 1.90 -> 1.9. Values do not change. So:
- the plugin writes numbers in that same form, so EliteMobs' rewrite leaves its bytes
  unchanged (proven: post-reload sha equals the written sha on every step);
- every comparison is by value, never by bytes;
- the staged S2 to S5 -norm files change sha the first time EliteMobs loads them.
  The ladder shas after a load are the "EM form" shas in SHA256SUMS.txt. S1-norm is already
  in EM form, so 57ad4ab2... stays stable.

## Config (config.yml)
- enabled: true
- poll-seconds: 30
- allow-reload-with-players-online: false (keep off; true applies at once and runs
  `em reload` with players on, which removes live elites and ends EliteMobs fights but kicks nobody)
- reload-command: "em reload"
- post-reload-check-seconds: 20

## Rig-only test seam
JVM flag `-Dexceleliteladder.rigTestFault=beforeMove|afterMove` forces a failed write or a
corrupt write, so the restore path can be proven. It logs a loud WARNING at start.
Never set it on a real server. Without the flag, the code path is inert.

## Tests
- Plain-JVM suite: 57 passed, 0 failed (`./run-tests.sh`). Fixtures are the staged
  MobCombatSettings.yml files only.
- Box rig (Paper 26.3, EliteMobs 10.9.10, ExcelGuildGate 0.3.3, ExcelTrainingWheels 0.1.6):
  seal 1 to 5 walk, empty wait, flag on, refusals, failed write restore, latch and retry,
  reset with no step-down. See RIG-TEST-RESULTS.md in the release folder.
