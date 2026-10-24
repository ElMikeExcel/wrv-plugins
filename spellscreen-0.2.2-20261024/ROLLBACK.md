# Rollback: ExcelWandBars 0.2.2 -> 0.2.1 and wands.yml back to 5 rows

Needs a restart (same rule: reason, 1 minute, 10-second countdown).

1. Rename plugins/ExcelWandBars-0.2.2.jar to `ExcelWandBars-0.2.2.jar.off-20261024` (never a name that loads).
2. Rename `ExcelWandBars-0.2.1.jar.bak-20261024` back to `ExcelWandBars-0.2.1.jar`.
3. Copy each `wands.yml.bak-20261024` back over its wands.yml:
   plugins/Magic/wands.yml and plugins/ExcelMagicGate/stages/stage1..5/wands.yml.
   (Hashes after: Magic + stage1 1a9accf2..., stage2 3dc1bf99..., stage3 a8afb0d8...,
   stage4 e5165a7e..., stage5 fb5a8896...)
4. Restart per the rule.

What players notice after a rollback (tested on the rig, phase "rollback"):
* The spell list is 5 rows again. Spells in slots 0 to 44 stay where they were. A spell a
  player put in row 6 is still on the wand; Magic shows it on page 2.
* Bedrock Classic players get Magic's chest again (with the barrier row).
* `/spellbook` works as in 0.2.1, with no rank header.
* plugins/ExcelWandBars/spellscreen-tips.yml stays and is ignored by 0.2.1 (harmless; may be deleted).
* The Bedrock wand tip goes back to the 0.2.1 "right-click / left-click" wording.

Only the wands.yml part (step 3) can also be undone without a restart with `magic load`
(Bedrock players rejoin after a Magic reload).
