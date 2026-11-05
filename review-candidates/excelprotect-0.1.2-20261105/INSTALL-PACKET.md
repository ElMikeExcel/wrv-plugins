# ExcelProtect 0.1.2 — INSTALL PACKET (damage incidents)

Status: **BUILT + TESTED + RIG-PROVEN — awaiting its install gate.** Filed PENDING
in `~/workspace/wrv-install-queue/MANIFEST.md`; the failsafe lane installs at its
gate. This is a jar+config change — **fully gated**: BACKUP-FIRST + cap-at-6 +
verified-empty server (or weekend countdown protocol). NOT a text live-edit.

Build folder: `~/workspace/protect-damage-build/` · Spec: `SPEC.md` ·
Tests/rig: `TEST-REPORT.md` · Build record: `BUILD-RECORD.md`.

## The change

ExcelProtect gains a `damage` ledger stream: one record per **damage incident**
(a player's continuous exposure to one source), so elite/environment damage
tuning finally has per-incident evidence — the gap behind the Nov 5 Kailey
lava near-miss investigation. New: `DamageIncidents` (pure coalescer),
`DamageWatch` (MONITOR listener glue), `EliteProbe` (reflective EliteMobs
attribution — `EntityTracker.isEliteMob` / `getEliteMobEntity().getLevel()`,
verified against the real EliteMobs 10.9.12 jar), `DamageReport`
(`/excelprotect damage <player> [last-hours]` console query), 3 config keys,
`Exporter.STREAMS` + `damage`. Everything else is 0.1.1 behavior, unchanged.

## Artifacts

| Item | Value |
|---|---|
| Candidate jar | `~/workspace/protect-damage-build/ExcelProtect-0.1.2.jar` (also staged in the records package folder) |
| Size | **138,039 B** |
| SHA-256 | **`26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`** |
| Determinism | Two clean builds byte-identical (`ExcelProtect-0.1.2-repro.jar` in the build folder hashes the same) |
| Tests | 26/26 JUnit checks pass, 0 failures (16 carried from 0.1.1 unchanged + 10 new) |
| Live jar today | `plugins/ExcelProtect-0.1.1.jar`, 122,081 B, SHA-256 `b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b` (the 0.1.1 install's own verified anchor — `packages/excelprotect-0.1.1-20261104/BUILD-RECORD.md`) |

## LIVE GATE (before touching anything)

Download/read the live `plugins/ExcelProtect-0.1.1.jar`: it must be
**122,081 B and hash `b24c0ef2…`** (or be otherwise proven to be the 0.1.1
install of record). Any other state = STOP and reconcile first.

## Steps

1. **BACKUP-FIRST (cap-at-6):** fresh Shockbyte backup first. Before creating
   it, prune the OLDEST manual backups until the post-creation total is ≤6;
   never delete the Daily Backup's newest copy or the two newest manual
   restore points; name prunees in the install report.
2. **Gate check:** server verified empty (list = 0 twice) or weekend countdown
   protocol. Marker discipline applies in full (create
   `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window start,
   delete at finish — hand windows only; the failsafe uses its own).
3. **Swap:** rename the live jar aside:
   `plugins/ExcelProtect-0.1.1.jar` → `plugins/ExcelProtect-0.1.1.jar.held-<date>-pre012`.
   Place the candidate as `plugins/ExcelProtect-0.1.2.jar`.
   **Exactly one active ExcelProtect jar afterwards.**
4. **Config: DO NOT replace the live config.** 0.1.2 ships three new keys
   (`log.combat.damage-incidents`, `combat.damage-incident-gap-ms`,
   `combat.near-death-health-fraction`) and every one defaults safely when
   absent (incidents ON, 2000 ms, 0.30) — the live file works untouched.
   Optional, same window: append the three keys (with their shipped comments)
   to the live `config.yml` for operator visibility, keeping an in-place
   `.bak-<date>-pre012` of the config first. Never rewrite a live value.
5. Start the server; run the boot proof below.
6. On success: write `INSTALLED.txt` into the build folder (date, live jar
   hash after swap if computable — panel file manager cannot hash, so size +
   single-jar gate — boot lines quoted, backup name, prunees) and flip the
   MANIFEST entry to INSTALLED with the same facts.

## Boot proof (all required)

- Startup completes, server ONLINE, **SEVERE 0**.
- `[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked`
  (the new 0.1.2 awake line; `elitemobs=hooked` expected live — EliteMobs
  10.9.12 is installed. `absent`/`unavailable` would still be a pass for the
  plugin but must be reported, not waved through.)
- `[ExcelProtect] READY version=0.1.2` with the usual adapter/toggles tail.
- `excelprotect status` → `version=0.1.2`, `dropped-total=0`, and the shed
  order now includes `damage` after `combat`.
- Plugin count unchanged (**79**), guild LOCKED 3 of 5, TPS ~20.

## Post-install proof (player-side, owed after install)

A volunteer takes a short beating (or a lava dip): console
`excelprotect damage <player>` shows the incident — source (+ elite level if
an EM elite did it), total damage, ticks, health drop, NEAR DEATH/DIED marks
as applicable. A new `plugins/ExcelProtect/damage-<date>.jsonl` exists and its
record parses as the spec'd `damage_incident` shape.

## Rollback

Stop the server, remove `ExcelProtect-0.1.2.jar`, restore
`ExcelProtect-0.1.1.jar.held-<date>-pre012` to `plugins/ExcelProtect-0.1.1.jar`,
restore the config backup if step 4's optional append was taken, start.
0.1.2's only new on-disk state is `damage-*.jsonl` lines, which 0.1.1 simply
does not read (its housekeeping still rotates them — stream-agnostic) — no
data migration either direction.
