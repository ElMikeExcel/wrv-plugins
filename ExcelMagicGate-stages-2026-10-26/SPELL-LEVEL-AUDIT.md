# WRV Magic spell level audit (read-only), Oct 26 2026
Written about 3:10 PM CDT, Mon Oct 26 2026. Asked by Mike (approved 2:59 PM CDT): find "empty" spell levels, since spells should get stronger with every level. **Read-only: no live change, no edit to any staged packet.** Proposed fixes below are NOT applied anywhere.
## Sources
```
070989603433  ../spells-by-seal4-20261026/staged/seal4-full/upload/stage5/spells.yml.uploading
a37585dd3576  ../spells-by-seal4-20261026/staged/seal4-full/live-20261026-1457/stage5/spells.yml
c505e510e92b  /workspace/up2/stage1/spells.yml.uploading
```
- Staged stage5 (full set) and live stage5 (pulled 2:57 PM) have **identical level blocks** (they differ only in Disposal and the 3 Seal 4 cooldowns). Checked by diffing every `spell|N`.
- Live Seal 1 (stage1, sha c505e510): the 8 Seal 1 spells with levels (heal, cure, boon, shell, recall, mend, flashheal, greaterheal) have the **same levels as staged**. Storm Shackle has **no levels on live Seal 1** (its levels arrive with the Seal 2 files). Only diff for Seal 1 spells: `cooldown_mage` 1000 vs 941 (the staged GCD tweak).
- Method: effective config per level = base merged with `|2..|N` (Magic inheritance; lists replace), then every changed key vs the level before. `upgrade_required_*`, `worth`, `earns` ignored. `config.yml` has `load_default_spells: false`, so these files are the whole spell set.
## Headline
- Master wand (full set at Seal 4): **104 spells**. Only **36 have levels**; **68 are single-level** and never level up (no `spell|2` key).
- Of the 36 levelled spells, **8 spells / 9 levels are flagged**: 5 EMPTY (mend 2, flash heal 2, greater heal 2, storm shackle 2, missile 5), 2 COSMETIC only (flare 2 and 3), 1 NON-NUMERIC only (platform 4), 1 DEBATABLE (lightning 2).
- **No backwards levels**: no damage, heal, range, duration or radius goes down anywhere. Numbers that drop are the good kind (cooldown, mana on workbench, spread, breakable). Lightning 2 radius 8 -> 6 is the only power-ish number that drops; it is the strike spread (tighter aim), so marked debatable.
- Live now: mend 2, flash heal 2 and greater heal 2 are empty **on the live Seal 1** too.
- Note on what was told to Mike earlier ("every spell has its own levels"): that is only true for 36 of 104. The other 68 cast the same at every rank.
- Rank gates: `upgrade_required_path` / casts on level N is the gate to go from N to N+1 (Magic `checkAndUpgrade` uses the current level's values). **All gates unchanged** in every proposal.

## Proposed fixes (not applied)

| Level | Now | Proposed | YAML (add to that level block) |
|---|---|---|---|
| Storm Shackle 2 (`stormshackle|2`) | EMPTY: only the upgrade gate is set | cooldown 8 -> 7 s (in play 6.4 -> 5.6 s); mana 15 -> 16. Damage untouched (balance anchor). L3/L4 inherit 7 s | `costs.mana 16, parameters.cooldown 7000` |
| Magic Missile 5 (`missile|5`) | EMPTY: re-states the level 4 values (mob damage 3 hearts, player damage 2 hearts, range 64, mana 15). Nothing changes | cooldown 1 -> 0.85 s; range 64 -> 72. Damage stays 3 hearts (EliteMobs ceiling from Oct 24 fix); mana stays 15 (flat by design) | `parameters.cooldown 850, parameters.range 72` |
| Flare 2 (`flare|2`) | COSMETIC: adds a star firework ring on hit. No number that affects play changes | range 16 -> 24; cooldown 2 -> 1.5 s; mana stays 10 | `parameters.range 24, parameters.cooldown 1500` |
| Flare 3 (`flare|3`) | COSMETIC: adds a second, higher firework ring. No number that affects play changes | range 24 -> 32; cooldown 1.5 -> 1 s; mana stays 10 | `parameters.range 32, parameters.cooldown 1000` |
| Lightning 2 (`lightning|2`) | DEBATABLE: only the strike spread radius 8 -> 6 blocks (tighter aim). No damage, cooldown or cost change | optional: cooldown 45 -> 40 s (keeps radius 6); L3 inherits 40 s; mana stays 80 | `parameters.cooldown 40000` |
| Platform 4 (`platform|4`) | NON-NUMERIC: brush becomes "your wand brush" instead of white glass. Size, cooldown, cost all unchanged | radius 2 -> 2.5 (keeps the wand-brush feature); mana stays 60 | `parameters.radius 2.5` |
| Mend 2 (`mend|2`) | EMPTY: only the upgrade gate is set | heal 0.75 -> 0.875 heart (amount 1.75); mana 4 -> 6 | `costs.mana 6, parameters.amount 1.75` |
| Flash Heal 2 (`flashheal|2`) | EMPTY: only the upgrade gate is set | heal 2.5 -> 2.75 hearts (amount 5.5); mana 15 -> 19 | `costs.mana 19, parameters.amount 5.5` |
| Greater Heal 2 (`greaterheal|2`) | EMPTY: only the upgrade gate is set | heal 3 -> 3.5 hearts (amount 7); mana 8 -> 9 | `costs.mana 9, parameters.amount 7` |

Notes on the fixes:
- Heal steps sit halfway between the neighbours; mana rises a little so cost per heart stays in line (mend 4/6/8/12/17, flash heal 15/19/24/35, greater heal 8/9/10).
- Storm Shackle is the Oct 23 balance "damage anchor", so its fix touches cooldown only (7 s; 5.6 s in play with the 0.2 wand reduction). Levels 3 and 4 inherit the 7 s.
- Magic Missile damage is capped at 6 (3 hearts) by the Oct 24 EliteMobs ceiling and its mana is flat at 15 by design (Oct 23 "pure upgrade" rule), so its level 5 gets cooldown and range instead.
- Flare, Platform, Lightning keep their flat mana (flat by design for those).
- If applied, it must go into every stage file that carries the spell (mend/flash heal/greater heal: stage1-5; storm shackle/missile/flare/lightning: where present in stage2-5; platform: stage4-5) and the stage1 copy for live Seal 1. Seal 1 changes would need the usual restart/reload path; stage2-5 files are read only when a seal breaks.

## Every levelled spell, level by level

Gate = the path/casts set on that level (needed to reach the next level). Hearts = HP/2. Mana = effective cost at that level.

### Heal (`heal`), Seal 1, Healing, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 12 | student / 50 casts | base: range (blocks) 8; cooldown 8 s; heal 0.5 heart |  |
| 2 | 12 | apprentice / 50 casts | cooldown 8 s -> 7 s; range (blocks) 8 -> 32 |  |
| 3 | 15 | apprentice / 50 casts | adds an instant 1 heart on cast (then the 10 s regen as before); cooldown 7 -> 6 s; mana 12 -> 15 |  |

### Boon (`boon`), Seal 1, Defense, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 20 | beginner / 40 casts | base: cooldown 6 s |  |
| 2 | 20 | student / 50 casts | adds Jump Boost III |  |
| 3 | 20 | master / 50 casts | Speed II -> Speed III |  |
| 4 | 20 | master / 50 casts | adds Night Vision |  |

### Cure (`cure`), Seal 1, Healing, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 20 | student / 20 casts | base: radius (blocks) 5; cooldown 12 s |  |
| 2 | 20 | apprentice / 30 casts | also removes Weakness and Wither; cooldown 12 -> 10 s |  |
| 3 | 20 | master / 30 casts | radius 5 -> 8 blocks; also removes Blindness and Hunger |  |
| 4 | 20 | master / 30 casts | also feeds the target: +1 hunger and +1 saturation x4 (every 0.5 s); cooldown 10 -> 8 s |  |
| 5 | 20 | master / 30 casts | can now cure zombie villagers back into villagers (new feature, with effect) |  |

### Shell (`shell`), Seal 1, Defense, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 75 | student / 50 casts | base: range (blocks) 32; radius (blocks) 3; lasts 4 s; cooldown 5 s |  |
| 2 | 75 | apprentice / 50 casts | breakable 4 -> 3; reflect_chance 25% -> 50%; lasts 4 s -> 5 s |  |
| 3 | 75 | master / 50 casts | breakable 3 -> 2; reflect_chance 50% -> 75% |  |
| 4 | 75 | master / 50 casts | breakable 2 -> 1; reflect_chance 75% -> 90% |  |

### Storm Shackle (`stormshackle`), Seal 1, Damage, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 15 | apprentice / 40 casts | base: range (blocks) 48; radius (blocks) 8; target_count 3; player damage 0.25 heart; mob damage 0.25 heart; cooldown 8 s |  |
| 2 | 15 | apprentice / 40 casts | EMPTY: only the upgrade gate is set | **EMPTY** |
| 3 | 18 | master / 40 casts | Slowness I -> II on every hit; mana 15 -> 18 |  |
| 4 | 20 | master / 40 casts | first target gets Slowness VII for 1 s (near root), then Slowness II for 4 s; chain unchanged; mana 18 -> 20 |  |

### Recall (`recall`), Seal 1, Movement, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | - | beginner / 25 casts | base: cooldown 15 s |  |
| 2 | - | student / 30 casts | can recall to your death point and soul (new feature) |  |
| 3 | - | apprentice / 30 casts | sneak-cast places your own marker (new feature) |  |
| 4 | - | master / 30 casts | 2 markers |  |
| 5 | - | master / 30 casts | 2 -> 3 markers |  |

### Torch (`torch`), Seal 2, Utility, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 50 | beginner / 30 casts | base: range (blocks) 32; lasts 30 s; cooldown 10 s |  |
| 2 | 50 | student / 30 casts | cooldown 10 s -> 8 s |  |
| 3 | 50 | master / 50 casts | cooldown 8 s -> 5 s |  |
| 4 | 50 | master / 50 casts | cooldown 5 s -> 2 s |  |

### Peek (`peek`), Seal 2, Utility, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 30 | beginner / 10 casts | base: range (blocks) 32; lasts 2 s; radius (blocks) 0; cooldown 5 s |  |
| 2 | 30 | student / 40 casts | radius (blocks) 0 -> 1 |  |
| 3 | 30 | apprentice / 50 casts | radius (blocks) 1 -> 1.5 |  |
| 4 | 30 | apprentice / 50 casts | radius (blocks) 1.5 -> 2 |  |

### Workbench (`workbench`), Seal 2, Utility, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 50 | apprentice / 50 casts | base: cooldown 20 s |  |
| 2 | 30 | student / 50 casts | mana 50 mana -> 30 mana; cooldown 20 s -> 10 s |  |
| 3 | 10 | student / 50 casts | mana 30 mana -> 10 mana; cooldown 10 s -> 5 s |  |
| 4 | 0 | master / 100 casts | mana 10 mana -> 0 mana; cooldown 5 s -> 0 s |  |

### Bubble (`bubble`), Seal 2, Defense, 2 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 10 | student / 50 casts | base: radius (blocks) 1; range (blocks) 25; cooldown 2 s; lasts 1 s; duration 3 s |  |
| 2 | 10 | student / 50 casts | radius (blocks) 1 -> 2; range (blocks) 25 -> 40; lasts 1 s -> 2 s |  |

### Fling (`fling`), Seal 2, Movement, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 50 | student / 50 casts | base: duration 20 s; cooldown 5 s |  |
| 2 | 50 | apprentice / 100 casts | cooldown 5 -> 2 s; launch speed 1.5-2 -> 2-3 (sneak-cast stays at 2) |  |
| 3 | 50 | master / 150 casts | cooldown 2 s -> 1 s; max_speed 3 -> 4; min_speed 2 -> 3 |  |
| 4 | 50 | master / 150 casts | cooldown 1 s -> 0.2 s; max_speed 4 -> 5 |  |

### Blink (`blink`), Seal 2, Movement, 6 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 50 | student / 40 casts | base: range (blocks) 12; cooldown 10 s |  |
| 2 | 50 | student / 50 casts | range (blocks) 12 -> 16 |  |
| 3 | 50 | apprentice / 50 casts | adds sneak-up Ascend (to the top); cooldown 10 -> 7 s; range 16 -> 24 |  |
| 4 | 50 | apprentice / 75 casts | adds sneak-down Descend; cooldown 7 -> 5 s; range 24 -> 32 |  |
| 5 | 50 | master / 100 casts | can blink through walls up to 8 blocks; range 32 -> 48 |  |
| 6 | 50 | master / 100 casts | cooldown 5 -> 2 s; safe-landing check off |  |

### Magic Missile (`missile`), Seal 3, Damage, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 15 | beginner / 30 casts | base: range (blocks) 24; player damage 1 heart; mob damage 2 hearts; cooldown 1 s; lasts 10 s |  |
| 2 | 15 | beginner / 50 casts | break_durability 2 -> 3; mob damage 2 hearts -> 2.5 hearts; player damage 1 heart -> 1.25 hearts; range (blocks) 24 -> 32 |  |
| 3 | 15 | student / 50 casts | break_durability 3 -> 4; player damage 1.25 hearts -> 1.5 hearts; range (blocks) 32 -> 48; target_breakables 1 -> 2 |  |
| 4 | 15 | apprentice / 50 casts | break_durability 4 -> 5; mob damage 2.5 hearts -> 3 hearts; player damage 1.5 hearts -> 2 hearts; range (blocks) 48 -> 64; target_breakables 2 -> 3 |  |
| 5 | 15 | apprentice / 50 casts | EMPTY: re-states the level 4 values (mob damage 3 hearts, player damage 2 hearts, range 64, mana 15). Nothing changes | **EMPTY** |

### Frost (`frost`), Seal 3, Damage, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 40 | beginner / 50 casts | base: range (blocks) 48; radius (blocks) 1; damage 0.5 heart; duration 1 s; lasts 4 s; cooldown 10 s |  |
| 2 | 40 | student / 50 casts | cooldown 10 s -> 9 s; duration 1 s -> 2 s; mob damage none -> 3 hearts; player damage none -> 1 heart; radius (blocks) 1 -> 2; lasts 4 s -> 5 s |  |
| 3 | 40 | apprentice / 50 casts | cooldown 9 s -> 8 s; duration 2 s -> 3 s; lasts 5 s -> 8 s |  |
| 4 | 40 | apprentice / 50 casts | cooldown 8 -> 7 s; freeze 3 -> 3.5 s; mob damage 3 -> 4 hearts; player damage 1 -> 1.25 hearts; radius 2 -> 3; also freezes lava; ice lasts 8 -> 10 s |  |

### Arrow (`arrow`), Seal 3, Damage, 6 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 20 | beginner / 40 casts | base: lasts 5 s; range (blocks) 50; cooldown 2 s |  |
| 2 | 20 | beginner / 30 casts | arrow damage x0.75 -> x0.8; speed 2 -> 2.5; spread 6 -> 5 (tighter) |  |
| 3 | 20 | student / 50 casts | arrow_damage 0.8 -> 0.85; speed 2.5 -> 3; spread 5 -> 4 |  |
| 4 | 20 | apprentice / 50 casts | arrow_damage 0.85 -> 0.9; speed 3 -> 3.5; spread 4 -> 3 |  |
| 5 | 20 | master / 50 casts | arrow_damage 0.9 -> 0.95; speed 3.5 -> 4; spread 3 -> 1 |  |
| 6 | 20 | master / 50 casts | arrow_damage 0.95 -> 1; speed 4 -> 5; spread 1 -> 0 |  |

### Flare (`flare`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 10 | student / 50 casts | base: range (blocks) 16; cooldown 2 s |  |
| 2 | 10 | apprentice / 50 casts | COSMETIC ONLY: adds a star firework ring on hit. No number that affects play changes | **COSMETIC** |
| 3 | 10 | apprentice / 50 casts | COSMETIC ONLY: adds a second, higher firework ring. No number that affects play changes | **COSMETIC** |

### Stun (`stun`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 15 | student / 50 casts | base: radius (blocks) 4; damage 0.5 heart; duration 3 s; cooldown 8 s |  |
| 2 | 15 | apprentice / 50 casts | damage 0.5 heart -> 0.75 heart; duration 3 s -> 3.5 s; target_breakables 0 -> 1 |  |
| 3 | 15 | apprentice / 50 casts | damage 0.75 heart -> 1 heart; duration 3.5 s -> 4 s; target_breakables 1 -> 2 |  |

### Push (`push`), Seal 3, Damage, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 20 | beginner / 40 casts | base: range (blocks) 8; target_count 2; cooldown 5 s |  |
| 2 | 20 | student / 40 casts | living_entity_speed 0.5 -> 0.6; range (blocks) 8 -> 10; target_count 2 -> 4 |  |
| 3 | 20 | master / 50 casts | living_entity_speed 0.6 -> 0.7; range (blocks) 10 -> 12; target_count 4 -> 6 |  |
| 4 | 20 | master / 50 casts | adds a sneak-down "shockwave" (lift then push everything within 8-12 blocks); push speed 0.7 -> 0.75; range 12 -> 16; repeat 3 -> 4; targets 6 -> 8 |  |

### Stream (`stream`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 30 | apprentice / 30 casts | base: radius (blocks) 2; range (blocks) 32; duration 10 s; player damage 0.125 heart; mob damage 0.25 heart; cooldown 5 s; lasts 0.05 s |  |
| 2 | 30 | apprentice / 30 casts | mob damage 0.25 heart -> 0.375 heart; player damage 0.125 heart -> 0.25 heart; undo_to 3 -> 4 |  |
| 3 | 30 | apprentice / 30 casts | mob damage 0.375 heart -> 0.5 heart; player damage 0.25 heart -> 0.375 heart; undo_to 4 -> 5 |  |

### Wave (`wave`), Seal 3, Damage, 2 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 150 | master / 50 casts | base: range (blocks) 3; target_count 16; cooldown 30 s; lasts 15 s; damage 1 heart |  |
| 2 | 150 | master / 50 casts | item_speed 0.2 -> 0.4; living_entity_speed 0.3 -> 0.6; speed 0.4 -> 0.8; x_size 2 -> 3; z_size 2 -> 3 |  |

### Smite (`smite`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 100 | apprentice / 50 casts | base: range (blocks) 32; radius (blocks) 1.5; cooldown 20 s; damage 1.5 hearts; duration 5 s; lasts 10 s |  |
| 2 | 100 | master / 50 casts | break_durability 50 -> 100; damage 1.5 hearts -> 2 hearts; destructible_durability 200 -> 500; radius (blocks) 1.5 -> 2.5 |  |
| 3 | 100 | master / 50 casts | break_durability 100 -> 1000; damage 2 hearts -> 2.5 hearts; destructible_durability 500 -> 5000; radius (blocks) 2.5 -> 3.5 |  |

### Growth (`growth`), Seal 3, Utility, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 100 | (no path) / 50 casts | base: range (blocks) 32; radius (blocks) 2; cooldown 3 s |  |
| 2 | 100 | (no path) / 50 casts | radius (blocks) 2 -> 3 |  |
| 3 | 100 | (no path) / 50 casts | radius (blocks) 3 -> 4 |  |
| 4 | 100 | (no path) / 50 casts | radius (blocks) 4 -> 5 |  |

### Hat (`hat`), Seal 3, Utility, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 30 | apprentice / 40 casts | base: range (blocks) 32; cooldown 10 s; lasts 30 s |  |
| 2 | 30 | master / 40 casts | lasts 30 s -> 60 s |  |
| 3 | 30 | master / 40 casts | lasts 60 s -> 120 s |  |

### Lightning (`lightning`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 80 | apprentice / 50 casts | base: range (blocks) 32; radius (blocks) 8; cooldown 45 s; lasts 8 s |  |
| 2 | 80 | master / 50 casts | DEBATABLE: only the strike spread radius 8 -> 6 blocks (tighter aim). No damage, cooldown or cost change | **DEBATABLE** |
| 3 | 80 | master / 50 casts | strikes twice (Repeat 2) instead of once: 5 hearts x2 in the area |  |

### Chain Lightning (`chainlightning`), Seal 3, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 75 | apprentice / 50 casts | base: range (blocks) 48; radius (blocks) 8; target_count 3; player damage 0.75 heart; mob damage 1 heart; cooldown 20 s |  |
| 2 | 75 | master / 50 casts | target_count 3 -> 5 |  |
| 3 | 75 | master / 50 casts | target_count 5 -> 8 |  |

### Reflect (`reflect`), Seal 3, Defense, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 10 | beginner / 25 casts | base: range (blocks) 3; radius (blocks) 0; lasts 0.8 s; cooldown 3 s |  |
| 2 | 10 | student / 50 casts | breakable 3 -> 2; radius (blocks) 0 -> 1; reflect_chance 25% -> 50%; lasts 0.8 s -> 0.9 s |  |
| 3 | 10 | apprentice / 50 casts | breakable 2 -> 1; radius (blocks) 1 -> 1.5; reflect_chance 50% -> 75%; lasts 0.9 s -> 1 s |  |
| 4 | 10 | master / 50 casts | radius (blocks) 1.5 -> 2; reflect_chance 75% -> 100%; lasts 1 s -> 1.1 s |  |
| 5 | 10 | master / 50 casts | adds a 1 s "reflective" modifier on the caster on top of the shield (new feature) |  |

### Admission (`admission`), Seal 2, Utility, 2 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 50 | apprentice / 20 casts | base: range (blocks) 16; radius (blocks) 3; lasts 8 s; cooldown 10 s |  |
| 2 | 50 | apprentice / 20 casts | opens all doors (not just unlocked ones); range 16 -> 32 |  |

### Critical (`critical`), Seal 4, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | - | (no path) / 50 casts | base: cooldown 0.5 s; damage 1 + damage_dealt * 0.5 |  |
| 2 | - | master / 100 casts | probability 20% -> 40% |  |
| 3 | - | master / 100 casts | probability 40% -> 60% |  |

### Counterspell (`counterspell`), Seal 4, Damage, 2 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 70 | apprentice / 50 casts | base: range (blocks) 50; cooldown 5 s |  |
| 2 | 70 | apprentice / 50 casts | fizzle chance 30% -> 0% (always works) |  |

### Arrow Rain (`arrowrain`), Seal 4, Damage, 2 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 30 | apprentice / 50 casts | base: lasts 15 s; cooldown 10 s |  |
| 2 | 30 | apprentice / 50 casts | arrows 4 -> 8, arrows are on fire |  |

### Fury (`fury`), Seal 4, Damage, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 100 | master / 50 casts | base: range (blocks) 64; radius (blocks) 24; target_count 3; player damage 2 hearts; mob damage 4 hearts; cooldown 30 s |  |
| 2 | 100 | master / 50 casts | target_count 3 -> 6 |  |
| 3 | 100 | master / 50 casts | target_count 6 -> 8 |  |

### Platform (`platform`), Seal 4, Building, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 60 | apprentice / 50 casts | base: range (blocks) 8; radius (blocks) 1; lasts 20 s; cooldown 5 s |  |
| 2 | 60 | master / 50 casts | radius 1 -> 1.5 (the "disc" alias name is dropped) |  |
| 3 | 60 | master / 50 casts | radius (blocks) 1.5 -> 2 |  |
| 4 | 60 | master / 50 casts | NON-NUMERIC ONLY: brush becomes "your wand brush" instead of white glass. Size, cooldown, cost all unchanged | **NON-NUMERIC** |

### Harden (`harden`), Seal 4, Building, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 30 | apprentice / 50 casts | base: range (blocks) 16; lasts 4 s; cooldown 1.5 s |  |
| 2 | 30 | apprentice / 50 casts | stone -> stone bricks; lasts 4 -> 5 s |  |
| 3 | 30 | apprentice / 50 casts | -> smooth stone; lasts 5 -> 6 s |  |
| 4 | 30 | apprentice / 50 casts | -> polished blackstone; size 2 -> 3; lasts 6 -> 7 s |  |
| 5 | 30 | apprentice / 50 casts | -> obsidian; lasts 7 -> 8 s |  |

### Mend (`mend`), Seal 1, Healing, 5 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 4 | student / 40 casts | base: range (blocks) 16; heal 0.75 heart |  |
| 2 | 4 | student / 40 casts | EMPTY: only the upgrade gate is set | **EMPTY** |
| 3 | 8 | apprentice / 40 casts | mana 4 mana -> 8 mana; heal 0.75 heart -> 1 heart |  |
| 4 | 12 | master / 40 casts | mana 8 mana -> 12 mana; heal 1 heart -> 1.25 hearts |  |
| 5 | 17 | master / 40 casts | mana 12 mana -> 17 mana; heal 1.25 hearts -> 1.5 hearts |  |

### Flash Heal (`flashheal`), Seal 1, Healing, 4 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 15 | apprentice / 40 casts | base: range (blocks) 16; heal 2.5 hearts |  |
| 2 | 15 | apprentice / 40 casts | EMPTY: only the upgrade gate is set | **EMPTY** |
| 3 | 24 | master / 40 casts | mana 15 mana -> 24 mana; heal 2.5 hearts -> 3 hearts |  |
| 4 | 35 | master / 40 casts | mana 24 mana -> 35 mana; heal 3 hearts -> 3.5 hearts |  |

### Greater Heal (`greaterheal`), Seal 1, Healing, 3 levels

| Lvl | Mana | Gate | What changes | Flag |
|---|---|---|---|---|
| 1 | 8 | master / 40 casts | base: range (blocks) 16; heal 3 hearts |  |
| 2 | 8 | master / 40 casts | EMPTY: only the upgrade gate is set | **EMPTY** |
| 3 | 10 | master / 40 casts | mana 8 mana -> 10 mana; heal 3 hearts -> 4 hearts |  |

## Single-level spells (68, no levels at all)

Feed (S1), Extinguish (S1), Aqualung (S1), Diving Helmet (S1), SuperHeal (S1), Blessing (S1), Recall to Last Death (S1), Backflip (S2), Gasp (S2), Magic Torch (S2), Throw Magic Torch (S2), Dash (S2), Evade (S2), Hulk Leap (S2), Piggyback (S2), Repair Item (S2), Repair Equipment (S2), Glide (S2), Lift (S2), Stash (S2), Pull (S3), Sparkle (S3), Grow (S3), Familiar (S3), Ice Arrow (S3), Light Arrow (S3), Shock Arrow (S3), Normal Arrow (S3), Wolf (S3), Cat (S3), Gather (S3), Pollinate (S3), Tame (S3), Eat Food (S2), Roll Dice (S2), Cloak (S4), Boomerang (S3), Throw Sword (S3), Skating (S2), Wear Skates (S2), Earth (S4), Whip (S3), Tame Pet (S2), Anvil (S2), Healing (S2), Music (S2), Sweep (S4), Power Stone (S4), Force (S4), Invincible (S4), Tendril (S4), Shrink (S4), Holy Light (S3), Master Blast (S4), Sunny Day (S4), A Dark and Stormy Night (S4), Weather (S4), Time (S4), Local Rain (S4), Restore Local Weather (S4), Walls (S4), Sponge (S4), Tent (S4), Tiny House (S4), Giant House (S4), Rubberize (S4), Ward (S1), Lay on Hands (S4)

These are not "empty levels", they simply have no levels. Adding levels to them would be a separate design job (ask Mike if wanted).

## Files
- `AUDIT.md` (this), `spell-level-audit-20261026.csv` (one row per spell per level, 201 rows: 133 level rows for the 36 levelled spells + 68 single-level rows), `staged_diff.txt` / `live_diff.txt` / `s1_diff.txt` (raw per-level diffs), `diff.py`, `build.py` (regenerate).
