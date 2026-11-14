# BUILD RECORD — ExcelProtect 0.1.9 (P-PROTECT-I4-1, test T-I4)

Closed: 2026-11-14 (desk build by first worker; mirror rig leg completed on re-dispatch, run 340 worker).
Live server was never touched; all rig work ran on the Homebase test mirror
(`C:\Users\mikeexcel\wrv-testserver\server`).

## The defect (T-I4)

ExcelProtect 0.1.8's item-break handler (`ItemEvents.onItemBreak`) read
`Damageable.getMaxDamage()` guarded only by `hasDamageValue()`. Paper throws
`IllegalStateException` ("We don't have max_damage! Check hasMaxDamage first!")
when an item's meta carries a damage value but no `max_damage` component —
exactly what a meta round-trip (e.g. PDC stamping) produces. The handler threw
and the break was recorded nowhere.

## The fix (0.1.9)

`ItemEvents.maxDurabilityOf(Damageable, typeMax)` guards the meta read with
`hasMaxDamage()` and a try/catch; when the component is absent (or the read
still refuses) it falls back to the material's max durability, and to 0 (the
existing non-damageable "unknown" convention) when the material has none
either. The break is always recorded, never thrown away. Pure core
`resolveMaxDurability(hasMaxDamage, metaMaxDamage, typeMaxDurability)` is
unit-pinned without a server.

## Base gate (work/base-gate-build.log)

- 0.1.8 sources rebuilt byte-identical to the live-premise jar:
  `ExcelProtect-0.1.8.jar` SHA-256
  `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a`
  (fingerprint `EXC-PROTECT-0.1.8-WRV-20261108-01-a018d8`).
- Base suite on 0.1.8 sources: 97 found / 97 successful / 0 failed.

## Candidate (work/build-019-a.log, work/build-019-b.log)

- `ExcelProtect-0.1.9.jar` — 269,208 B, SHA-256
  `893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626`
  (fingerprint `EXC-PROTECT-0.1.9-WRV-20261114-01-0af02b`).
- Two independent builds (A and B) produced the identical hash — deterministic.
- Suite on 0.1.9: 99 found / 99 successful / 0 failed (both builds).

## Discrimination (work/discrim-018.log)

The 0.1.9 test suite run against the 0.1.8 sources: 99 found / 97 successful /
2 failed — the two new T-I4 regression tests
(`itemBreakWithDamageButNoMaxDamageComponent`,
`itemBreakMaxDurabilityResolutionMatrix`) fail on 0.1.8 with
"ItemEvents.maxDurabilityOf is absent — on 0.1.8 the handler reads
getMaxDamage() guarded only by hasDamageValue() and throws
IllegalStateException…", and pass on 0.1.9. The tests pin the defect.

## Mirror rig leg (T-I4) — completed on re-dispatch

First worker staged everything but its boot was interrupted before launch:
candidate 0.1.9 placed in the mirror (`plugins\ExcelProtect-0.1.9.jar`,
hash-verified in place `89389288…`), 0.1.8 held aside as
`ExcelProtect-0.1.8.jar.held-i4-20261114`, full pre-rig snapshot taken
(`snapshot-i4-20261114`), ProtectRig driver built (mode `probe`).
Re-dispatch verified the boot had NEVER run: no `boot-i4.log`, no java
process, `items-2026-11-14.jsonl` untouched since the 08:28 boot.

This run booted the mirror exactly ONCE via a one-shot scheduled task
(`ClaudeI4Boot`, deleted after use; LastTaskResult 0): JDK 25
(Temurin 25.0.5), `-Xms512M -Xmx2G -jar paper-26.3-245.jar nogui` from the
server folder, output to `boot-i4.log`. Boot 2026-11-14 11:02:56–11:03:53
CST; server `Done (20.161s)`; rig printed `RIGOBS DONE mode=probe` and the
server self-shut-down.

### T-I4 verification (evidence in rig-evidence/)

- READY line: `[ExcelProtect] READY version=0.1.9 dir=plugins\ExcelProtect
  gp=on vault=on mcmmo=on we=on retention-days=30`
- `item_break` records in `items-2026-11-14.jsonl`: **2** (file grew
  2,359 B / 13 lines pre-rig → 3,317 B / 15 lines post-rig):
  1. Synthetic T-I4 probe break — RigAlpha `minecraft:wooden_pickaxe`,
     damage 58, `max_durability` 59 (the material fallback: the item's meta
     carried damage with no `max_damage` component — the exact 0.1.8 crash
     shape, now recorded).
  2. Wear probe break — RigAlpha `minecraft:leather_helmet`, damage 55,
     `max_durability` 55.
- `"We don't have max_damage"` exception lines in boot log: **0**
- `SEVERE` lines in boot log: **0**
- Evidence files: `rig-evidence/boot-i4.log` (105,289 B, SHA-256
  `537d598a165c85ad9e0b76a8068ca1a4b13b7286276c2b92bd9c783b54165992`),
  `rig-evidence/items-2026-11-14.jsonl` (3,317 B, SHA-256
  `acbe9014576c546bb00126d3fcb7087bcff0595f47b78675b3a88e5d3ea0a9e2`).

**T-I4 verdict: PASS.**

## Mirror restoration

Mirror restored from `snapshot-i4-20261114` via `robocopy /MIR` with the
server stopped (robocopy exit 3 = copied + extras purged, success range):

- Snapshot verified before restore: 11,218 files / 3,525,741,824 B.
- Restored mirror: 11,218 files / 3,525,741,824 B.
- Protect jar back to 0.1.8: 268,894 B, SHA-256
  `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a`.
- Config back to: 7,991 B, SHA-256
  `7a423378a29b283b6715557b97cb29bb08015ef9f0d10dc2ec909060a348d605`.
- 0.1.9 jar, held 0.1.8 jar, ProtectRig.jar and ProtectRig data dir purged
  by the mirror restore (verified absent).

PC cleanup (per PC cleanup rule): snapshot directory, ProtectRig i4
sources/classes, the i4 build/stage/snapshot/inspect scripts, `boot-i4.ps1`
and the PC copy of `boot-i4.log`, and all one-shot helper scripts deleted;
scheduled task `ClaudeI4Boot` unregistered; final sweep shows no `*i4*`
residue in `wrv-testserver`, no java process, mirror intact at
11,218 files / 3,525,741,824 B. Desk-side evidence kept in `rig-evidence/`.
