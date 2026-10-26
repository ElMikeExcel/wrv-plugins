# Install ExcelEliteLadder 0.1.1 (NOT DONE; needs the owner's go)

Replaces live 0.1.0 (Oct 25, 2026, about 11:18 PM CDT). Only change: Seal 5 spawn 0.11 -> 0.12.
Not urgent: the jar only matters when Seal 5 breaks (live is 1 of 5 seals). It must be in before
Seal 5 breaks. Any planned restart works (the nightly 2:00 to 5:00 AM CDT window is fine).

## What the live change takes
- One jar swap in plugins/ plus one server restart. Nothing else.
- No change to plugins/EliteMobs/MobCombatSettings.yml, no `em reload`, no combat change today.
- Do NOT touch plugins/ExcelEliteLadder/ (state.yml, config.yml, backups/, apply-log.txt).
- A plugin jar cannot be swapped with a reload; it needs the restart. Players: the restart
  itself is the only interruption (usual countdown if anyone is on).

## Drift guard (before the swap, read only)
1. `/eliteladder status` (console): Applied step S<n> with n below 5, Live file S<n>-norm,
   Pending none, no failure latch. If Live file shows OFF LADDER or a latch: STOP and report.
2. Live plugins/ExcelEliteLadder-0.1.0.jar sha256 = ee98604a43f761dab6007e24d6446ffe22e4894ee2a631b968cdd1ed20f07bdc
   (0.1.0 and 0.1.1 are both 31,330 B, so size alone cannot tell them apart; use the sha,
   or `version ExcelEliteLadder` must say 0.1.0). Different: STOP.
3. Live plugins/EliteMobs/MobCombatSettings.yml sha256 = the step's EM-form sha
   (S1 57ad4ab2..., S2 9b4c3f95..., S3 983fa757..., S4 95aface7...). Note it; it must be the same after.

## Steps (panel File Manager, plugins/)
1. Upload ExcelEliteLadder-0.1.1.jar under the temp name ExcelEliteLadder-0.1.1.jar.uploading.
   Confirm sha256 f7ec4c1ff86487e80a992ce1a982151ec8900ceab482b8ca46c94b42bdf71521
   (31,330 B, the same size as 0.1.0, so the sha is the real check; after the restart
   `version ExcelEliteLadder` must say 0.1.1).
2. Rename the live ExcelEliteLadder-0.1.0.jar to ExcelEliteLadder-0.1.0.jar.bak-<yyyymmdd>
   (the .bak keeps Paper from loading it; two jars with the same plugin name would clash).
   Never overwrite or delete an existing .bak.
3. Rename ExcelEliteLadder-0.1.1.jar.uploading to ExcelEliteLadder-0.1.1.jar.
4. Restart (or wait for the nightly restart).
5. Console must show:
   - "ExcelEliteLadder enabled: applied step S<n>, watching ExcelGuildGate seals every 30s ..."
   - about 5 s later "GuildGate seals observed: <n> of 5"
   - no "Baseline adopted" line is needed (state carries over), no "EliteLadder applied" line.
6. `version ExcelEliteLadder` -> 0.1.1. `/eliteladder status` -> same Applied step and Live file as
   before, Pending none.
7. MobCombatSettings.yml sha256 unchanged from drift-guard step 3.

## If Seal 5 already broke under 0.1.0 (live S5 with spawn 0.11, sha 89fd7435...)
0.1.1 will show "Live file: OFF LADDER (... eliteMobsSpawnPercentage=0.11 (want 0.12))" and do
nothing. Fix by hand with the server empty: dated backup of MobCombatSettings.yml, change line 6
`eliteMobsSpawnPercentage: 0.11` to `0.12` (or upload expected/S5-norm-EMform-0.12-MobCombatSettings.yml,
sha 49763c61...), `em reload`, then `/eliteladder status` shows Live file S5-norm.

## Rollback
1. Rename ExcelEliteLadder-0.1.1.jar to ExcelEliteLadder-0.1.1.jar.failed-<yyyymmdd>.
2. Rename ExcelEliteLadder-0.1.0.jar.bak-<yyyymmdd> back to ExcelEliteLadder-0.1.0.jar.
3. Restart. Status shows the same step; Seal 5 would then apply 0.11 again.
