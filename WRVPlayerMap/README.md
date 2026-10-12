# WRV Player Map 0.2 prototype

An enhancement to **ordinary Minecraft maps**: use a new empty map normally, then see nearby players' names and distant-player arrows drawn as pixels over its native terrain. This replaces the earlier command-issued personal-map prototype. It remains isolated and is **not installed on the live server**.

## Normal player instructions

1. Craft or acquire an ordinary empty map and use it normally. Newly activated maps gain tracking automatically; no special command or extra map item is needed.
2. Hold the filled map in your **main hand on phones**. Java offhand holding is recognized too. Explore normally: the map retains its real vanilla ID, region, scale, world and terrain history.
3. Share the map or copy it normally. At a cartography table, paper zooms out, an empty map makes a copy, and a glass pane makes a locked, plain map for walls/art. The add-on does not replace these recipes, consume extra materials or create extra map IDs.
4. Optional: `/playermap list` lists visible online players and their worlds. `/playermap` shows help. The former `/playermap get` and `/playermap scale` controls no longer issue or modify anything.

North is up. Your added name/arrow is green and prefixed `*`; other added names are yellow. In-area arrows show facing. Off-map arrows point toward the player from the **map center**. Vanilla cursors, banners and other native decorations remain as they normally would.

Each map stays with its original world and region. Taking an Overworld map through a portal does not turn it into a Nether map. In another dimension, it keeps its ordinary terrain image without misleading live player coordinates. Activate a separate ordinary map there if you want a map of that world; normal Minecraft dimension-specific terrain behavior still applies.

## What stays plain

- Maps that already existed when this feature was enabled are not automatically adopted, including their normal untracked zooms/copies.
- Treasure/explorer maps are not enrolled by merely being created, loaded, held or traded.
- Locked maps always stay plain. Locking an improved map creates Minecraft's normal separate locked ID and captures its terrain only; the player-label layer is not baked into the art.
- Custom/virtual maps and activation outputs with custom names, lore, colors, location names or plugin data are skipped conservatively.

**Map-wall caveat:** Minecraft copies share a map ID and therefore one image per viewer. An **unlocked** framed copy can show the same overlay while that viewer holds another copy with the same ID. Independently styled held/frame copies are not promised. Use a glass-pane locked copy for a reliably plain wall/art display. Existing map walls remain outside automatic enrollment.

## Tracking, permissions and crowding

All visible online players in the map's world can be drawn, including players far beyond its terrain coverage. Other-world players are counted separately; `/playermap list` gives their names/worlds without inventing converted coordinates. No remote terrain is generated.

Coincident markers spread slightly with lines back to their real positions. The self arrow is drawn last. On crowded 128×128 maps, a footer reports hidden labels and name priority cycles every three position snapshots (normally three seconds). `/playermap list` is the full-name fallback. Long/unsupported glyphs are fitted or replaced with `?` safely.

- `wrv.playermap.use` (default true): activate and view the enhancement. Denying it leaves ordinary map behavior available.
- `wrv.playermap.visible` (default true): appear in the **added pixel names/markers**. This does not suppress Minecraft's own cursor when vanilla would show it, such as another holder of the same map. It is not a complete vanish permission.
- Bukkit `canSee` is respected by the added overlay. Exact Paper's native renderer also respects `canSee`; test the installed vanish plugin rather than assuming it uses that API.

Positions refresh once per second by default. Logout is removed from snapshots, and stale pixels clear transparently. Client packet timing is additional. Normal vanilla inventory handling, map consumption, dropping and crafting semantics are retained; no command/join/reconnect grant or automatic item replacement exists.

## Narrow scope and storage

Enrollment uses Paper's `PlayerMapFilledEvent`, specifically empty-map activation. It captures the vanilla-created identity early and refuses changes to that identity before applying metadata. `MapInitializeEvent` **only restores IDs already known**; it never globally converts maps. A lineage tag copied by vanilla recipes allows only validated native zoom descendants of a known activated map to enroll when held. Normal copies reuse the same ID. Locked or unmarked items never activate an overlay.

