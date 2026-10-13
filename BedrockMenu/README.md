# BedrockMenu

`/menu` opens native Geyser forms for Bedrock players and the DeluxeMenus
`server_menu` chest GUI for Java players. `/tpmenu` opens the player picker.

## Current source: 1.4.3

The readable original 1.4.1 Java sources have been recovered from repository
history and restored under `src/`. The 1.4.2 Spawn Shop addition is now ordinary
source code, so the normal build no longer rewrites old class files.

Buttons: Spawn, Teleport to a player, Spawn Shop (`/shop`), mcMMO Stats,
Leaderboard, Server Info, Auto-open on join. No Home/Set Home. `/shop`
matches the historical patch and the included DeluxeMenus menu's command; the
old README incorrectly called this `dm open spawnshop`.

The redundant Jobs & Money button and its `jobs browse` route were removed
from both ordered arrays. This source change does not disable Jobs or change
job enrollment. The separately prepared 1.4.2-menu.1 live-compatible hotfix
does not include the spawn repairs below. See `MENU_REMOVAL.md`.

### Spawn repairs

- Query the enabled EssentialsSpawn plugin's real `getSpawn(String)` method for
  the `default` spawn. The old reflection attempted a no-argument `getSpawn()`
  on the Essentials core instead.
- If that lookup cannot supply a location, keep the village's x=-784, z=-272
  fallback, one block above the highest block, in the first loaded NORMAL
  overworld. The player's current Nether/End world is never used for this
  fallback. This is a surface fallback, not a full terrain-hazard search.
- On servers with multiple overworlds, set `fallback-world` in
  `plugins/BedrockMenu/config.yml` to the village world. Blank preserves the
  first-NORMAL-world default. An unknown or non-overworld name fails safely
  with a player message. This option affects only the village fallback.
- Reject a fallback surface without room for the player's head at the world's
  build limit, check teleport success, and avoid executing a delayed menu action
  after its player disconnects. A cancelled teleport is never retried elsewhere.

Spawn retains the existing direct Bukkit teleport behavior; it does not add
Essentials command cooldowns, charges, or warmups. Teleport-to-player continues
using Essentials `/tpa`, `/tpaccept`, and `/tpdeny` under the player's identity.
Geyser/Cumulus remain reflection-only dependencies.

## Build and checks

Requires Maven and JDK 21. From the repository root:

```bash
mvn -pl BedrockMenu -am verify
```

Or run `./build.sh` here. The current plugin jar is built from `src/`, `plugin.yml`,
and `config.yml`. Unit tests in `tests/` cover the ordered native-menu button/command mapping,
spawn API selection, missing or disabled plugins, world selection, coordinates,
height limits, and failure paths. They do not replace a Paper + Geyser + Essentials staging-server test.

## Recovery material

These artifacts remain separate from the active source build:

- `classes-1.4.1/`: original 1.4.1 class files and descriptor.
- `decompiled-1.4.1/`: CFR 0.152 reference output, including its known invalid
  decompiler types. Do not build or edit this as the canonical source.
- `history/1.3.0-teleport-source/`: earlier teleport implementation.
- `tools/Patch.java` and `tools/plugin-1.4.2.yml`: original 1.4.2 ASM patch inputs.
- `tools/rebuild-1.4.2.sh`: historical payload reconstruction (JDK 21 and ASM
  9.8+; downloads pinned ASM 9.9.1 with Maven when `ASM` is unset); outputs under `build/recovery-1.4.2/`. ZIP metadata is not promised to
  match the earlier archive byte-for-byte.

See `SOURCE_RECOVERY.md` for provenance, comparison scope, and validation limits.
