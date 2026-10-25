# ExcelClaimFly

Build Flight pass for Wink Reyes Valley. A timed pass bought in the Spawn Shop (Travel & Animals, $1,000 for 30 minutes)
lets a player fly only inside their own GriefPrevention claim. The pass is a LuckPerms temporary permission, so it keeps
counting while offline and survives restarts. Leaving the claim, a fight (any mob, projectile or player hit) or the pass
ending turns flight off, and the player lands with no fall damage. Works for Bedrock players through Geyser/floodgate
(double-tap jump; tappable confirm form).

- Build: `./build.sh` (JDK 25, `--release 21`, fixed jar date, deterministic; runs the 35 unit tests).
- Player: `/buildfly` (time left), `/buildfly help`, `/buildfly buy`. Alias `/claimfly`.
- Admin (op): `/buildfly give <player> <min> | end <player> | info <player> | confirm <player> | reload`.
- Depends on: Vault, LuckPerms, GriefPrevention. Soft: floodgate.
- `server-config/`: the Spawn Shop button, the rotation tip and the What's New entry that ship with it (ExcelMenu, ExcelTips, ExcelWhatsNew).
- `rig-tests/`: the box test-server scripts (Java bots, Bedrock protocol client, tip + What's New).

Status: staged, NOT installed. See CHANGELOG-0.1.0.md, INSTALL-0.1.0.md, ROLLBACK-0.1.0.md, RIG-TEST-RESULTS-0.1.0.md.
