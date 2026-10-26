# CHANGELOG: Magic (elmakers) stage files, all magic opens by Seal 4 (ExcelMagicGate stages 2 to 5)

Record type: install record for config files read by ExcelMagicGate 0.1.0 (WRV plugin). Magic 11.2.5 is the elmakers plugin and is not changed. No jar changed.
Status: **Part A LIVE (dormant) Oct 26, 2026, about 3:20 to 3:34 PM CDT. Part B, the Seal 1 level fix, ExcelWandBars 0.2.9/0.3.0 and the Quartermaster fix are NOT live** (they ride the next approved restart).
This PR is a draft record. Do not merge.

## Approvals (owner, Oct 26, 2026, CDT)
- 2:04 PM: asked for all abilities released by Seal 4 ("Since stage 5 release people are going to elitemobs world anyways") and an organized Spell Book.
- 2:13 PM: Disposal spell removed. Sunny Day, Stormy Night, Weather and Time released at Seal 4 with a long per-player cooldown.
- 2:14 PM: Invincible at Seal 4, 5 s, long cooldown.
- 2:15 PM: Masons building spells for everyone at Seal 4. Seal split approved.
- 2:16 PM: Spell Book shows rank-locked spells greyed out, broken seals only (later ExcelWandBars version, not in this record).
- 2:17 PM: 8 Spell Book groups (Healing, Defense, Damage, Movement, Summon, Building, World, Utility) (ExcelWandBars 0.3.0, PR #90, not in this record).
- 2:48 PM: "Yes, put the new spell files in now" (Part A, no restart).
- 2:49 PM: "Yes, do the restart once it's empty" (Part B and the other restart items; not done yet).
- 3:05 PM: "Yes, use all the level fixes" (all 8 spells, Lightning included). 3:10 PM: per-level numbers OK.
- 3:28 PM: keep Mend 2 at 0.88 heart and 6 mana.

## What changed, file by file (Part A, 21 files, live)
### Spell release by seal (wands.yml rank spell lists, paths.yml, grants.txt in stages 2 to 5)
Spells on each rank wand, cumulative (Beginner / Student / Apprentice / Master):

| Seal | Before | After | New on Master wand (before, after) |
|---|---|---|---|
| 1 (live, unchanged) | 6 / 12 / 15 / 17 | 6 / 12 / 15 / 17 | 17, 17 |
| 2 The Pathfinder | 12 / 27 / 34 / 37 | 14 / 32 / 42 / 45 | +20, +28 |
| 3 The Wonder | 19 / 44 / 56 / 63 | 22 / 53 / 68 / 76 | +26, +31 |
| 4 The Masons | 19 / 44 / 56 / 63 | 23 / 55 / 75 / **104** | +0 (Masons only on a hidden builder wand), +28 |
| 5 The Guild Opens | 23 / 55 / 76 / 105 | 23 / 55 / 75 / 104 | +42, +0 |

Full Master set is 104 (was 105, Disposal removed). Magic "Loaded N spells" on the rig: 43 / 93 / 161 / 202 / 202 for Seals 1 to 5.

Spells moved earlier (every one used to wait for Seal 5):
Seal 2: 28 new on the Master wand, 9 of them moved here
  - Beginner: Eat Food (`eat_food`, was Seal 5, Utility), Roll Dice (`rolldice`, was Seal 5, Utility)
  - Student: Skating (`skating`, was Seal 5, Movement), Wear Skates (`wearskates`, was Seal 5, Movement), Admission (`admission`, was Seal 5, Utility)
  - Apprentice: Tame Pet (`tamepet`, was Seal 5, Summon), Anvil (`anvil`, was Seal 5, Utility), Healing (`healing`, was Seal 5, Healing), Music (`music`, was Seal 5, Utility)
Seal 3: 31 new on the Master wand, 5 of them moved here
  - Beginner: Reflect (`reflect`, was Seal 5, Defense)
  - Student: Boomerang (`boomerang`, was Seal 5, Damage), Throw Sword (`throwsword`, was Seal 5, Damage), Whip (`whip`, was Seal 5, Damage)
  - Master: Holy Light (`holylight`, was Seal 5, Healing)
Seal 4: 28 new on the Master wand, 28 of them moved here
  - Beginner: Cloak (`cloak`, was Seal 5, Defense)
  - Student: Earth (`earth`, was Seal 5, Building)
  - Apprentice: Critical (`critical`, was Seal 5, Damage), Counterspell (`counterspell`, was Seal 5, Damage), Arrow Rain (`arrowrain`, was Seal 5, Damage), Sweep (`sweep`, was Seal 5, Damage), Power Stone (`powerstone`, was Seal 5, Damage)
  - Master: Force (`force`, was Seal 5, Damage), Fury (`fury`, was Seal 5, Damage), Invincible (`invincible`, was Seal 5, Defense), Tendril (`tendril`, was Seal 5, Damage), Shrink (`shrink`, was Seal 5, Damage), Master Blast (`masterattack`, was Seal 5, Damage), Sunny Day (`day`, was Seal 5, World), A Dark and Stormy Night (`night`, was Seal 5, World), Weather (`weather`, was Seal 5, World), Time (`time`, was Seal 5, World), Local Rain (`localrain`, was Seal 5, World), Restore Local Weather (`restorelocalweather`, was Seal 5, World), Platform (`platform`, was Seal 5, Building), Walls (`walls`, was Seal 5, Building), Sponge (`sponge`, was Seal 5, Building), Tent (`tent`, was Seal 5, Building), Tiny House (`tiny_house`, was Seal 5, Building), Giant House (`giant_house`, was Seal 5, Building), Harden (`harden`, was Seal 5, Building), Rubberize (`rubberize`, was Seal 5, Building), Lay on Hands (`layonhands`, was Seal 5, Healing)
Removed: [('Disposal', '2')]

The other spells of each seal are unchanged (same seal, same rank band).

### Disposal spell removed (stages 2 to 5)
- `wands.yml`: `disposal` dropped from `apprentice.spells` and `master.spells`.
- `paths.yml`: dropped from the path.
- `spells.yml`: definition replaced by `disposal:` / `  enabled: false` (Magic skips it at load). Rig: `castp Tester disposal` = "Unknown spell disposal" at every seal.
- `grants.txt`: `lp group default permission set magic.cast.disposal true` commented out.
- Reason: it destroyed items with no undo (same reason Essentials /disposal was turned off at 2:03 PM). Nobody ever had it: live is Seal 1 and Disposal was a Seal 2 spell.

### World and Invincible cooldowns (stage 4 and 5 `spells.yml`)
Magic has no server-wide cooldown, so these are per player. Every rank wand carries `cooldown_reduction: 0.2`, so in-play waits are 80% of the setting.
- `day.parameters.cooldown`: 1800000 (30 min, unchanged; 24 min in play).
- `night.parameters.cooldown`: 1800000 (30 min, unchanged; 24 min in play).
- `weather.parameters.cooldown`: 900000 -> 1200000 (15 -> 20 min; 16 min in play).
- `time.parameters.cooldown`: 10000 -> 1200000 (10 s -> 20 min; 16 min in play).
- `invincible.parameters.duration`: 5000 (5 s, unchanged). `invincible.parameters.cooldown`: 180000 -> 600000 (3 -> 10 min; 8 min in play). `cooldown_mage` 6000 unchanged.
- Local Rain and Restore Local Weather are `player_weather: true` (only the caster sees it), unchanged.

### Masons for everyone at Seal 4 (stage 4)
- `grants.txt`: the `lp group builder ...` block is commented out, so ExcelMagicGate never runs `lp creategroup builder`. The 8 nodes `magic.cast.platform`, `walls`, `sponge`, `tent`, `tiny_house`, `giant_house`, `harden`, `rubberize` go to `lp group default`.
- `wands.yml` and `paths.yml`: the 8 spells are on `master.spells` and the master path at Seal 4 (stage 4 rank lists now equal stage 5).
- The hidden `masons` wand template stays (unused; ExcelWandBars already excludes it). No builder group exists on the live server.

### Display names for 5 spells (`messages.yml`, `spells:` block)
- Stages 2 to 5: `throwmagictorch` Throw Magic Torch, "Toss a magic light at a far spot"; `hulkleap` Hulk Leap, "Leap high and far, with a safe landing"; `tamepet` Tame Pet, "Tame animals and care for your pets".
- Stages 4 and 5: `localrain` Local Rain, "Rain that only you can see"; `restorelocalweather` Restore Local Weather, "Bring back the real weather for you".
- Every line 38 characters or less, no em dashes (Bedrock forms). The Master Wand name from 1:07 PM is kept.

### Spell level fixes (stages 2 to 5 `spells.yml`, via tools/levelfix.py; details in SPELL-LEVEL-AUDIT.md)
Every level now changes something real. Rank gates (`upgrade_required_path`, casts) unchanged. No damage cap changed.

| Level | Before | After |
|---|---|---|
| Mend 2 (`mend|2`) | empty (same as level 1: 0.75 heart, 4 mana) | heal 0.88 heart (`parameters.amount 1.75`), `costs.mana 6` |
| Flash Heal 2 (`flashheal|2`) | empty (2.5 hearts, 15 mana) | 2.75 hearts (`amount 5.5`), mana 19 |
| Greater Heal 2 (`greaterheal|2`) | empty (3 hearts, 8 mana) | 3.5 hearts (`amount 7`), mana 9 |
| Storm Shackle 2 (`stormshackle|2`) | empty (8 s, 15 mana) | cooldown 7000 (7 s, 5.6 s in play), mana 16; levels 3 and 4 inherit 7 s. Damage untouched |
| Magic Missile 5 (`missile|5`) | empty (restated level 4) | cooldown 1000 -> 850, range 64 -> 72; 3 heart damage cap kept, mana 15 kept |
| Flare 2 (`flare|2`) | cosmetic only | range 16 -> 24, cooldown 2000 -> 1500 |
| Flare 3 (`flare|3`) | cosmetic only | range 24 -> 32, cooldown 1500 -> 1000 |
| Platform 4 (`platform|4`) | brush change only | radius 2 -> 2.5 |
| Lightning 2 (`lightning|2`) | strike spread 8 -> 6 only | cooldown 45000 -> 40000; level 3 inherits |

Mend mana by level is now 4 / 6 / 8 / 12 / 17 (heals 0.75 / 0.88 / 1 / 1.25 / 1.5 hearts). Beginner wands cast Mend at level 2, so a Beginner pays 6 mana (was 4) and heals 0.88 heart (was 0.75): fewer casts per mana bar.
The same three heal fixes for live Seal 1 (`plugins/Magic/spells.yml` and stage1) are in `seal1-levelfix-not-live/` and are **NOT live** yet.

### Wands rebased on live (stages 2 to 5 `wands.yml`)
The drift guard found the live blue mana boss bar (`base_wand.boss_bar` "Mana @mana", from the ExcelWandBars 0.2.7 install) in all 4 live stage wands.yml. The new files are built on the live copies: the only changes are the rank spell lists and one WRV header comment.

### stage4/config.yml
Now the same file as stage5/config.yml (e478fa13): header line and the automata comment block. No setting value changed.

## Install record (Part A)
- Route: Shockbyte panel File Manager on the desktop (box SFTP times out). Order stage5, stage4, stage3, stage2.
- Drift guard before each file: live matched `replaced-base/` (the 2:57 PM CDT live download).
- Backups per file: `<name>.bak-20261026` (copy) and `<name>.old-20261026` (the renamed original). Uploaded as `<name>.uploading`, renamed into place one by one.
- Read-back 3:36 PM CDT: every file downloaded again, all 21 sha256 match `live-stages/`:

| Live path | Replaced (sha256, short) | Now (sha256, short) | Read-back |
|---|---|---|---|
| `plugins/ExcelMagicGate/stages/stage2/grants.txt` | `fb3ab70d` | `b9f8414c` | OK |
| `plugins/ExcelMagicGate/stages/stage2/messages.yml` | `5b55d644` | `d526ae61` | OK |
| `plugins/ExcelMagicGate/stages/stage2/paths.yml` | `124f6b12` | `fc49dbee` | OK |
| `plugins/ExcelMagicGate/stages/stage2/spells.yml` | `0b267e35` | `0958a6e9` | OK |
| `plugins/ExcelMagicGate/stages/stage2/wands.yml` | `68ec54ac` | `2c117abe` | OK |
| `plugins/ExcelMagicGate/stages/stage3/grants.txt` | `dfbf729d` | `3b0a902a` | OK |
| `plugins/ExcelMagicGate/stages/stage3/messages.yml` | `5b55d644` | `d526ae61` | OK |
| `plugins/ExcelMagicGate/stages/stage3/paths.yml` | `96c53500` | `ff289851` | OK |
| `plugins/ExcelMagicGate/stages/stage3/spells.yml` | `99c951cb` | `a31f77d4` | OK |
| `plugins/ExcelMagicGate/stages/stage3/wands.yml` | `52218430` | `8ea84f46` | OK |
| `plugins/ExcelMagicGate/stages/stage4/config.yml` | `ec157ef2` | `e478fa13` | OK |
| `plugins/ExcelMagicGate/stages/stage4/grants.txt` | `686e802d` | `172da2ac` | OK |
| `plugins/ExcelMagicGate/stages/stage4/messages.yml` | `5b55d644` | `793b6968` | OK |
| `plugins/ExcelMagicGate/stages/stage4/paths.yml` | `22335b76` | `c8e537e3` | OK |
| `plugins/ExcelMagicGate/stages/stage4/spells.yml` | `b5e2a1a7` | `0288266b` | OK |
| `plugins/ExcelMagicGate/stages/stage4/wands.yml` | `dbf75eb8` | `2595abaa` | OK |
| `plugins/ExcelMagicGate/stages/stage5/grants.txt` | `70d1fa4e` | `5bc0349f` | OK |
| `plugins/ExcelMagicGate/stages/stage5/messages.yml` | `04293ccc` | `793b6968` | OK |
| `plugins/ExcelMagicGate/stages/stage5/paths.yml` | `d916b29d` | `1c1fdefb` | OK |
| `plugins/ExcelMagicGate/stages/stage5/spells.yml` | `a37585dd` | `3c7dabe8` | OK |
| `plugins/ExcelMagicGate/stages/stage5/wands.yml` | `dcdb8b8c` | `2595abaa` | OK |

- No `magic load`, no `magic reload`, no restart, no whitelist change. Live stays at Seal 1 ("Applied Magic stage: 1 of 5"); these files are only read when a seal breaks.
- Nothing changes for players today.

## Not live (rides the approved combined restart)
- Part B (`part-b-not-live/`): ExcelMagicGate `config.yml` (`messages.stages.2-5` text: Seal 4 "Every school of magic is open", Seal 5 "no new spells") and ExcelGuildGate `config.yml` (Seal 4 and 5 story beats and "What fell open" lines). Neither plugin has a reload command. Must be live before Seal 4 breaks.
- Seal 1 level fix (`seal1-levelfix-not-live/`, `magic load` or the restart).
- ExcelWandBars 0.2.9 (PR #88) and 0.3.0 Spell Book (PR #90), Quartermaster curing fix.
- What's New entry: not posted.

## Tests (rig only, never the live server; TEST-RESULTS.md)
- Runs 2 to 5 (2:38 to about 3:23 PM CDT): each seal applied by the real seal watch, counts per rank exactly as the table above, 0 SEVERE, 28 known recipe warnings each apply, Disposal "Unknown spell" at every seal, no builder group, cooldown messages 24 / 16 / 16 / 8 min, display names shown, blue mana bar on every rank wand, level-fix numbers on every info screen.

## Rollback (ROLLBACK.md)
Before any seal past 1 applies: rename `<name>` to `<name>.rejected-20261026` and copy `<name>.bak-20261026` back (or rename `.old-20261026` back). No reload, no restart. After a seal applies with the new files, see ROLLBACK.md (ExcelMagicGate keeps the previous Magic set in `backups/`).
Rerun the per-wand override step after any later seal apply.

## Folder
- `live-stages/stageN/`: the 21 files now live (byte-identical to the read-back).
- `replaced-base/stageN/`: the files they replaced (the live 2:57 PM CDT download).
- `diffs/`: unified diffs replaced -> live, one per file.
- `part-b-not-live/`, `seal1-levelfix-not-live/`: staged, not live.
- `tools/`: build.py, levelfix.py, check.py (paths point at the private build box). `build_tables.py`, `per-stage-counts.csv`, `spells-by-seal.csv`: the seal tables.
- `SHA256SUMS.txt`: full hashes.
