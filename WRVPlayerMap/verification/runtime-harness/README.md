# Disposable exact-Paper runtime harness

**Never install `WRVRuntimeHarness.jar` on a real server.** It creates synthetic map fixtures and deliberately shuts down its server when checks finish. This folder contains only original test source, configuration, runner, and evidence. It contains no Minecraft, Paper, JDK binaries, real player data, or worlds.

## Verified result

Final prototype JAR SHA-256:

`c3161b67ece29bdfeed00f4218ea1f7e47ac54ef0ff221c383ec18bf56b50e01`

The exact runtime and a subsequent clean restart each passed **8 test groups / 150 assertions** on 2026-10-12 UTC:

- Paper 26.2 build 123, commit `50018799f282010f7eb15d32c29cadbbe7c9d740`
- API `26.2.build.123-stable`
- Eclipse Temurin Java 25.0.4+7-LTS

The production source also compiled with Java 25 `javac --release 21` against this exact API. It emitted only deprecated-API notes.

## Coverage

1. Exact Paper/API versions and successful prototype enable.
2. Native `CraftMapRenderer` stays first, overlay stays contextual, and maps remain native/non-virtual.
3. Real `CraftMapCanvas` null clearing preserves underlying terrain pixels.
4. Registered map reuse across five scales, separate owners/worlds, idempotent attachment, unrelated vanilla maps untouched, and changed registered metadata refused safely.
5. Map ID and center survive a clean full server restart; overlay reattaches during plugin startup, before harness attachment code.
6. Initial/cache-reset frames clear all stale pixels; unchanged subsequent frames write zero pixels; removed marker pixels return to transparent.
7. Synthetic players with real server `ItemStack`/`MapMeta`: single issuance, repeated-command protection, selected-held-item priority, main/offhand detection, clear-on-unheld or permission removal, borrowed maps remain terrain-only, cross-world guard, world-switch ID reuse, cursor and replacement protection, and no allocation when inventory is full.
8. Visibility permission, `canSee`/vanish, and disconnection filtering prevent peer labels/positions from appearing in generated frames.

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
     --plugin target/WRVPlayerMap-0.1.0.jar \
     --annotations-jar /path/to/annotations-26.0.2.jar \
     --java-home "$JAVA_HOME"
   ```

The runner compiles the test harness, copies only the supplied prototype/harness into the disposable server, runs two complete boots, and requires a successful persisted-ID check on the second boot. Each boot writes a console log and SHA-256 manifest. A 180-second timeout requests a normal stop before attempting process termination. Restart fixtures are retained for reproducibility. Re-running against the same disposable folder reuses them.

## Evidence

The `evidence/` folder records the tested artifact hashes, the accepted boot/restart summary and compact final results. The first boot of a brand-new fixture prepares persistence state; only a later full boot establishes the restart claim.

## Limits

No real Java/Bedrock client, phone display, client packets, Geyser/Floodgate combination, or production plugin stack was exercised. Player and inventory objects are synthetic; map views, canvas, palette colors, item metadata, serialization, and server restart are real. The cloud JVM produced external Minecraft-authentication/Paper-update DNS warnings; local tests passed, but authenticated client connectivity is unverified.

The initial smoke harness expected a startup-banner version format while Bukkit returns a different format. Correcting that test expectation produced a pass without changing the prototype.
