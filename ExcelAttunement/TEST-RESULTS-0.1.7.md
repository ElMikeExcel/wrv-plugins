# Test results: ExcelAttunement 0.1.7 (Oct 26, 2026, about 1:00 to 1:20 AM CDT)

## Build
- The 0.1.6 source zip (7e2028b4) rebuilt on the box = 441d3f70..., the live jar, byte for byte. So the base is exactly live.
- 0.1.7 = 4b5d8837eca4039e0d3f63a33fdd849a94e245718cbe769c51725c0d71665dcc. Three builds (two from the work tree, one clean from source/ExcelAttunement-0.1.7-src.zip) are byte-identical, and the rig ran this exact jar.

## Unit tests (plain JVM): 977 passed, 0 failed (evidence/unit-tests-0.1.7.txt)
- All 0.1.6 suites still pass (stance, weapons, ranged, lock, ward, arena, spell table).
- New HotbarSkipTest (321 checks; 656 + 321 = 977) covers:
  - scroll direction and wrap (8>0, 0>8)
  - the reported layout (wand in slot 5, potions in 1 and 9) from slots 1, 4, 6 and 9, both directions: all 8 other slots reached, never the locked one
  - two locked slots in a row
  - locked slots at the wrap edge
  - every other slot locked (stay put)
  - an exhaustive 9x9 check that a hop never returns the old slot or a locked slot

## Rig (Paper 26.3 box rig, Magic 11.2.5, Java bot; script evidence/t_scroll.js)
Hotbar: 1 healing potion, 2 bread, 3 iron sword, 4 cobblestone, **5 wand**, 6 torch, 7 apple, 8 shield, 9 splash healing potion. Each scroll notch is a one-slot select from the server's current slot. The server-side slot is read back after every step.

| Check | 0.1.6 (live today) | 0.1.7 |
|---|---|---|
| No stance: scroll onto slot 5 | lands on 5 | lands on 5 |
| Fighter: scroll right from 4 | **stuck: 4>4>4>4...** | 4>6>7>8>9>1>2>3>4>6... all 8 slots |
| Fighter: scroll left from 6 | **stuck: 6>6>6...** | 6>4>3>2>1>9>8>7>6... all 8 slots |
| Fighter: sword (3) to potion (9), 5 notches | **stuck at 4** | 3>4>6>7>8>9 |
| Fighter: potion (9) to potion (1) scrolling left | **stuck at 6** | 9>8>7>6>4>3>2>1 |
| Fighter: number key 5 (wand) | refused (stays on 1) | refused (stays on 1) |
| Fighter: number keys 9, 1, 3, 7 | work | work |
| Fighter: cast blocked (backstop) | BLOCKED cast 'missile' | BLOCKED cast 'missile' |
| Caster (sword moved to slot 5): scroll right / left | **stuck at 4 / stuck at 6** | hops slot 5, all 8 slots both ways |
| Items lost or duplicated | none (41 = 41) | none (41 = 41) |
| Stance cleared: slot 5 reachable | yes | yes |

- 0.1.7 rig run: **20 of 20 PASS** (evidence/rig-run-0.1.7-after.log).
- 0.1.6 baseline: 14 pass, 6 FAIL, reproducing the bug (evidence/rig-run-0.1.6-before.log). That run used the script one revision earlier. Only the dummy zombie setup changed after it (it burned in daylight, so later Fighter refreshes did not land). The stuck scroll rows above are from runs where the stance was live.
- Rig boot with 0.1.7: "Enabling ExcelAttunement v0.1.7", "Stance lock: weapon-lock ON, wand-lock ON, actionbar ON (live ON), sounds ON.", 0 SEVERE. The only ERROR lines are the rig's known Jobs "Vault is required" lines, which are rig-only.
- The rig was put back to 0.1.6 afterwards.

## Modes
- Java default (Classic wand controls), and every WandBars style: the lock lives in ExcelAttunement's hotbar handler, which sees every hotbar change whatever the wand style. So the fix is the same in every mode.
- Bedrock touch: each slot is a direct tap, so there was never a trap (a tap on the wand was refused, the other slots were tappable). Unchanged.
- Bedrock controller bumpers step one slot like the wheel, so they now hop too.
- Not verified on a real Bedrock device. Owed in game: a Java player in Fighter stance scrolls past the wand, and a Bedrock controller if anyone has one.
