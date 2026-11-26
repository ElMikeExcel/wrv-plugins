# INSTALL PACKET — ExcelEMFixes v24 (party-drops module; ExcelEMPartyDrops fold, Wave 3)

Build: ExcelEMFixes v24 (Wink Reyes Valley), built Nov 25, 2026
(built as 0.24.0, renamed per D247 — simple v-numbers fleet-wide).
Ruling: D245 (Mike, Nov 25, 2026) — consolidation programme
Wave 3: ExcelEMPartyDrops 0.1.0 folds into ExcelEMFixes as the
toggleable `party-drops` module; the standalone jar retires in
the SAME window, only after module proof (done — see
RECEIPTS.md: unit suite 2,075/0 clean run, rig Phase A solo
32 -> 32 exact vanilla + party 22 -> 34 at 1.5x with all 16
audit lines "participants=2 multiplier=1.5", Phase B
toggle-off party 35 -> 35 exact vanilla, SEVERE 0 both phases).
Lint: 5983 B / max line 88 / non-ASCII 18 (P3 targets met).
RECEIPTS.md: `~/workspace/excelemfixes-partydrops-20261125/packet-v24/RECEIPTS.md`
— sha256 `98afec10fc0f1b0febf2aedf60a9c79bd91e87f40a416401f7d7f286a260f146`.

## FILED (G1 refresh done Nov 25, 2026, 10:15 PM CT — see RECEIPTS.md filing refresh)

Filed into the MANIFEST as a PENDING pointer stub after the
ExcelEMFixes 0.23.0 install closed (failsafe 20261125-2146,
supervisor run 962 close-out). Filing re-pull Nov 25, 2026,
10:15 PM CT CONFIRMED the gate below: live
ExcelEMFixes-0.23.0.jar 609,952 B /
48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b
and live config 87,117 B / daa7b8bd... byte-identical to the
staging pull, so deploy-config.yml stands (no regeneration).

## What changes

- ExcelEMFixes 0.23.0 → v24: adds ONE module, `party-drops`
  (26 modules total). Player-facing behavior is IDENTICAL to
  the live ExcelEMPartyDrops 0.1.0: the `/partydrops` command
  name, its status|reload shape, its excelpartydrops.admin
  permission node, and the drop multiplier at the live
  configured values (all equal the shipped defaults).
- The standalone `ExcelEMPartyDrops-0.1.0.jar` is REMOVED in
  this window (kept in the backup set as rollback).
- No data migration: the module keeps no state and writes no
  files. Config values only — the live standalone values were
  diffed fresh at the gate and equal the shipped defaults
  (RECEIPTS.md).

## Live-premise gate (verify BEFORE any step; abort on mismatch)

- Live ExcelEMFixes jar + config: RE-PULL AT FILING (see above).
- `plugins/ExcelEMPartyDrops-0.1.0.jar` — 14,543 B — sha256
  `12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df`
  (receipt Nov 25, 2026 ~9:51 PM CT, RECEIPTS.md; re-verify at
  the window).
- Live `plugins/ExcelEMPartyDrops/config.yml` — 2,117 B —
  sha256 `7f64d4b4ac8d16594f310bbe8c054c492229808e442d47eae20e30863b30dc31`,
  values enabled true / party-radius 50.0 / step-per-extra 0.5 /
  multiplier-cap 3.0 / participant-cap 5 /
  respect-farming-cap false / hint-cooldown-seconds 600 /
  debug false.
- Server verified empty (or the weekend push protocol, if
  Mike directs).

## Sequence (in order; do not reorder)

1. **Backup.** Snapshot/backup per the standing window
   procedure, PLUS file copies of: the live ExcelEMFixes jar +
   config.yml and the ExcelEMPartyDrops jar + its config.yml.
   Cap-at-6 house rule applies to manual backups (name
   prunees; never the Daily newest or the two newest manuals).
   Keep the backup until the window closes clean — the
   standalone jar in it is the rollback.
2. **Jar swap (server stopped).** Remove
   `ExcelEMPartyDrops-0.1.0.jar` from `plugins/`. Replace the
   ExcelEMFixes jar with `ExcelEMFixes-v24.jar` (626,284 B,
   sha256 `372c41031c8336a78d1113a1e9778c0fbcadd3afbb15c1840e16c56b0e042df9`).
   Replace ExcelEMFixes `config.yml` with this packet's
   `deploy-config.yml` (91,671 B, sha256
   `5a4a9468808a540409f1fee66ced8aab78c36acebeae5d25abd009ad59b63a01`
   — the live config plus ONLY the `modules.party-drops`
   section; pure addition, verified by diff: +105 / −0 lines;
   REGENERATE from the filing-time live pull if that pull
   differs from the staging pull). The standalone jar must be
   out BEFORE the boot: both jars listen to the same death
   event — never let both be present at a boot (double
   multiplier risk). Leave `plugins/ExcelEMPartyDrops/` (its
   config.yml) in place; it becomes inert history once its
   jar is gone.
3. **Boot verify.** Boot the server. Required evidence:
   - Boot SEVERE count 0.
   - ExcelEMFixes reports 26 modules enabled, `party-drops`
     among them.
   - The party-drops ready line reads: "Party Drops ready:
     ON, radius 50.0 blocks, step 0.5, multiplier cap 3.0,
     participant cap 5, EliteMobs hooked".
   - `/partydrops status` (console or op) reports ON, radius
     50.0, step 0.5, cap 3.0, participant cap 5, EliteMobs
     hooked.
4. **Close-out.** Record the window (hashes, boot lines,
   SEVERE count) in the install record; run the surfaces pass
   in SURFACES.md. Proof owed after install: one natural
   elite kill with a second player nearby during natural
   play, read from the audit line (participants=2,
   multiplier=1.5) — Iris-run, per the standing testing
   split.

## Rollback

1. Stop the server. Remove `ExcelEMFixes-v24.jar`; restore
   the backed-up 0.23.0 jar + live config.yml.
2. Restore `ExcelEMPartyDrops-0.1.0.jar` into `plugins/`
   from the backup set.
3. Boot; verify the standalone's enable line reports ON,
   radius 50.0, EliteMobs hooked. No state exists to
   restore — the module wrote nothing.

## Gates / discipline

- Marker discipline with re-checks before Shut Down and
  before Start; BACKUP FIRST + cap-at-6; emptiness
  `list` = 0/10 twice.
- Verification by script output: gate hashes via sha256sum
  of the live files; boot evidence via grep of the boot log
  for the ready line above + SEVERE count; module count via
  the "ExcelEMFixes v24 ready" line's module list.
- D246 statement: shipped defaults vs WRV values +
  hardcoding sweep + gate-7 dependency statement are in
  RECEIPTS.md.
- Marker on install:
  `~/workspace/excelemfixes-partydrops-20261125/packet-v24/INSTALLED.txt`
