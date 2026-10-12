# WRV Player Map prototype

A small, original Paper add-on that keeps Minecraft's native terrain renderer and adds player names and arrows as map **pixels**. It uses an ordinary filled-map item, so it does not depend on Java-only cursor captions or a browser map. No Cartographer code or assets are used.

This is a **separately tested prototype**, not an installed replacement for BlueMap. It never reads or deletes BlueMap files and has no BlueMap dependency, web service, port, resource pack or client-mod requirement.

## Use

- `/playermap` or `/playermap get`: receive one personal map, or reuse the one already carried. Put it in the **main hand on phones**. Java offhand holding is also recognized. No map is handed out automatically on join, death or reconnect.
- `/playermap scale 0` through `4`: select the five vanilla scales: 128, 256, 512, 1024 and 2048 blocks across. The held map is preferred when an older lost copy was later recovered. Default scale is 2.
- `/playermap list`: list visible online players and their worlds, including full names when the pixel display is crowded.
- North is up. Your marker/name is green and prefixed `*`; other names are yellow. In-area arrows show facing. Out-of-area arrows indicate the bearing from the **map center**, not the viewer.

The map uses a normal, fixed vanilla-aligned region centered on the area where that player's world/scale map is first requested. It does not continuously recenter or render unexplored faraway terrain. Each scale has its own ordinary exploration history, just like separate vanilla maps. All online players in that same world can have markers even when they are beyond the map's terrain coverage.

Names are placed around markers with leader lines. Coincident markers spread apart slightly, retaining a line to the actual position. The self arrow is drawn last. A 128×128 map has finite room: on unusually crowded maps, a footer reports hidden labels and label priority cycles every three snapshots (three seconds at the default update rate). All same-world markers remain eligible; `/playermap list` is the complete text fallback. Very long/unsupported glyphs are fitted or replaced with `?` rather than treated as formatting.

## Worlds, privacy and items

- When its owner holds it after changing worlds, the same item switches to that owner's dedicated map for that world and zoom. Returning reuses the prior ID and terrain. Other-world players are counted, not plotted at misleading converted coordinates.
- Personal maps borrowed by another player show ordinary terrain only. Normal/unrelated maps are never claimed or modified. Map frames are not a separate tracking display; dynamic rendering requires the corresponding personal map to be held.
- `wrv.playermap.use` defaults to true. Denying it blocks issuing and viewing overlays. `wrv.playermap.visible` defaults to true; denying it hides that player from other people's maps. Bukkit `canSee` is respected, so hidden players are excluded when the installed vanish plugin uses that API. The viewer always retains their own marker.
- Logout removes the position snapshot and cached viewer frame. Permission/vanish/world changes are reflected no later than the next snapshot, normally one second. Minecraft/client packet timing is additional.
- A full inventory or map on the cursor causes a clear retry message without dropping an item. Repeated commands reuse the carried map, including offhand. Replacements after deliberate loss reuse the stored ID and are rate-limited (default 60 seconds, minimum 10). This is not an absolute ban on multiple physical copies: storing/dropping one and deliberately requesting a later replacement can leave an old copy. No automatic item flooding or per-movement map-ID creation occurs.
- Crafting/cartography changes of tagged maps are blocked. Change zoom with the command so the ID registry stays consistent.

## Storage and performance

`plugins/WRVPlayerMap/maps.yml` records owner UUID, world UUID, scale, map ID and center. Back it up **together with the worlds' native map data**. Missing/changed map IDs, changed centers/scales and malformed registry data fail closed rather than claim unrelated maps. Writes are atomic where supported. A failed write blocks further allocations until the storage issue is repaired and the plugin restarted.

The number of registered IDs is bounded to one per owner × world × five scales. Disconnecting, moving and repeated requests do not allocate IDs. The plugin samples online player positions once per second by default, caches contextual frames and writes only changed pixels. It does not query blocks/chunks, generate chunks, scan worlds, keep chunks loaded or run its own terrain renderer. Normal vanilla exploration still has its usual cost. Unchanged frames at the same snapshot are reused.

A local synthetic test with twelve coincident players measured about 0.3 ms for overlay layout per render after warm-up. That excludes map packet transport, terrain updates and real host/client performance; it is not a 3 GB server TPS guarantee. Actual phone readability and the installed Geyser/Floodgate/vanish behavior still require client acceptance testing.

## Build and verification

From the repository root: `./mvnw -B -ntp -pl WRVPlayerMap -am clean verify`.

The regular reproducible reactor emits Java 21 bytecode against the repository's pinned Paper 1.21.4 API. Additionally, this prototype was compiled directly with Java 25 against the exact Paper 26.2 build 123 API and loaded/tested on Paper `26.2-123-main@5001879` with Temurin `25.0.4+7`. There is no NMS or Geyser API dependency.

The module's automated tests cover geometry/scales/negative coordinates, edge arrows, overlap/overflow, stale-pixel clearing, registry reuse/reload/corruption, normal-map isolation, inventory/cursor/offhand/repeated requests, owner permissions and deliberate replacement cooldowns. See `verification/` for the exact isolated-server harness, reproducibility details and explicit remaining client checks.

The intended live compatibility targets are Geyser `2.11.3-b1251` (`50a464d`) and Floodgate `2.2.5-SNAPSHOT` (`b141-81b65cc`). Pixel transport is the compatibility strategy; a real Java and Bedrock client have **not** yet visually verified this candidate.

## Staging before any live install

1. Make a disposable, private test server using the exact intended Paper, Java, Geyser and Floodgate builds. Install this candidate JAR there and preserve both world map data and the plugin registry across clean restarts.
2. Join with a real Java account and at least two Bedrock phones. Request maps, hold them in the main hand, travel inside/outside the map and compare labels/arrows with known player positions. Check every scale, matching and different dimensions, identical positions, long Floodgate names and a dark/bright terrain background.
3. Check logout, reconnect, server restart, death/loss, replacement cooldown, inventory full/cursor/offhand, permissions and actual vanish integration. Confirm non-player maps and their IDs/renderers remain unchanged.
4. Watch tick time/memory with the actual family player count for at least one gameplay session. No live installation, restart, BlueMap removal or permission changes are implied by building this prototype.
5. If removal is needed after a future authorized install, stop the test server, remove this plugin JAR, retain its registry/world map data for rollback and restart. Dedicated map items revert to their native terrain; no automatic world or map-data deletion is part of removal.

## API/source rationale

- [Exact installed Geyser translator source](https://github.com/GeyserMC/Geyser/blob/50a464d7db132ca5943b608a2110c158bf09702a/core/src/main/java/org/geysermc/geyser/translator/protocol/java/level/JavaMapItemDataTranslator.java) converts map pixel colors into Bedrock colors while supplying an empty decoration-caption string. This is why names are drawn as pixels. That source inspection does not replace a phone test.
- [Paper MapCanvas API](https://jd.papermc.io/paper/26.2/org/bukkit/map/MapCanvas.html) documents per-renderer layers and null color revealing the layer beneath. The exact build123 behavior was also checked on real CraftMapCanvas instances.
