# Test results: ExcelWandBars 0.2.8 (Oct 26, 2026)

## Fold (fresh dry run on a clean copy of 0.2.7 src)
- `rankup-fold-0.2.7.patch` applied with `-p2` to a fresh copy of `wandbars-027-20261026/src`; every hunk applied
  except two in `ExcelWandBars.java` (the `rankUp` field and its getter, context moved by 0.2.7's wand slot code),
  merged by hand. `build.sh` / `plugin.yml` version lines taken from 0.2.7 and set to 0.2.8.
- Full diff against 0.2.7: `ExcelWandBars-0.2.7-to-0.2.8.diff` (11 files).

## Unit tests
**1105 passed, 0 failed** (`evidence/unit-tests-028.log`): all 0.2.7 tests plus RankUpTest (ladder, earn math,
daily cap, bonus cap, text length, shipped config: enabled true, broadcast true, no seal cap, 800/1600/1800,
master shows as Master, no em dash, no "seal" in player text, the cheer text).

## Private rig (Paper, Magic 11.2.5, mcMMO, Jobs, ExcelTogether, two Java test bots; never the live server)
| Check | Result |
|---|---|
| Mend at full health | no SP ("no one was hurt") |
| Hurt, then Mend | +2 SP, bar `+2 SP \| Beginner 2/800` |
| Mend again within 60 sec (x5) | 0 SP each ("too soon") |
| Weekend bonus (mcMMO xprate 1.2) | `x1.2 weekend` |
| Party member near + weekend | `x1.5 (weekend + group)`, capped |
| Beginner to Student | rank-up via addsp + check |
| Student to Apprentice | by casting Flash Heal |
| Apprentice to Master | rank-up, titles `Student rank!` / `Apprentice rank!` / `Master rank!` |
| Server cheer | the other bot saw `<name> reached Student / Apprentice / Master rank!` |
| Wand item | still one item, bound; names Student Wand / Apprentice Wand / Wizard Wand (Magic's name) |
| Spell info | `Your rank: Master (top rank).` |
| Relog and full server restart | Master wand, 17 spells, SP and today's SP kept; boot `Enabling ExcelWandBars v0.2.8`, `Rank-up by casting ON.` |
| Party line overlap (ExcelTogether 0.4.1 and the live 0.4.2) | SP line waited about 2 sec after `Party: nobody in range` / `Party bonus ACTIVE`, waited 2, skipped 0 |

Evidence: `evidence/rig028.txt`, `overlap028.txt`, `overlap028-et042.txt`, `relog028-a.txt`, `relog028-b.txt`.

## Not verified
- Nickname display (no Essentials on the rig). The code uses the display name with colours and a leading `~` removed;
  falls back to the account name.
- A real Bedrock client (the cheer and title are plain chat/title, same path as Java through Geyser).
