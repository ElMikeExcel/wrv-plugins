# TEST RESULTS: ExcelWandBars 0.3.0 (Oct 26, 2026)

Jar `9dff01536f882fbdeda0f4d2911269d8402daf1cbbeada3cc19a580a945db4e5`. Every rig run in "Final runs" used this exact jar.

## Unit tests
`src/run-tests.sh`: **1319 passed, 0 failed** (0.2.9 had 1227; `SpellbookTest` adds 92). Log: `evidence/unit-tests.log`.
Covers: the bundled spellbook.yml (8 groups, counts 10/8/30/14/5/9/6/22 = 104, no Disposal, every group matches the
approved spells-by-seal CSV); Seal 1 counts (Healing 7, Defense 4, Damage 1, Movement 2, Utility 3; Summon,
Building, World hidden); Beginner wand locked spells only from the Seal 1 rank wands (no spoilers); Seal 4 counts;
sort by rank band then name; Java pages of 10 and Bedrock pages of 12 (edges, past-the-end page, empty list);
`Other` for unlisted spells and the warning list; duplicate spells (first group wins, reported); reserved keys
(`other`, `all`, `group`, `locked`) and broken input never throw; `|level` keys; show-locked off; button and title
formats; more than 1000 generated Bedrock strings at 38 characters or less, no em dashes, no "click".
All 0.2.9 tests still pass unchanged.

## Private rig (`/workspace/wb030-rig`, Paper 26.3, Magic 11.2.5, Geyser + Floodgate, copy of the 0.2.9 rig)
Test accounts only (offline-mode bots). The Bedrock form path is driven with the rig-only `simbedrock`, `lastform`
and `formtap` admin helpers: the same form objects and button callbacks as a real Bedrock player; the bot also gets
the chat fallback because it is a Java client. Rig stopped after the runs (process gone, ports free).

### Final runs on this jar
**Run 2, simulated Seal 4 full set** (`stage.sh seal4full`: approved Seal 4 classes/paths/spells/wands, Disposal
removed; staged before boot). `evidence/rig2.js`, `evidence/rig2.log`, boot `evidence/rig-boot-run2-seal4-then-seal1.log`.

| Case | Result |
|---|---|
| Boot | PASS: `Spell Book groups ON (spellbook.yml, 8 groups, 104 spells listed, 104 on the rank wands now)`, no unlisted warning, no new errors |
| F. Java Master wand: home | PASS: 104 spells, 21 on bar, 83 in book; Healing 10, Defense 8, Damage 30, Movement 14, Summon 5, Building 9, World 6, Utility 22 |
| F2. Java Damage paging | PASS: 3 pages of 10, `[< Prev] [Groups] [Next >]` links right, `bar 9` mark on Storm Shackle |
| F3. Java All A to Z | PASS: page 11 of 11 holds the last 4 |
| F4. Java picker, slot 7 | PASS: groups first (Defense 7: the spell already in slot 7 left out), Damage page 3 of 3 with Previous page / Back to groups / Back to my bar |
| G. Java Student wand at Seal 4 | PASS: 55 known, 49 locked; `[World +6]`, `[Damage 16 +14]`; World all gray `unlocks at Master`; Damage page 2 mixes known then locked |
| H. Bedrock Master: home form | PASS: 8 group buttons with counts, All spells A to Z, Put spells on my bar, controls, tip, Close |
| H. Bedrock Damage | PASS: forms of 12 / 12 / 6, Next page / Previous page / Back to groups |
| H2. Bedrock picker | PASS: bar form -> slot 9 -> Pick -> groups (Damage 29 without the slot's spell) -> Utility 1/2 (`(slot N)` marks) -> 2/2 -> Repair Equipment; slot 9 changed, Storm Shackle to the Spellbook, loadout saved |
| I. Bedrock Apprentice: locked | PASS: `World (6 locked)`, `Damage (23, +7 locked)`; World form all locked; locked page `Unlocks at Master rank.` with `Back to World` |
| J. Unlisted spell (rig spellbook.yml edited, then restored) | PASS: reload -> WARN naming it, `[Other 1]` on home, Other page lists it; restored file back to 104 |
| J2. Broken spellbook.yml | PASS: WARN with the parse error, groups from the jar copy, players unaffected; restored |
| K. `groups: false` + reload | PASS: exact 0.2.9 flat Spell Book on Java and Bedrock; back to groups after restore + reload |

**Run 3, Seal 1 (live Magic files, `stage.sh live` + `magic load`)**. `evidence/rig1.js`, `evidence/rig1.log`.

| Case | Result |
|---|---|
| Boot / reload | PASS: `8 groups, 104 spells listed, 17 on the rank wands now`, no warning |
| A. Java Master wand | PASS: 17 spells, `[Healing 7] [Defense 4] [Damage 1]`, `[Movement 2] [Utility 3]`; Summon/Building/World hidden |
| A2. Group -> spell info -> `[Back to Healing]` | PASS; `/spellinfo stormshackle` typed directly gets `Back to Spell Book` (no stale back link) |
| A3. `/spellbook group defense`, `/spellbook group nope` | PASS: Defense page; unknown group says `No group called nope. Open /spellbook to see the groups.` |
| A4. All A to Z | PASS: 2 pages of 10; page 9 clamps to the last page |
| A5. Java picker by group | PASS: `put feed 4` swapped Feed (slot 3) and Mend (slot 4) |
| A6. 0.2.9 unchanged | PASS: `/spellbar`, spell info screens identical except the back line |
| B. Java Beginner wand | PASS: 6 known, 11 locked, only Seal 1 spells; `[Damage +1]`; `/spellbook locked superheal` page; typing a locked spell name opens its locked page |
| C. Bedrock Master | PASS: home form, Healing form with `- bar N` marks, spell form `Back to Healing` returns to the group, Back to groups, All A to Z 12 + 5 |
| C4. Bedrock picker | PASS: Put spells on my bar -> slot 2 -> Pick -> Defense -> Boon; Boon slot 2, Shell slot 7 |
| D. Bedrock Beginner | PASS: `Healing (3, +4 locked)`, `Damage (1 locked)`; locked Cure page shows its description and `Back to Healing` |
| E. Classic style | PASS: no `bar N` marks (page marks), no bar button |

### Earlier run (jar `3e9834a4`, before the rig fixes)
`evidence/rig1-run1-oldjar.log`, boot `evidence/rig-boot-run1-seal1.log`. Found and fixed: Java home heading repeated
the hover hint; a group page header printed a raw line break; a stale `Back to Healing` after typing `/spellinfo` for a
spell outside that group; `Damage (0, +1 locked)` now `Damage (1 locked)`.

## Not tested
- A real Bedrock phone: how the gray color code looks in a form button and that 13+ button forms scroll fine.
- The Bedrock Classic spell-list intercept opening the new home (needs a real Geyser client; the same
  `openSpellBook` entry point is covered by the rig).
- Real Seal 4 install (Seal 4 is simulated with the staged files).

## Known, outside this plugin
- `localrain`, `restorelocalweather`, `throwmagictorch`, `tamepet`, `hulkleap` have no display name in Magic's config, so
  every screen (0.2.9 too) shows the key. Fix in the Seal 4 staging (spell `name:` or messages), not in this jar.
- Pre-existing rig boot noise, same as the 0.2.9 rig: Jobs Vault errors, Magic `recipe spellbook` warnings.
