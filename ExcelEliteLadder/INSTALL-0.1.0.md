# Install ExcelEliteLadder 0.1.0 (NOT DONE; needs the owner's go and an empty-server window)

PRECONDITION (hard): live plugins/EliteMobs/MobCombatSettings.yml must equal S1-norm
(sha 57ad4ab2c75fd8c68508aafdc8e27cc3601afb454f7295958e893c19c45c976d: S1 values plus dungeon 0.5/2.0)
and GuildGate must show 1 seal. If live is the old S0 (2861a26d...) or the old S1 (b5cb1d7b...),
the plugin refuses, logs why, and does nothing. Nothing breaks, but the ladder will not move.
So install the S1-norm swap first (or in the same window, before the restart).

Also needed: ExcelTrainingWheels 0.1.6 (draft PR #69). Live 0.1.5 has a stale row 5 (2.35).

Steps:
1. Server empty (two reads, 0 players). Take the usual dated backup of plugins/EliteMobs/MobCombatSettings.yml.
2. Check the live MobCombatSettings.yml sha = 57ad4ab2... (S1-norm).
3. Upload ExcelEliteLadder-0.1.0.jar (sha in SHA256SUMS.txt) to plugins/. No config needed; the
   default config.yml is created on first start (allow-reload-with-players-online: false).
4. Restart (or wait for the nightly restart).
5. Console must show:
   - "ExcelEliteLadder enabled: applied step none (no verified baseline) ... waits for an empty server"
   - about 5 s later: "GuildGate seals observed: 1 of 5" and
     "Baseline adopted: live MobCombatSettings.yml equals ladder S1-norm ..."
6. `/eliteladder status` shows: Current seals 1 of 5, Applied step S1, Live file S1-norm, Pending none.

After install, each seal break gives "Seal N is broken: elite step SN is pending" (if players are on),
then "EliteLadder applied SN ..." when the server empties, then "Post-reload check SN: ... match".
If seals were already 2+ at install, the step applies at the first empty moment (boot counts).

After any S2+ apply, the live sha is the "EM form" sha in SHA256SUMS.txt (EliteMobs rewrites number text).