As with other server extensions, installed plugins are trusted. A plugin that deliberately substitutes an old plain map *before* this add-on's earliest activation listener cannot be distinguished from Paper's original result through that event alone. The checks reject later replacements and obvious custom outputs; coexistence with another map-activation replacement plugin requires integration testing. This is not a sandbox against privileged plugins forging metadata.

Back up `plugins/WRVPlayerMap/maps.yml` **with the worlds' native map data**. Format 2 records ID/world/center/scale/root ancestry without ownership. Malformed or impossible ancestry and changed registered identities fail closed. Writes are atomic where supported; a write failure stops further enrollment until storage is repaired and the plugin restarted. Ordinary maps continue working.

The old isolated 0.1 owner-map registry is deliberately not imported automatically. Preserve it separately if evaluating an upgrade of that unpublished-to-production prototype; never delete native world map data as a migration shortcut.

The plugin creates **zero map IDs**. Entries follow maps that vanilla itself creates through activation/zoom. It samples only online player positions and held items, caches contextual frames and writes changed pixels. It does not scan world files, blocks/chunks/entities, generate chunks, force chunks to stay loaded, render terrain, use BlueMap data or expose a web port. Startup does not enumerate/load every registered map; views reattach lazily as used. Normal vanilla terrain work remains normal vanilla work.

## Build, verification and remaining checks

From repository root: `./mvnw -B -ntp -pl WRVPlayerMap -am clean verify`.

This module alone pins Paper API `1.21.5-R0.1-20250925.065803-26`, which contains `PlayerMapFilledEvent` and keeps the existing Java 21 reactor usable. The other modules retain their API pins. The candidate is also compiled/tested against the exact deployed Paper `26.2-123-main@5001879`, API `26.2.build.123-stable`, Temurin `25.0.4+7`. The production plugin has no NMS or Geyser API dependency.

See `verification/runtime-harness/` for original, reproducible isolated tests and final artifact hashes. Tests use actual native map creation, clone/zoom/lock post-processing, real item metadata/map canvases and clean server restarts, with synthetic players. They verify native ID/terrain/flags, ancestry, sharing, locked plain copies, no overlay baked into locked art, permissions, stale pixels and dimensions.

**Still unverified:** real Java and Bedrock phone display, UI clicks/shift-click crafting/inventory packet synchronization, actual Geyser `2.11.3-b1251` (`50a464d`) / Floodgate `2.2.5-SNAPSHOT` (`b141-81b65cc`) together, installed vanish integration, and live-host tick/memory performance. No headless test is presented as phone acceptance.

Before any authorized live installation, use an isolated staging session with Java and at least two Bedrock phones. Activate an ordinary map; share/clone/zoom/lock it; visit another region and dimension; test main/offhand, overlaps, long names, reconnect, full restart, and existing treasure/art maps. Specifically check an unlocked framed copy while holding its twin, then a locked copy. Observe a normal family gameplay session for readability and performance. Removal only needs the plugin JAR removed during an authorized stop/restart: maps retain vanilla terrain; preserve registry/world data for rollback.

## Primary-source rationale

- [Exact Paper empty-map hook](https://github.com/PaperMC/Paper/blob/50018799f282010f7eb15d32c29cadbbe7c9d740/paper-server/patches/sources/net/minecraft/world/item/EmptyMapItem.java.patch) supplies the native-created item through [PlayerMapFilledEvent](https://github.com/PaperMC/Paper/blob/50018799f282010f7eb15d32c29cadbbe7c9d740/paper-api/src/main/java/io/papermc/paper/event/player/PlayerMapFilledEvent.java).
- [Exact installed Geyser translator](https://github.com/GeyserMC/Geyser/blob/50a464d7db132ca5943b608a2110c158bf09702a/core/src/main/java/org/geysermc/geyser/translator/protocol/java/level/JavaMapItemDataTranslator.java) forwards map pixels but supplies empty decoration captions, which is why added names use pixels.
- [Paper MapCanvas API](https://jd.papermc.io/paper/26.2/org/bukkit/map/MapCanvas.html) provides transparent renderer layers. Exact build 123 behavior is tested with real native canvas instances.

All add-on and test code is original; no Cartographer code/assets are reused. This work does not perform a live install, BlueMap removal, port change or production restart.
