# TEST RESULTS: all magic by Seal 4 packet (rig only)

Rig: /workspace/s4-rig (private, Java 25971 / rcon 25972, flat world, offline mode; never touches the live server). Paper 26.3, Magic 11.2.5, ExcelMagicGate 0.1.0, ExcelGuildGate 0.3.3, ExcelWandBars 0.2.9, LuckPerms 5.5.87.
Seed = live Seal 1: plugins/Magic = stage1 files + live messages (wands 289539cd, spells c505e510, paths 95d9f0a7, messages 5b55d644), ExcelMagicGate stages 0-1 unchanged, stages 2-5 = new/, both new configs, appliedStage 1.
Method: bot "Tester" online; `guildgate grant <seal>` one at a time; the real seal watch (every 60 s) applied each stage by itself; then for each rank `clear`, `wandp Tester <rank>`, `wandbars spelldump Tester`, compared the spell keys with new/stageN/wands.yml.

## Run 3 (display names, Oct 26 2026, 2:47 to 2:53 PM CDT): evidence/run3/
Same rig, same reset (Seal 1 seed, LuckPerms and Magic data wiped), stages 2-5 = new/ after the display-name rebuild (only the 4 messages.yml changed: stage2/3 d526ae61, stage4/5 793b6968; every other file sha identical to run 2). Script test/seals3.js = seals2.js plus the spelldump name line per key.
| Seal | Magic "Loaded N spells" | WARNINGS | SEVERE | Beginner | Student | Apprentice | Master |
|---|---|---|---|---|---|---|---|
| 1 (boot) | 43 | - | 0 | 6 | 12 | 15 | 17 |
| 2 (seal watch 2:49 PM) | 93 | 28 | 0 | 14 | 32 | 42 | 45 |
| 3 (2:50 PM) | 161 | 28 | 0 | 22 | 53 | 68 | 76 |
| 4 (2:51 PM) | 202 | 28 | 0 | 23 | 55 | 75 | 104 |
| 5 (2:53 PM) | 202 | 28 | 0 | 23 | 55 | 75 | 104 |
Counts = run 2 exactly. The 28 warnings are the same crafting-recipe lines as run 2 (rig only). Disposal: "Unknown spell" every seal.
Spelldump names (Student wand has Throw Magic Torch and Hulk Leap; Apprentice adds Tame Pet; Master at Seal 4/5 adds Local Rain and Restore Local Weather): "Throw Magic Torch (Rank 1)", "Hulk Leap (Rank 1)", "Tame Pet (Rank 1)", "Local Rain (Rank 1)", "Restore Local Weather (Rank 1)". At Seal 5, 0 of the 104 Master spells show a name equal to the raw key. Rig plugins/Magic/messages.yml after Seal 5 = 793b6968 (the stage5 file).

## Run 2 (Oct 26 2026, 2:38 to 2:44 PM CDT): evidence/run2/
| Seal | Applied by | Magic "Loaded N spells" | LP grant cmds | Beginner | Student | Apprentice | Master | Keys = packet |
|---|---|---|---|---|---|---|---|---|
| 1 (boot) | seed | 43 | - | 6 | 12 | 15 | 17 | yes (stage1) |
| 2 | seal watch, 2:39 PM | 93 | 45 | 14 | 32 | 42 | 45 | yes |
| 3 | seal watch, 2:40 PM | 161 | 76 | 22 | 53 | 68 | 76 | yes |
| 4 | seal watch, 2:41 PM | 202 | 104 | 23 | 55 | 75 | 104 | yes |
| 5 | seal watch, 2:43 PM | 202 | 104 | 23 | 55 | 75 | 104 | yes |

