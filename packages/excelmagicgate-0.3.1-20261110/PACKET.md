# ExcelMagicGate 0.3.1 — Install Packet

For the installer (supervisor / failsafe / parent hand window).
The build itself did NOT install anything. This is a jar-only
swap: 0.3.1 changes only the wand-lore block text (the milestone
lines); config, earned.yml, stage packets and every other file
stay byte-untouched.

## Payload

| Item | Value |
|---|---|
| Jar | `ExcelMagicGate-0.3.1.jar` |
| Size | 243885 B |
| SHA-256 | `454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2` |
| Build ID | `EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311` |
| Replaces (live) | ExcelMagicGate 0.3.0 amended (build -02) |

## Gates before anything moves

1. **Live-jar gate.** The live plugin file is
   `ExcelMagicGate-0.2.0.jar` (the panel filename convention the
   -02 window installed under). Verify before swapping: size
   **243572 B**, SHA-256
   `4cab2689d073a2a4e7141a31c24fd1a05cd7011fdf846a2fd84193e5a1836384`
   (the amended -02 jar, from `build-0.3.0-amended/BUILD-RECORD.md`
   on disk; its INSTALLED.txt records the same identity placed
   live Nov 10, 4:34–5:23 AM CST). Any mismatch: STOP, the live
   tree changed since the build base.
2. **BACKUP FIRST + cap-at-6.** Fresh Shockbyte backup before the
   swap; prune oldest manual backups first so the post-creation
   total is <= 6 (never delete the Daily Backup's newest copy or
   the two newest manual restore points). The backup is the
   rollback plan.
3. **Emptiness gate.** Install only on a verified-empty server
   (list = 0 twice) or under the standing weekend protocol. This
   is a jar change, NOT a text live-edit.

## Swap

1. Stop / verified-empty. Hold the outgoing -02 jar aside under a
   held name (panel convention: `…held-<date>-pre031`).
2. Place the candidate UNDER THE LIVE FILENAME
   `plugins/ExcelMagicGate-0.2.0.jar`; panel checksum must read
   243885 B / `454186c0…` EXACT before Start.
3. **Do NOT touch** `plugins/ExcelMagicGate/config.yml` or
   `plugins/ExcelMagicGate/earned.yml` — 0.3.1 reads the same
   config and the same store format (both byte-identical by
   design); no placement re-run, no config merge. In-place `.bak`
   copies are fine, originals stay untouched.
4. ONE boot.

## Boot verification (all must hold)

- Done ~20 s; SEVERE 0 attributable.
- Enable line carries build `EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311`.
- `Earning: ON — <N> players tracked, placement done 2026-11-10
  (mage-pull-20261110), demotion ENABLED` (player count may have
  grown with accrual; the placement date/source must still read
  the Nov 10 record).
- `Wand inventory sync: ON` line present (carried behavior).
- `/magicgate status`: stage as before, ceiling as before,
  reconcile ON, demotions pending 0, lore seam resolves to
  "property (verified)" after the first wand refresh.
- **Lore spot-check (the point of this build):** any live wand's
  lore block now ends with the two milestone lines —
  `Milestones: Student 100 · Apprentice 400` and
  `· Master 2,000` — under the existing Rank / Credited casts /
  Next lines. (Rig-proven form, boot-031-3.log.)

## Player-facing behavior

The only visible change: wand lore gains the milestone numbers.
Ranks, credited counts, gaps, demotion, credit and the inventory
sync behave exactly as under -02. No heads-up is required beyond
the normal install note; no player data changes.

## Rollback

Restore the held -02 jar under the live filename and reboot
(config and earned.yml were never edited, so no data rollback
exists), or restore the pre-install Shockbyte backup.

## Records

Records package: `records/excelmagicgate-0.3.1-20261110/` in this
build folder (jar, sources, tests, stage packets, TEST-OUTPUT.txt,
FINGERPRINTS.md, RIG-VERDICT.md, this packet as PACKET.md, and a
PUBLICATION-NOTE.md). MANIFEST entry: staged as
MANIFEST-ENTRY-DRAFT.md in this build folder — the supervisor
files it as PENDING; this build filed nothing.
