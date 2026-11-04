# INSTALL PACKET — Spell logic-fix pass (spells.yml balance retune)

Applies the logic-test fixes Mike approved Nov 2, 2026 ("do what you think
is best, go for all") to the findings in
`~/workspace/spell-audit/LOGIC-TESTS-20261102.md`. Full per-spell record,
tier math, and jar evidence: `install-20261102/FIXES.md`.

Config-only pass: four staged spells.yml files, five destinations (the s3
file serves BOTH the live file and the stage-3 template — the same layout
as the Arrow, Fling, particle, and repair passes). No jar changes, no other
config changes.

## What changes (summary — FIXES.md is the record)

- **wolf:** mana 50 → 100, cooldown 5000 → 60000 (`max_wolves: 5` stays).
  Pacing fix only — the relog-orphan loophole is class behaviour and is
  NOT closed by this pass (FIXES.md, honesty note).
- **familiar:** mana 20 → 50, cooldown 30000 → 300000 (SpawnEntityAction
  has no cap parameter — jar-proven). **cat:** cooldown 60000 → 300000.
- **P1 damage retune:** arrow ×6 mana 40; missile ×4 mana 20; missile|5
  mana 30 + cooldown 850 → 1000; wrv_spark 20/23/26; dash mana 100; stream
  mana 100 (base; tiers inherit); whip mana 40; sweep mana 100 and
  masterattack mana 16 (stage-4/5 files only).
- **stormshackle upkeep (P3):** active_costs mana 2/1.5/1/0.5 →
  6/18/30/42 per second. Activation mana, damage, slowness, range,
  targeting, and cooldowns untouched; no `disable_mana_regeneration`
  anywhere (grep count 0 in all four staged files).
- NOT touched: `repairitem` / `repairequipment` (0.5.2 behaviour rides
  these same files and is byte-identical to base), every `pvp_restricted`
  value (check-7 is a separate policy decision), Fling (Mike's Nov 1
  retune stands).

## Staged files

| staged file | sha256 | bytes |
|---|---|---|
| `install-20261102/stage-s2/spells.yml` | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` | 86,716 |
| `install-20261102/stage-s3/spells.yml` | `66986716c0bd69325d480f3613e11fead7f8d55459bb2116ac55d0db86631f31` | 140,362 |
| `install-20261102/stage-s4/spells.yml` | `a019fa6c447b99d2ecf991a55ab06f872150f3aecda364fc87c79ddacad53daa` | 178,603 |
| `install-20261102/stage-s5/spells.yml` | `6b317053ee78f9365ba055af3d314c4621ff4168a961c6b57dea601f32d5842b` | 178,601 |

Harness-verified before filing (Mike's Nov 2 gate — a spells.yml change
does not install until the logic harness has run on it): before
13/22/24/24 HIGH → after 6/7/8/8 HIGH, zero CRITICAL; every remaining HIGH
is individually explained in FIXES.md (the pool-60 band cases and Shackle's
activation-model rows). Bases verified against the Window #2 hashes before
editing — see FIXES.md.

## Gates (per destination — any mismatch = STOP for that destination)

The current file at each destination MUST hash to its Window #2 final
(the ExcelTogether 0.5.2 staging values) before it is replaced:

| destination | staged file | current file MUST hash |
|---|---|---|
| `plugins/Magic/spells.yml` (LIVE) | stage-s3 | `29dfe0dc2d256908a37931bbdb491becdcbd005818772e716e2318aafdf07f9c` |
| `plugins/ExcelMagicGate/stages/stage3/spells.yml` | stage-s3 | `29dfe0dc2d256908a37931bbdb491becdcbd005818772e716e2318aafdf07f9c` |
| `plugins/ExcelMagicGate/stages/stage2/spells.yml` | stage-s2 | `91240009bf43e6a630382435465aa0aaab244a147274b5f41b70889ab18966a1` |
| `plugins/ExcelMagicGate/stages/stage4/spells.yml` | stage-s4 | `4594b8823912160f36482821aee8a9978d3ba79943df1e323797f268bb09805f` |
| `plugins/ExcelMagicGate/stages/stage5/spells.yml` | stage-s5 | `fa31fa208f34b23a1bb0740d03ff139e0b4cc01539e3236570de00f6fc48b741` |

## Steps

1. For each of the five destinations: verify the gate hash, back up the
   current file in place as `spells.yml.bak-20261102-prelogicfix`, place
   the staged file, verify the placed hash against the staged table.
2. **Restart the server** (panel Shut Down → Start; a full restart, not a
   `/magic reload`, so the boot-verify below runs against the exact lines
   of record). Do not leave destinations split across windows — a stage
   flip with a stale template would re-apply the old numbers.
3. Boot-verify from the fresh log:
   - Done line present; TPS 20.0.
   - Magic loads with **"Loaded 167 spells"** and no spell-load errors;
     SEVERE 0 attributable (the lone pre-existing Essentials
     unsupported-version ERROR is not attributable).
   - GuildGate **LOCKED with seal count UNCHANGED (3 of 5)** from the
     pre-window read.
   - Together's awake line still shows the 0.5.2 repair registration +
     confirm gate (proves the repair spells survived the swap in-file).
4. Write the marker `install-20261102/INSTALLED.txt` with the window
   record (placed hashes, backup names, boot lines) and flip this entry
   in the MANIFEST to INSTALLED.

## Rollback

Restore each destination's `spells.yml.bak-20261102-prelogicfix` and
restart. All values return to the Window #2 state (including the 0.5.2
repair spells, which this pass never altered). Nothing else was changed,
so nothing else needs rolling back.

## Player proofs owed (the acceptance that counts)

1. **Mike — Shackle bar visibly drains:** toggle Storm Shackle on a target
   and watch the mana bar: at rank 1 it should now visibly fall (net
   1 mana/s at the 60 pool, faster at rank) and the channel should end on
   its own when the pool empties (~30–60 s from full depending on rank)
   instead of running forever.
2. **Jester — wolf pacing feel:** Wolf should feel like preparation now —
   100 mana and a 1-minute cooldown per wolf — not a mid-fight button.
   Report whether the pack still builds at a satisfying prep pace.
3. (Either) **Missile/Arrow feel:** both still fire briskly in bursts but
   holding the button should empty the bar in ~20 s at the 100 pool.

## Same-pass surfaces

No new spells, commands, or menus — nothing to add. If the website or
Guide spell text quotes mana/cooldown numbers for any retuned spell, those
numbers are stale after this lands; that refresh belongs to the already
owed website-edit pass, not to this packet's placements.
