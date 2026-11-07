# INSTALL PACKET — ExcelProtect 0.1.6 "Recovery" (working time filters + entity rollback + inventory rollback + container undo + WorldEdit logging)

**What:** swap the live ExcelProtect 0.1.5 jar for 0.1.6. 0.1.6 fixes
the dead `from=`/`to=` absolute-time filters (and makes malformed
filter values fail loudly instead of silently no-oping), adds
`/excelprotect restore-entity` (re-create a recorded dead entity —
type, place, name, equipment, tame owner — once-only + force), adds
`/excelprotect restore-inventory` (slot-exact REPLACE of a player's
inventory + ender chest from a death snapshot, sharing the deaths
once-only marks), extends rollback undo to containers, and records
WorldEdit edits into the blocks stream via a fail-soft hook (new
toggle `log.blocks.worldedit`, default ON). Everything 0.1.5 does is
carried unchanged; stream formats are untouched. **Config:** the
live config may stay as-is (the one new key defaults ON when absent,
which is the desired state); placing the shipped 0.1.6 `config.yml`
is equally safe if the live file is first hash-checked against the
0.1.5 shipped config — when in doubt, do not touch the config.

**Until this build lands: do NOT use `to=` / `from=` filters on the
live server** — on 0.1.5 they are silently dead (see
`~/workspace/protect-rollback-proof/VERDICT.md`). Use `hours=` only.
**After this install,** run the live rollback proof on the scratch
pad per `~/workspace/protect-rollback-proof/LIVE-PROOF-PACKET.md`
(it exercises the fixed `to=` path live) — that proof is the gate
for scheduling CoreProtect's retirement.

## Gate — read before touching anything

- **BACKUP FIRST + cap-at-6:** take a fresh Shockbyte backup before
  the swap. Before creating it, prune the OLDEST manual backups until
  the post-creation total is ≤ 6; never delete the Daily Backup's
  newest copy or the two newest manual restore points; name prunees
  in the report.
- **VERIFIED-EMPTY:** install only with the server verified empty
  (list = 0 twice) per the standing gate (weekend countdown protocol
  supersedes on Fri/Sat if Mike directs).
- **PANEL DISCIPLINE:** one panel actor; create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window
  start, delete at finish; re-check the marker immediately before
  Shut Down and again before Start.
- **QUEUE ORDER:** 0.1.5 is LIVE (installed Nov 6, 2026). This entry
  swaps 0.1.5 → 0.1.6 directly. Never skip a version.

## LIVE GATE — jar identity, or STOP

The live `plugins/ExcelProtect-0.1.5.jar` MUST be exactly:
- size **225,181 B**, SHA-256
  `c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`

If it is anything else, STOP and reconcile before proceeding.

## Candidate

- `ExcelProtect-0.1.6.jar` — **234,451 B**, SHA-256
  `097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`
- Fingerprint inside (`excel-build.txt`):
  `EXC-PROTECT-0.1.6-WRV-20261106-01-c016rf`
- plugin.yml version `0.1.6`, authors `[Iris, Mike]`, softdepend
  gains `WorldEdit` (present live: 7.4.6).

## Swap

1. Stop the server (verified empty; marker discipline above).
2. Fresh backup per BACKUP FIRST + cap-at-6 (this backup is the
   rollback plan).
3. Replace `plugins/ExcelProtect-0.1.5.jar` with the candidate jar
   (keep the filename `ExcelProtect-0.1.6.jar`). Config untouched
   unless hash-checked per the note above.
4. Start the server.

## Boot-verify (all must appear; any SEVERE attributable to
ExcelProtect = rollback)

- `[ExcelProtect] READY version=0.1.6` with `we=on` in the same line
- `[ExcelProtect] worldedit logging: ON (WorldEdit edits recorded into the blocks stream, actor-attributed)`
- `[ExcelProtect] recovery: ON rollback=blocks+containers+undo(incl. containers) restore-entity restore-inventory (0.1.6)`
- All 0.1.5 boot lines unchanged (streams, combat, damage, dealt,
  healing, locations, clarity, timelines).
- `/excelprotect status` reports `version=0.1.6` and `we=on`.
- Boot completes with the guild line unchanged (`guild LOCKED 3 of 5`
  as of this writing) and plugin count 77.

## Post-install proofs (in order)

1. **Live rollback proof** per
   `~/workspace/protect-rollback-proof/LIVE-PROOF-PACKET.md`
   (scratch pad ≥200 blocks from builds, console-driven, empty
   server) — this is the first live use of the fixed `to=` filter.
2. First real WorldEdit edit by staff → confirm a `worldedit` line
   lands in the day's blocks stream with the actor's name.
3. First `restore-entity` / `restore-inventory` uses are staff
   discretion; both are preview-first and self-log in the admin +
   system streams.

## Rollback

Restore the pre-install backup (or swap the 0.1.5 jar back and
restart). 0.1.6 writes no new stream formats, so a downgrade loses
nothing recorded while it was live (WorldEdit lines are ordinary
block_change records; restore marks live in the existing Recovery
file).
