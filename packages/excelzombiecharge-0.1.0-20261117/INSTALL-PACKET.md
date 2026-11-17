# ExcelZombieCharge 0.1.0 — INSTALL PACKET (rename swap)
Created Oct 22, 2026 by the nightly work pass. Source of truth: BUILD-RECORD.md in this folder.

## What
Identity-only rename swap: ExcelProvoke 0.1.0 (live since Oct 21, ~10:52 PM CDT) -> ExcelZombieCharge 0.1.0. Same mechanic: zombie family + piglins charge at +50% MOVEMENT_SPEED for 60 ticks, refreshed per qualifying ranged/Magic hit. New plugin also strips the legacy `excelprovoke:speed` modifier key. Admin command stays /provoke.

## Artifact
- ExcelZombieCharge-0.1.0.jar — 21,919 B — SHA-256 0a173132295e26d7012094958d4d306cd5f7ee2c62bc9332257ed4fcb1142084 (this folder; Drive-deposited, MD5-verified per BUILD-RECORD.md)
- Tests: 93 JVM checks green; rename rig proven (legacy strip, arrow spot proof 0.2300 -> 0.3450, /provoke status answers). Clean-room rebuild byte-identical.

## Gate (all must pass)
1. Quiet scope: reversible swap, rollback below written in advance.
2. Panel console `list` reads 0 players, twice, separated in time. Anyone online = HOLD, touch nothing.
3. No other panel actor active.

## Steps
1. Verify live plugins folder contains ExcelProvoke-0.1.0.jar.
2. Panel Shut Down (NEVER console `stop` — it auto-restarts).
3. Remove ExcelProvoke-0.1.0.jar -> keep as rollback (rename to ExcelProvoke-0.1.0.jar.bak-rename or move out of plugins/); remove the stale plugins/ExcelProvoke/ folder (holds only regenerated defaults).
4. Upload ExcelZombieCharge-0.1.0.jar into plugins/; verify size 21,919 B.
5. Panel Start.
6. Boot-verify: ExcelZombieCharge enable line ("charge ON, speed x1.50 (+0.5) for 60 ticks"); `guildgate status` = LOCKED 0 of 5; zero SEVERE in boot segment; `list` = 0.
7. Write marker ~/workspace/excelzombiecharge/INSTALLED.txt and move the manifest entry to INSTALLED.

## Rollback
Panel Shut Down; remove ExcelZombieCharge jar + plugins/ExcelZombieCharge/ folder; restore ExcelProvoke-0.1.0.jar (+ its folder if removed); Start; verify the ExcelProvoke enable line and guildgate LOCKED 0 of 5.

## After install (same pass)
- Website + Guide already document the charge AS ExcelZombieCharge (PR #75, Oct 22) — the swap makes the live name match the docs; no further docs change needed.
- Player proofs owed (one at a time, natural play): a ranged hit triggering the charge on Java and on Bedrock.
