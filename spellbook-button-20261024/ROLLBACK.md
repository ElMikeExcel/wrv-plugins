# Rollback (needs a restart, same restart rule: reason, 1 minute, 10-second countdown)

1. Remove or rename ExcelWandBars-0.2.1.jar, ExcelBedrockMenu-1.5.18.jar and
   autosort-0.1.17+26.3-paper.jar. Rename them to `*.jar.off-20261024`, never to a name
   that loads.
2. Rename ExcelWandBars-0.2.0.jar.bak-20261024 back to ExcelWandBars-0.2.0.jar and
   ExcelBedrockMenu-1.5.17.jar.bak-20261024 back to ExcelBedrockMenu-1.5.17.jar.
3. Copy plugins/ExcelMenu/menus/server_menu.yml.bak-20261024 over server_menu.yml. This
   step alone would also work with `excelmenu reload`.
4. Copy plugins/ExcelMenu/menus/teleport_confirm_spawn.yml.bak-20261024 over
   teleport_confirm_spawn.yml (compass Spawn goes back to the old fixed village spot).
   This step alone also works with `excelmenu reload`.
5. Restart per the rule.

What players notice after a rollback:
* The Spellbook, Wand controls and Auto-sort buttons are gone.
* WandBars 0.2.0 reads a saved `bars` choice as "Spell bars" on ANY platform, Java
  included, the same as live 0.2.0 today. `/wandstyle classic` fixes it for that player.
* There is no wand tip. plugins/ExcelWandBars/tips.yml is ignored and can stay.
* Auto-sort stops. plugins/autosort/active.txt can stay or be deleted. It does nothing
  without the jar.

Tested on the box test server with phase "rollback": 0.2.0, EBM 1.5.17 and the old menu
went back, the server booted clean, and the saved bars choice applied under 0.2.0.
`/wandstyle classic` restored Classic, and `/wandstyle ok` gave 0.2.0's usage line.
After that the new set was restored and booted clean again.
