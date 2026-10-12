# Disposable exact-Paper runtime harness

**Never install `WRVRuntimeHarness.jar` on a real server.** It creates synthetic map fixtures and deliberately shuts down its server when checks finish. This folder contains only original test source, configuration, runner, and evidence. It contains no Minecraft, Paper, JDK binaries, real player data, or worlds.

## Verified result

Final prototype JAR SHA-256:

`382e40bdb3eae37d2c5dc6b6344d57749701f4b589bcdfd77919aac2750b9dc9`

The exact runtime and a subsequent clean restart each passed **9 test groups / 111 assertions** on 2026-10-12 UTC:

- Paper 26.2 build 123, commit `50018799f282010f7eb15d32c29cadbbe7c9d740`
- API `26.2.build.123-stable`
- Eclipse Temurin Java 25.0.4+7-LTS

The production source also compiled with Java 25 `javac --release 21` against this exact API. It emitted only deprecated-API notes.

## Coverage

1. Exact Paper/API versions and successful prototype enable.
2. Native `CraftMapRenderer` remains first, overlay remains contextual, and maps stay non-virtual; real `CraftMapCanvas` null pixels restore native terrain.
3. Actual `PlayerMapFilledEvent` dispatch on a native-created ordinary map keeps its ID, center, scale, world, tracking flags and terrain bytes. The plugin only replaces the event result and never issues a second item. Unmarked existing maps and denied activations stay plain.
4. Native item cloning shares the map ID. Native zoom post-processing creates the normal new IDs through all five scales while preserving lineage, including multiple zooms before a held-map check. Unmarked existing maps' zooms remain untracked.
5. Native glass-pane lock post-processing creates its normal separate locked ID. It stays plain despite inherited metadata, and a byte comparison proves added labels were not baked into its locked terrain.
6. Native ID/item metadata/registry survive a clean full restart; the overlay reattaches lazily before the explicit held-map check and no new registry entry is created.
7. Initial/cache-reset frames clear stale pixels, unchanged frames write zero pixels, and removed markers return to transparent.
8. Synthetic players with real server items/map views verify sharing without owner binding, main/offhand versus stored items, permission clearing, fixed map world/ID across dimensions, and shared-ID identity of held/framed copies.
9. Added overlay labels/markers respect visibility permission, `canSee`, and online status. Native Minecraft cursor behavior is preserved and is not hidden by this add-on's visibility permission.

## Reproduce safely

Requirements: Python 3, a Java 25 JDK (Temurin 25.0.4+7 for an exact match), the built prototype JAR, and a **new disposable server folder**. Use no production world or other production plugins.

1. Obtain the exact official Paper artifact from the [Paper build metadata](https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/123). Save it as `paper-26.2-123.jar` in the disposable folder. Verify SHA-256:

   `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`

2. Copy `server.properties.loopback` to that folder as `server.properties`. It binds only `127.0.0.1:25578`, enables authentication and a whitelist, and disables RCON/query. The runner checks these settings and rejects extra plugin JARs.

3. Set `JAVA_HOME` to the Java 25 JDK. In the disposable folder, run official Paperclip's preparation-only mode to download/patch the server and extract runtime libraries without starting Minecraft:

   ```sh
   "$JAVA_HOME/bin/java" -Dpaperclip.patchonly=true -jar paper-26.2-123.jar
   ```

   If Java cannot reach the official Mojang download endpoint, `META-INF/download-context` inside the verified Paper JAR provides the exact official Mojang URL, expected SHA-256, and cache filename. A standard HTTPS download to `cache/<filename>` with that checksum is equivalent. Do not use a third-party server mirror.

4. Read the [Minecraft EULA](https://www.minecraft.net/en-us/eula). Establish or confirm your acceptance before creating `eula.txt` with `eula=true`. This runner will never create acceptance for you.

5. Obtain JetBrains annotations 26.0.2 from your Maven cache or [Maven Central](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar). The runner verifies its official SHA-256:

   `2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297`

6. Run from any directory with explicit paths:

   ```sh
   python3 verification/runtime-harness/run.py \
     --server-dir /path/to/disposable-server \
     --plugin target/WRVPlayerMap-0.2.0.jar \
     --annotations-jar /path/to/annotations-26.0.2.jar \
     --java-home "$JAVA_HOME"
   ```

The runner compiles the test harness, copies only the supplied prototype/harness into the disposable server, runs two complete boots, and requires a successful persisted-ID check on the second boot. Each boot writes a console log and SHA-256 manifest. A 180-second timeout requests a normal stop before attempting process termination. Restart fixtures are retained for reproducibility. Re-running against the same disposable folder reuses them.

## Evidence

The `evidence/` folder records the tested artifact hashes, the accepted boot/restart summary and compact final results. The first boot of a brand-new fixture prepares persistence state; only a later full boot establishes the restart claim.

## Limits

No real Java/Bedrock client, phone display, UI inventory/crafting clicks, client packets, Geyser/Floodgate combination, or production plugin stack was exercised. Players/inventories are synthetic. Event dispatch, native map creation and clone/zoom/lock post-processing, map views/canvas, item metadata, serialization and full restarts are real. Shared frame identity is checked server-side, not through a client screenshot. The cloud JVM produced external Minecraft-authentication/Paper-update DNS warnings; local tests passed, but authenticated client connectivity is unverified.

This harness intentionally imports exact-version server internals to verify native map operations. The runner includes the patched server JAR in its test classpath. The production plugin itself has no NMS dependency. Both final runs use the same SHA-pinned 0.2.0 candidate, including the registry and lineage-hardening fixes.
