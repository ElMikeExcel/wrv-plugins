# INSTALL PACKET — ExcelProtect 0.1.7 "Parity" (DeathRecord importer fix + Telemetry kill-field parity)

**What:** swap the live ExcelProtect 0.1.6 jar for 0.1.7. 0.1.7 carries
exactly two decided features (Mike, Nov 6, 2026) and nothing else:
(1) the DeathRecord importer fix — `/excelprotect import deathrecord`
now honors DeathRecord's restored marks (already-restored burns import
pre-marked in Protect's restore marks and are never re-offered by Lost
& Found; already-restored death items are excluded from imported
snapshots; xp-restored deaths import without grantable XP) and
migrates DeathRecord's per-player Lost & Found state (free-used flag,
paid recovery counts) by delta, idempotently; (2) Telemetry kill-field
parity on `entity_death` — elite/elite_level from the damage-time
cached EliteMobs probe, killer weapon (main hand at death time),
origin taxonomy (`origin_class`/`rg_denied` from RewardGuard's PDC
marks), fall/explosion damage shares, `fall_softened`, and the elite
explosion integrity flag. Everything 0.1.6 does is carried unchanged;
all record changes are additive fields. **Config: do not touch it** —
0.1.7 adds no keys; the live config stays exactly as 0.1.6 left it.

**ORDERING — this install lands AFTER ExcelProtect 0.1.6.** The live
jar gate below requires the 0.1.6 jar of record; if the live jar is
still 0.1.5 (or anything else), STOP — install 0.1.6 first under its
own packet (`~/workspace/protect-recovery-build/INSTALL-PACKET.md`).

**After this lands, the DeathRecord import runs clean** — as its own
console packet, not inside this install window: re-run the census
first (DeathRecord is still live and writing; the Nov 6 census counts
will have moved), then follow
`~/workspace/protect-rollout-item13a/IMPORTER-PACKET.md` with the
0.1.7 semantics: `/excelprotect import deathrecord plugins/ExcelDeathRecord`
(the folder argument is part of idempotency — always exactly that
path), expecting the reply's restored-marked counts to reconcile with
the fresh census (R-burns) and the migrated state to match LF-state.
Only after that import verifies does DeathRecord retire. Telemetry's
retirement proceeds on its own gates (13b: G1 report-tool repoint,
G3 one-day Protect-vs-Telemetry field diff — the mapping table in
BUILD-RECORD.md is the comparison reference).

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
- **LIVE GATE:** live `plugins/ExcelProtect-0.1.6.jar` MUST be
  234,451 B / SHA-256
  `097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`
  or STOP (never skip a version).

## Swap

1. Rename the live jar aside:
   `ExcelProtect-0.1.6.jar` → `ExcelProtect-0.1.6.jar.bak-20261106-pre017`
   (adjust the date to the window day; never delete).
2. Place `~/workspace/protect-elites-build/ExcelProtect-0.1.7.jar`
   (246,038 B, SHA-256
   `d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124`)
   as `plugins/ExcelProtect-0.1.7.jar`; verify the placed file's hash
   matches exactly. Exactly one active ExcelProtect jar afterwards.
3. Config: untouched. Data folder: untouched (0.1.7 adds one small
   state file at first import — `imported-lf-state.json` — created by
   the importer, not by the install).
4. Full Shut Down → Start (not a reload).

## Boot verify

- `READY version=0.1.7` line present.
- New lines present: `[ExcelProtect] importer: deathrecord honors
  restored marks + lost&found state migration (0.1.7)` and
  `[ExcelProtect] kill parity: elite/weapon/origin-class/damage-shares
  on entity_death (0.1.7)`.
- Carried lines intact: recovery line, worldedit logging ON (if WE
  present), guildgate LOCKED 3 of 5, plugin count 77, TPS 20.0×3.
- SEVERE 0 attributable to ExcelProtect.
- Smoke: `/excelprotect status` reports version=0.1.7; do NOT run the
  import in this window (it is the follow-on packet above).
- Write the marker `~/workspace/protect-elites-build/INSTALLED.txt`
  with window times, backup identity, gate + placed hashes, and the
  boot lines.

## Rollback

Restore `ExcelProtect-0.1.6.jar.bak-20261106-pre017` to the live name
(remove the 0.1.7 jar to a `.held` name), full Shut Down → Start,
repeat boot checks against 0.1.6's packet. 0.1.7 writes no data that
0.1.6 cannot read (all stream additions are additive fields;
`imported-lf-state.json` is simply ignored by 0.1.6 — but note a
rollback AFTER the DeathRecord import has run would leave 0.1.6's
importer able to re-import without the restored-mark protection:
if the import has already run, do not roll back without telling Mike).

## Proofs owed after install

- The DeathRecord import packet (above) — the real 77-death /
  ~2,197-burn import with fresh-census reconciliation.
- A natural elite kill in family play whose `entity_death` line
  carries `elite=true` + `elite_level` (the rig proved the mechanism
  against real EliteMobs 10.9.12; the in-play sighting closes it).
- The G3 one-day field diff window (Protect vs Telemetry kill lines)
  per the 13b gates, before Telemetry retires.
