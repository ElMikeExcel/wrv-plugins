# Sneak / right_click_sneak on Wink Reyes Valley: what the box notes say

* **Where it is configured:** Magic (elmakers) `plugins/Magic/wands.yml` under the
  `base_wand` controls. The same file is copied by ExcelMagicGate from
  `plugins/ExcelMagicGate/stages/stage1-5/wands.yml` at every seal.
  The live classic set has:
  - `mode: chest`, `left_click: cast`, `right_click: cycle`
  - `right_click_sneak: toggle` (sneak + right click opens the chest spell list)
  - `drop: cycle_reverse`, `drop_sneak: toggle`
  - `swap: cycle`, `swap_sneak: cycle_reverse`
* **Oct 23, 2026, 7:32 PM CDT (WRV-150):** chest-mode fix live. Before it, base_wand ran
  inventory mode, which could not be used on a Bedrock controller.
* **Oct 23, 2026, 7:44 PM CDT:** quick-cycle v2 live. Right click jumps to the next spell,
  and sneak + right click opens the list. Draft PR wrv-plugins #51.
* **Oct 23, 2026, ~11:03 to 11:17 PM CDT:** Bedrock casting plan
  (/workspace/magic-heal-plan/BEDROCK-CASTING.md). It suggested the action-bar wand
  (inventory mode + quick_cast) and noted that the controller player keeps sneak + right
  click cycling. Not live as a default.
* **ExcelWandBars 0.1.0** (install record batch-next/wand-style-optin/src/ExcelWandBars/INSTALL-RECORD-0.1.0.md)
  added the sneak bar flip for inventory mode. **0.2.0**
  (batch-next/wand-style-optin/INSTALL-RECORD.md, ship-20261025) added `/wandstyle`,
  with Classic (chest) as the default and Spell bars as an opt-in. In bars,
  right_click_sneak = cycle.
* **Live now:** since Oct 24, 2026, 11:37 AM CDT (SHIP-20261025): ExcelWandBars 0.2.0 and
  the classic wands.yml set (sha 1a9accf2...) for Magic and stages 1-5. Everyone is on
  Classic unless they typed `/wandstyle bars`. In 0.2.0, bars is allowed on Java too.
  The Route Sheet export /workspace/rec_noab/routesheet_B2_before_20261024.txt has no sneak or wand-style rows.
* **This batch (0.2.1, staged):** does not change wands.yml. Classic keeps
  right_click_sneak = toggle (the list). Sneak bars is Bedrock only, and the flip works
  only while the bar is open.