- Every apply: "Magic stage N applied successfully", zero SEVERE, Magic "WARNINGS: 28" each time, all 28 = the 14 known crafting recipe lines (broom_booster, magichat, ...), the same 28 as the Seal 1 boot. No other warning.
- Ranks unchanged: Spell Book headers at every seal say Beginner 0/800 SP to Student, Student 0/1,600 to Apprentice, Apprentice 0/1,800 to Master, Master top rank. Wand names Beginner/Student/Apprentice/Master Wand.
- Disposal: absent from every rank wand at every seal; `castp Tester disposal` = "Unknown spell disposal" at seals 1-5 (the `enabled: false` stub really unloads it); no magic.cast.disposal grant in any apply.
- Masons: Seal 4 grants set magic.cast.platform ... giant_house for group default; no `creategroup builder`, no "builder" anywhere in the log.
- Cooldowns (Master Wand, 0.2 wand cooldown reduction), second cast 7 s after the first: Sunny Day "You must wait another 25 minutes", Time "17 minutes", Weather "17 minutes", Invincible "8 minutes". (Magic rounds up: 24 / 16 / 16 / 8 min.)
- Announcements in chat: Seal 2 "New magic: Blink, Glide, Lift, Skating, Tame Pet, Healing, and light and vision spells."; Seal 3 "Lightning and Chain Lightning at full strength, Reflect, Boomerang and Holy Light."; Seal 4 "the last schools: building magic for everyone, weather and time, and the Master spells." + "Every spell in the valley is now open..."; Seal 5 "Every school of magic is already yours." + "Type /ag to visit the Adventurers Guild."
- Story lines (`guildgate narrate 4/5`, run 1): Seal 4 "The Masons take their place at Master rank for every mage, and the last schools return with them..."; Seal 5 "Your magic is already complete: every school opened with the Fourth Seal." No fight spoilers.

## Run 1 (2:30 to 2:35 PM CDT): evidence/run1/
Same results, except 4 spelldump counts were 1 short (seal 3 student, seal 4 student/apprentice/master, seal 5 master). Cause: the rig rcon helper read only the first 4 KB rcon packet, cutting one line at the packet edge. Fixed with rcon2.py (reads every packet); run 2 counts exactly match the packet. Not a packet problem.

Rig stopped at about 2:45 PM CDT.

## Run 4: wands rebuilt on the live copies (mana bar), Oct 26 2026 about 3:00 PM CDT
- Rig: stage1/Magic wands 4f8ee7bb (live), stages 2-5 wands from build.py LIVE.
- Spell counts (beginner/student/apprentice/master): Seal 1 6/12/15/17, Seal 2 14/32/42/45, Seal 3 22/53/68/76, Seal 4 23/55/75/104, Seal 5 23/55/75/104. Same as runs 2 and 3.
- Mana boss bar shown at every seal: Mana 60 / 100 / 150 / 200.
- Magic: Loaded 43/93/161/202/202 spells, WARNINGS 28 each, 0 SEVERE.
- Evidence: evidence/run4/.

## Run 5: all spell level fixes (Mike 3:05 PM CDT), Oct 26 2026 3:16 to 3:22 PM CDT
- Rig: stage1 spells 229f8c61 (Seal 1 level fix), stages 2-5 spells from build.py with levelfix.py.
- Spell counts: identical to run 4 at every seal and rank. Mana bar 60/100/150/200 at every seal.
- Magic: Loaded 43/93/161/202/202, WARNINGS 28 each, 0 SEVERE.
- Spell info screens (ExcelWandBars, read live from config) show the new numbers. Cooldowns are shown after each seal's wand cooldown reduction:
  - Mend rank 2 (Beginner wand): Heals 0.88 heart, Cost 6 Mana (all seals).
  - Flash Heal rank 2 (Student): Heals 2.75 hearts, Cost 19 Mana.
  - Greater Heal rank 2 (Apprentice): Then heals 3.5 hearts, Cost 9 Mana.
  - Storm Shackle rank 2: Cost 16 Mana, Cooldown 6.5 sec (Seal 2) / 6 sec (Seals 3-5) from 7 sec; ranks 3/4 inherit it.
  - Magic Missile rank 5: Cooldown 0.7 sec, Range 72 blocks; damage unchanged.
  - Flare rank 2: Range 24, Cooldown 1.3 sec; rank 3: Range 32, Cooldown 0.9 sec.
  - Lightning (Master, rank 3 inherits rank 2): Cooldown 34 sec (40 sec before reduction).
  - Platform rank 4: radius 2.5 in the loaded config (the info screen does not list radius).
- Evidence: evidence/run5/ (seals5.js, seals5.log without BOSSBAR lines, result5.json, boot.log, seal1-templates.txt). First attempt (bot timeout from the bossbar flood) kept in the rig at runs/run5a/.
