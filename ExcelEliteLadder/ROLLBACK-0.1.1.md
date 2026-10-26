# Rollback ExcelEliteLadder 0.1.1 -> 0.1.0

Jar only (state, backups and the live MobCombatSettings.yml are untouched by the upgrade):
1. Rename plugins/ExcelEliteLadder-0.1.1.jar to ExcelEliteLadder-0.1.1.jar.failed-<yyyymmdd>.
2. Rename ExcelEliteLadder-0.1.0.jar.bak-<yyyymmdd> back to ExcelEliteLadder-0.1.0.jar.
3. Restart. `/eliteladder status` shows the same applied step. Seal 5 would apply spawn 0.11.

If S5 (0.12) was already applied by 0.1.1, 0.1.0 shows the file as OFF LADDER (spawn 0.12,
want 0.11) and changes nothing. To undo the step itself, see ROLLBACK-0.1.0.md (restore the
newest backups/...-from-S4.bak, `em reload`, set state.yml appliedStep).
