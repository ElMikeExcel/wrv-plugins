# INSTALL PACKET — ExcelEMFixes 0.23.0 (wither-skulls module; ExcelEMWitherSkulls fold, Wave 2)

Build: ExcelEMFixes 0.23.0 (Wink Reyes Valley), built Nov 25, 2026.
Ruling: D245 (Mike, Nov 25, 2026) — consolidation programme Wave 2:
ExcelEMWitherSkulls 0.1.0 folds into ExcelEMFixes as the toggleable
`wither-skulls` module; the standalone jar retires in the SAME
window, only after module proof (done — see RECEIPTS.md: unit
suite 1,972/0 clean run, rig Phase A 100% leg 40/40 skulls + 0%
leg inert, Phase B toggle-off vanilla, SEVERE 0 both phases).
Lint: 5106 B / max line 95 / non-ASCII 19 (P3 targets met).
RECEIPTS.md: `~/workspace/excelemfixes-witherskulls-20261125/packet-0.23.0/RECEIPTS.md`
— sha256 `02191aa2fc25ce076137229e238f81be0956b344288c4ede246bf4b77c1a7d03`.

## What changes

- ExcelEMFixes 0.22.0 → 0.23.0: adds ONE module, `wither-skulls`
  (25 modules total). Player-facing behavior is IDENTICAL to the
  live ExcelEMWitherSkulls 0.1.0: the `/witherskulls` command
  name + alias `/wskulls`, its status|reload shape, its
  `excelwitherskulls.admin` permission node, and the skull top-up
  at the live configured target.
- The standalone `ExcelEMWitherSkulls-0.1.0.jar` is REMOVED in
  this window (kept in the backup set as rollback).
- No data migration: the module keeps no state and writes no
  files. Config values only — the live standalone values
  (enabled true, target 25.0, debug-log false) were diffed fresh
  at the gate and equal the shipped defaults (RECEIPTS.md).

## Live-premise gate (verify BEFORE any step; abort on mismatch)

- `plugins/ExcelEMFixes-0.22.0.jar` — 596,100 B — sha256
  `49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04`
- Live ExcelEMFixes `config.yml` — 84,251 B — sha256
  `e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086`
- `plugins/ExcelEMWitherSkulls-0.1.0.jar` — 14,862 B — sha256
  `9361dd79f9b0dd639d2b0256bfb866f57d3160ebe1e850e718d01a80e6abf8ed`
- Live `plugins/ExcelWitherSkulls/config.yml` — 1,507 B — sha256
  `a0c5a868fe05d38555fd8b1467b053193fcf82d525f15b1c1ec3a7c81a10da46`,
  values enabled true / target-chance-percent 25.0 / debug-log false.
- Server verified empty (or the weekend push protocol, if Mike directs).

## Sequence (in order; do not reorder)

1. **Backup.** Snapshot/backup per the standing window procedure,
   PLUS file copies of: the live ExcelEMFixes jar + config.yml and
   the ExcelEMWitherSkulls jar + its config.yml. Cap-at-6 house
   rule applies to manual backups (name prunees; never the Daily
   newest or the two newest manuals). Keep the backup until the
   window closes clean — the standalone jar in it is the rollback.
2. **Jar swap (server stopped).** Remove
   `ExcelEMWitherSkulls-0.1.0.jar` from `plugins/`. Replace the
   ExcelEMFixes jar with `ExcelEMFixes-0.23.0.jar` (609,952 B,
   sha256 `48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b`).
   Replace ExcelEMFixes `config.yml` with this packet's
   `deploy-config.yml` (87,117 B, sha256
   `daa7b8bde577447fc417523cfe416f8b7d65074acef3cf40081e0046600cd102`
   — the live config plus ONLY the `modules.wither-skulls`
   section; pure addition, verified by diff: +62 / −0 lines).
   The standalone jar must be out BEFORE the boot: both jars
   listen to the same death event — never let both be present at
   a boot (double top-up risk).
   Leave `plugins/ExcelWitherSkulls/` (its config.yml) in place;
   it becomes inert history once its jar is gone.
3. **Boot verify.** Boot the server. Required evidence:
   - Boot SEVERE count 0.
   - ExcelEMFixes reports 25 modules enabled, `wither-skulls`
     among them.
   - The wither-skulls ready line reads: "Wither Skulls ready:
     top-up ON, target skull chance 25.0% (realized 24.44% with
     no Looting), EliteMobs seam BOUND".
   - `/witherskulls status` (console or op) reports top-up ON,
     target 25.0%, seam BOUND.
4. **Close-out.** Record the window (hashes, boot lines, SEVERE
   count) in the install record; run the surfaces pass in
   SURFACES.md. Proof owed after install: one elite wither
   skeleton kill during natural play (debug-log may be enabled
   briefly to read the verdict line) — Iris-run, per the standing
   testing split.

## Rollback

1. Stop the server. Remove `ExcelEMFixes-0.23.0.jar`; restore the
   backed-up 0.22.0 jar + live config.yml.
2. Restore `ExcelEMWitherSkulls-0.1.0.jar` into `plugins/` from
   the backup set.
3. Boot; verify the standalone's enable line reports the seam
   BOUND and target 25.0%. No state exists to restore — the
   module wrote nothing.

## Gates / discipline

- Marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6; emptiness `list` = 0/10 twice.
- Verification by script output: gate hashes via sha256sum of the
  live files; boot evidence via grep of the boot log for the
  ready line above + SEVERE count; module count via the
  "ExcelEMFixes 0.23.0 ready" line's module list.
- D246 statement: shipped defaults vs WRV values + hardcoding
  sweep + gate-7 dependency statement are in RECEIPTS.md.
- Marker on install:
  `~/workspace/excelemfixes-witherskulls-20261125/packet-0.23.0/INSTALLED.txt`
