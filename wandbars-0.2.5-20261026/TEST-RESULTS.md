# ExcelWandBars 0.2.5 test results (box rig, 2026-10-26)

Jar: files/plugins/ExcelWandBars-0.2.5.jar
sha256 1e897c936430dacec3b15231e658f7dd87147f9ca8ff4f2cecdf67ba1cecde3c (rebuilds byte for byte)
Live base: ExcelWandBars-0.2.4.jar sha256 2c1526ba494a6402f508d050e2a7fcbbbf05c37c3ef1ad21b05f99cbbed519c2

## Status, kept separate

| Item | Status |
|---|---|
| Built | YES, deterministic jar |
| Unit tests | 460 passed, 0 failed (run-tests.sh) |
| Rig tested (Paper 1.21.11, mineflayer bots) | YES, see tables below |
| Java proof | Rig bot on a Java connection. Not tried with a real Java game client |
| Bedrock proof | Simulated only (`wandbars simbedrock` on a bot, swing, item use and drop sent as a phone would). NOT tried on a real phone through Geyser |
| Live server | NOT touched. Not installed |

## Rig runs

| Run | Jar | Result |
|---|---|---|
| main | 0.2.5 | 83 pass, 9 fail (all 9 explained below, none is a 0.2.5 regression) |
| restart (server stopped with the bar up on page 2) | 0.2.5 | 5 pass, 0 fail |
| probe (rechecks of the main fails) | 0.2.5 | 7 pass, 4 fail (explained below) |
| rollback to 0.2.4 with the 0.2.5 data and config | 0.2.4 | 5 pass, 0 fail |
| probe, same script, for comparison | 0.2.4 | 5 pass, 6 fail |


## What passed (main run, 0.2.5)

- Tap bar: every unlocked spell goes on the hotbar. Beginner 6 spells on 1 page, Student 12 on 2 pages, Master 17 on 3 pages. Every spell shown once, none hidden. There is always one empty slot to get items back.
- Crouch with only 1 page says so on the action bar ("all 6 spells are on the bar"). Crouch only pages when spells do not fit.
- Tap casts at once. The wand stays selected. The action bar shows name, mana, cooldown and rank.
- Hold-to-read (simulated Bedrock): press and hold as a swing, as a right click (item use) and as a drop all show the spell list or the last tapped spell page. None of them casts or closes the bar. Holding on a block never casts.
- Tap bar wand controls: quick cast on, left click none, drop none, right click none. Magic can no longer toggle the bar closed on a phone hold.
- Item safety in Tap bar, Classic, Cycle, Sneak bars, Original: put away, death (keepInventory on), log out and back, world change, chest open, dragging an icon out, server stop with the bar up on page 2. Items exact, no spell icons left anywhere.
- Sneak bars stays a separate option. On Java the Wand controls menu now has all 5 styles and Sneak bars applies on Java.
- Menu wording: every one-line description says what crouch (Bedrock) or sneak (Java) does. 42 chars or less. No em dashes.
- Java: key casts, left click reads (right after a cast it shows that spell), Q reads, sneak flips pages, right click puts items back, right click again brings the bar back.
- Restart: style and pages survive. Items back, no icons. Tap opens, tap casts.
- Rollback: 0.2.4 boots on the 0.2.5 data. Items intact. 0.2.4 puts its own Tap bar controls back (right click toggle, drop toggle). Tap still casts.

## Stance lock (ExcelAttunement 0.1.7)

The stance section in the main run was invalid. The harness used `/item`, which Essentials took over ("Only in-game players can use item"), so the sword was never given and no stance started. Fixed in the harness (`minecraft:item`) and rerun in the probe:

| Check | 0.2.5 | 0.2.4 |
|---|---|---|
| Sword hit gives Fighter stance (wand locked) | PASS | PASS |
| Scrolling both ways never lands on the wand, never stops on another slot | PASS (4>3>2>1>9>8>7 and 2>3>4>6>7>8>9) | PASS |
| Tap bar never opens or flickers while locked | PASS (opened 5 -> 5) | PASS |
| Refused switches seen as cancelled and ignored (MONITOR + ignoreCancelled) | PASS (counter 1) | n/a, counter is new in 0.2.5 |
| Stance cleared: tap the wand, Tap bar opens | PASS | PASS |
| "No item lost" | FAIL in the probe, harness only: the sword was written over the cobblestone slot, so the cobblestone check reads 0/13. Total item count unchanged (22 -> 22). Main run version of this check PASSED | |

No regression of ExcelAttunement 0.1.7 hotbar scrolling.

## Fails explained

| Fail | Why | Same on 0.2.4? |
|---|---|---|
| Death with keepInventory off: not every item stack on the ground | Iron ingots stay in the inventory after respawn on this rig (some rig plugin keeps them). Happens in Classic too, where WandBars holds no items. Nothing lost, no icons dropped | YES, same on 0.2.4 Classic and Tap bar |
| Sneak bars: selecting slot 9 does not put the bar away | By design. Sneak bars closes by right click. The probe proves right click closes it with items exact on both versions. Death and log out with the bar up were safe | YES, same on 0.2.4 |
| Original (Tap to pick): tapping an icon casts instead of picking, and hold does not cast | Pre-existing. Same result on 0.2.4 with a fresh wand. Not touched by 0.2.5. Worth a separate look | YES, same on 0.2.4 |
| Stance section in main (5 fails) | Harness bug above, rerun in the probe | n/a |

## Rig plugin versions (not the live set)

Paper 1.21.11, Magic 11.2.5, Geyser and Floodgate, ExcelAttunement 0.1.7, ExcelBedrockMenu 1.5.18, ExcelTrash 0.1.0, ExcelGuide 0.2.3 (scrubbed), autosort 0.1.17. The rig Magic has the staged Mend mana packet applied. Live versions are different for BedrockMenu, Trash, Guide and Vaults. 0.2.5 does not call any of them.

## Not tested

- A real phone through Geyser: whether press and hold arrives as a swing, a right click or a drop. All three are handled and each was simulated.
- A real phone long-press on a hotbar slot (drop).
- A real Java game client in game (only a bot on a Java connection).
- The live server. Nothing was installed or changed there.
