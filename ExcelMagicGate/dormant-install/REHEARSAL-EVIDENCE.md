# Dormant install — scratch rehearsal evidence (Sat Oct 17, 2026, ~7:21 PM CDT)

Full boot log: `boot1-dormant.log` (this folder). Scratch stage:
`~/workspace/magic-scratch/stage-dormant/` (Paper 26.2-123, same build as
live; port moved to 25597 only because a parallel scratch job held 25599).

## Rehearsal setup (fresh, mirroring live)

- Fresh plugins folder containing: LuckPerms-Bukkit-5.5.87.jar (live
  version; data folder copied with libs cache but a FRESH storage DB, so
  no test grants leaked in), ExcelGuildGate-0.1.0.jar with live-shaped
  data (state.yml: 0 of 5 seals, LOCKED), and the two jars copied from
  THIS packet's `staging/` folder (not from their build folders).
- Runbook Step 1 pre-seed performed exactly as written (extract
  `stages/stage0` from the staged ExcelMagicGate jar, copy the six .yml
  files into `plugins/Magic/`). Seed `wands.yml` verified byte-identical
  to the packet's stage-0 file (SHA256 5f726814…).
- Boot via the magic-scratch FIFO harness; probes fed from console;
  server stopped; scratch left STOPPED (0 java processes after).

## Probe results (verbatim from boot1-dormant.log)

Boot / enable lines:

```
[Magic] Enabling Magic v11.2.5-baa3d91
[Magic] Loaded 6 wands
[ExcelGuildGate] Enabling ExcelGuildGate v0.1.0
[ExcelMagicGate] Enabling ExcelMagicGate v0.1.0
[ExcelMagicGate] ExcelMagicGate enabled: applied stage 0 of 5, watching ExcelGuildGate seals every 60s
[Magic] Loaded 30 spells
[Magic] Loaded 5 progression paths
```

(`Loaded 6 wands` prints during Magic's enable; `Loaded 30 spells` /
`Loaded 5 progression paths` print a few seconds later. All three
present. Zero SEVERE lines in the whole boot; no stormshackle canary;
no vengeance warnings — the packet config does not load that spell.)

`magicgate status`:

```
Master switch: ON | Seal source: ExcelGuildGate API | Seals fallen: 0 of 5
Applied Magic stage: 0 of 5
 Stage 0 Dormant (pre-Seal-1) - APPLIED
 Stage 1 The Healer - locked (needs 1 seals)
 Stage 2 The Pathfinder - locked (needs 2 seals)
 Stage 3 The Wonder - locked (needs 3 seals)
 Stage 4 The Masons - locked (needs 4 seals)
 Stage 5 The Guild Opens, the Schools Open - locked (needs 5 seals)
```

`guildgate status`: `Gate: enabled | Guild: LOCKED | Progress: 0 of 5`
(campaign untouched).

LuckPerms probes (the dormancy proof):

```
lp group default permission check magic.cast.heal
  → "- default does not have magic.cast.heal set." / "Result: undefined"
lp group default permission check magic.cast.cure
  → "- default does not have magic.cast.cure set." / "Result: undefined"
```

`list` answered (`There are 0 of a max of 5 players online` — 5 is the
scratch cap; live is 10). `version` answered (Paper 26.2-123).

## Post-boot disk state (what the install creates)

- `plugins/ExcelMagicGate/`: `config.yml`, `state.yml`
  (`appliedStage: 0`, `lastKnownSeals: 0`), `stages/stage0..stage5/`
  (extracted by the bridge on first enable).
- `plugins/Magic/`: the six seeded files plus the rest of Magic's data
  tree, which Magic generates itself on first boot around the seed.
- Stage-0 `wands.yml` carries only the hidden spell-less base templates
  (base_keep, base_bound, basic_wand, base_wand) — no rank wands, and no
  loaded wand template carries any spell.
