# ExcelMagicGate 0.3.0 — Install Packet (Magic Rank earning)

For the installer (supervisor / parent hand window). The build itself did
NOT install anything. Read DESIGN.md §8 alongside this packet.

## Payload

| Item | Value |
|---|---|
| Jar | `ExcelMagicGate-0.3.0.jar` |
| Size | 237,422 B |
| SHA-256 | `515fe1c3194e56da734d5bdab257badef3e5074e1ac415ed11a375f100f88018` |
| Build ID | `EXC-MAGICGATE-0.3.0-WRV-20261110-01-5af7d1` |
| Replaces | ExcelMagicGate 0.2.1 |

## Gates before anything moves

1. **Live-jar gate.** The live plugin file is `ExcelMagicGate-0.2.0.jar`
   (the 0.2.1 build kept the 0.2.0 filename — template convention). Verify
   before swapping: size **192,747 B**, SHA-256
   `7110bfdc6f603ec987a7f8aaa0595e5dba11f883f909194cd6b65d00e38043fa2f`. Any
   mismatch: STOP, the live tree changed since the build base.
2. **BACKUP FIRST + cap-at-6.** Fresh Shockbyte backup before the swap;
   prune oldest manual backups first so the post-creation total is <= 6
   (never delete the Daily Backup's newest copy or the two newest manual
   restore points). The backup is the rollback plan.
3. **Emptiness gate.** Install only on a verified-empty server (list = 0
   twice) or under the standing weekend protocol. This is a jar+data change,
   NOT a text live-edit.

## Placement (DESIGN §8) — hard sequence, STOP gate at step 3

1. Take a FRESH §3f mage-data pull from the live server (all players'
   `plugins/Magic/data/players/*.dat`).
2. Run the placement against the fresh pull with the SHIPPED classification
   (config `earning:` block from this build). Regeneration command (from the
   records package, JDK 25):
   `javac` the test classes per `run-tests.sh`, then run
   `com.winkreyes.excelmagicgate.PlacementTool <fresh-pull-dir> <out-earned.yml> mage-pull-<date>`
   (the tool reads cast_count per spell key from each mage.dat, applies the
   shipped config's classification, and writes earned.yml in the exact store
   dialect).
3. **STOP GATE:** compare the tool's table against the locked placements —
   Jester Master (11,231 on the Nov 9 pull), Marvin Master (2,178), Mike
   Apprentice (1,210), Kailey below Student (12). Numbers should match the
   Nov 9 pull or differ ONLY by casts made since (growth, never a different
   rank for the four named players). Any rank disagreement: STOP, do not
   install, escalate — the classification or the pull is wrong.
4. Pre-write the verified earned.yml to
   `plugins/ExcelMagicGate/earned.yml` BEFORE the first 0.3.0 boot (the
   plugin loads it at enable and marks placement done from its header; a
   missing file means every player gets Mage-API fallback seeding instead —
   placement cast history would be lost).
5. Swap the jar (keep the live filename convention: install AS
   `ExcelMagicGate-0.2.0.jar` is NOT required — panel convention from 0.2.1
   kept the old name; either name works for Bukkit, but record the one used),
   keep the existing config.yml (the plugin merges the new `earning:`
   defaults; verify `earning.enabled: true` and
   `earning.demotion_enabled: true` in the merged config — demotion ships
   ENABLED because the seam is rig-proven, see BUILD-RECORD.md).
6. Boot checks: log shows build `EXC-MAGICGATE-0.3.0-WRV-20261110-01-5af7d1`,
   "Earning: ON — 11 players tracked, placement done <date>", "Credit hook
   registered", lore seam resolves to "property (verified)" after the first
   wand refresh. `/magicgate status` shows the Earning line.
7. First-join watch: Mike's own first join should reconcile him to
   Apprentice (pool 150) if his wand sits above it; Jester/Marvin stay
   Master. Announce in family chat BEFORE the window (demotions are the
   visible change): ranks now reflect credited casts; seals still set the
   ceiling.

## Player-facing behavior Mike should know (rig-proven, DESIGN §11)

- After placement, a wand above its owner's earned rank steps DOWN at the
  owner's next wand activation (spells are never removed; mana pool/regen
  and hotbar size follow the earned rank).
- Tier variants above the earned rank fail to cast until re-earned
  (Magic enforces the wand path at cast time — rig evidence in
  RIG-VERDICT.md). This is the one consequence worth a heads-up to Jester
  and Mike before the window.
- Rank progress is on the wand lore (Rank / Credited casts / Next).
- Renewal (heal-over-time) may occasionally not credit when its first tick
  lands late; instant heals credit normally.

## Rollback

1. Stop server (or verified-empty), restore the pre-install Shockbyte backup
   (step 2 above) — that restores the 0.2.1 jar, config, and Magic data.
2. If a file-level rollback is preferred: put back `ExcelMagicGate-0.2.0.jar`
   (192,747 B / `7110bfdc…`), remove `plugins/ExcelMagicGate/earned.yml`
   (0.2.1 ignores it, but remove for cleanliness), restore the pre-install
   config.yml if it was edited.
3. Note: any rank changes 0.3.0 made to wands live in Magic's mage data —
   only the full backup restore (step 1) rewinds those. File-level rollback
   leaves wands where 0.3.0 put them; 0.2.1's ceiling-floor will step
   under-ceiling wands back UP on its normal reconcile, but will not undo a
   demotion above the floor — prefer step 1.

## Records

Records package: `records-catchup-20261103/packages/excelmagicgate-0.3.0-20261110/`
(jar, sources, tests, stage packets, TEST-OUTPUT.txt, FINGERPRINTS.md,
RIG-VERDICT.md, this packet as PACKET.md). MANIFEST entry: staged as
MANIFEST-ENTRY-DRAFT.md in the build folder — the supervisor files it.
