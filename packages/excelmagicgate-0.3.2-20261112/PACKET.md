# ExcelMagicGate 0.3.2 — Install Packet

For the installer (supervisor / failsafe / parent hand window).
The build itself did NOT install anything. This is a jar-only
swap: 0.3.2 changes only the wand inventory sync's dead-key gate;
config, earned.yml, stage packets and every other file stay
byte-untouched.

## Payload

| Item | Value |
|---|---|
| Jar | `ExcelMagicGate-0.3.2.jar` |
| Size | 247078 B |
| SHA-256 | `895cf91dfd28bcca0c493dc2ef69fcc903257001ea1790bb70fcae3b0a81fb68` |
| Build ID | `EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b` |
| Replaces (live) | GATE-VERIFY — see gate 1 |

## Gates before anything moves

1. **Live-jar gate (GATE-VERIFY).** This build ran no live pulls
   and asserts nothing about the live tree. Before swapping, pull
   the live ExcelMagicGate plugin file fresh (PC route, or the
   panel fallback) and record its filename, size and SHA-256 as
   the packet's live-premise receipt. Expected predecessor: the
   0.3.1 jar (243885 B, SHA-256
   `454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2`)
   if the 0.3.1 packet installed as filed; otherwise the amended
   0.3.0 -02 jar. Any other identity: STOP, the live tree changed
   since the build base.
2. **BACKUP FIRST + cap-at-6.** Fresh Shockbyte backup before the
   swap; prune oldest manual backups first so the post-creation
   total is <= 6 (never delete the Daily Backup's newest copy or
   the two newest manual restore points). The backup is the
   rollback plan.
3. **Emptiness gate.** Install only on a verified-empty server
   (list = 0 twice) or under the standing weekend protocol. This
   is a jar change, NOT a text live-edit.

## Swap

1. Stop / verified-empty. Hold the outgoing jar aside under a
   held name (panel convention: `…held-<date>-pre032`).
2. Place the candidate UNDER THE LIVE FILENAME the predecessor
   runs under (from gate 1's receipt); panel checksum must read
   247078 B / `895cf91d…` EXACT before Start.
3. **Do NOT touch** `plugins/ExcelMagicGate/config.yml` or
   `plugins/ExcelMagicGate/earned.yml` — 0.3.2 reads the same
   config and the same store format (both byte-identical by
   design); no placement re-run, no config merge. In-place `.bak`
   copies are fine, originals stay untouched.
4. ONE boot.

## Boot verification (all must hold)

- Done ~20 s; SEVERE 0 attributable.
- Enable line carries build `EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b`.
- `Earning: ON — <N> players tracked, placement done 2026-11-10`
  (player count may have grown with accrual; the placement
  date/source must still read the Nov 10 record).
- `Wand inventory sync: ON` line present (carried behavior).
- `/magicgate status`: stage as before, ceiling as before,
  reconcile ON, demotions pending 0, lore seam resolves as before.
- **Gate spot-check (the point of this build):** the first sync
  for any player whose grant candidates include a key Magic has
  no template for logs, at WARN, `Wand inventory sync: spell
  '<key>' … has no template in Magic — skipped, wand data/config
  mismatch`, the sync completes, and its accounting counts the
  key as skipped, never granted. If NO such key exists on any
  wand (the expected case per the run-228 pull: all 80 of Mike's
  wand keys resolve), no WARNING appears and syncs read exactly
  as under 0.3.1 — the absence of the line is the healthy result,
  and the first future dead key will be named by it.

## Player-facing behavior

No visible change. Grants, ranks, credited counts, demotion,
credit and lore behave exactly as under 0.3.1; the only new
output is log-side (the dead-key WARNING + accounting) on a sync
that meets a dead grant candidate. No player data changes.

## Rollback

Restore the held predecessor jar under the live filename and
reboot. No data rollback is needed: 0.3.2 writes no new data
formats and changes no config.
