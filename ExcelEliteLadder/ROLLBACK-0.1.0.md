# Rollback ExcelEliteLadder 0.1.0

Remove the plugin only (keeps the current elite step):
1. Delete plugins/ExcelEliteLadder-0.1.0.jar. Restart. MobCombatSettings.yml stays at whatever step
   was applied. Ladder steps go back to manual swaps.

Undo one ladder step (server empty):
1. Pick the newest plugins/ExcelEliteLadder/backups/MobCombatSettings.yml.<stamp>-from-S<n>.bak.
   It is the exact file from before that step.
2. Copy it over plugins/EliteMobs/MobCombatSettings.yml and run `em reload` (or restart).
3. Set plugins/ExcelEliteLadder/state.yml appliedStep to <n> (or remove the jar first), or the plugin
   will see seals > applied and step up again at the next empty moment.
   With the jar installed and appliedStep left higher than the file, it refuses (drift) and changes nothing.

Full rollback to the pre-ladder file: use the dated .bak from the S1-norm install window
(old S1 b5cb1d7b... or old S0 2861a26d...), `em reload`, and remove the jar.

Never delete the backups folder. Backups are never overwritten.
