# Disposable exact-Paper XP runtime harness

Never install `WRVXpRuntimeHarness.jar` on a real server. It creates synthetic players and deliberately stops its isolated server after testing. Only test source and compact evidence are committed here; no Minecraft/Paper/JDK binaries, worlds, player data, or executable harness JAR are included.

## Verified result

The unchanged mcMMO `2.3.003-WRV-XP-2` candidate, SHA-256 `5f6279995c7b30240d2a7e07cb916b6e28b82537826f9ef0f2702b080cf20978`, passed 38 runtime assertions with unchanged WeekendXP `1.0.2` on:

- Paper 26.2 build 123, API `26.2.build.123-stable`
- Eclipse Temurin Java 25.0.4+7-LTS
- An isolated authenticated, whitelisted server bound only to `127.0.0.1:25581`, with query/RCON/management endpoints disabled

Native Paper `CraftBossBar`, mcMMO profiles, the plugin scheduler, command handling, custom XP events and `PlayerExpChangeEvent` dispatch are exercised. Synthetic `CraftPlayer`/`ServerPlayer` objects have in-memory connections whose packet sends are suppressed. They do not connect real clients or authenticate accounts.

## Coverage

- Both plugin versions enable, vanilla bridge support is confirmed, and Monday's WeekendXP reset is applied.
- Join/proximity/show/status create no indicator; real stored positive fractional skill XP does.
- Repeated gains reuse one native bar; it remains through tick 59 and expires at tick 60 after the latest gain.
- Eligible native-source vanilla XP events are observed next tick; repeated orb events refresh the same deadline.
- The Together skill factor still composes with the weekend/global factor; two one-point vanilla events at the combined 1.50 factor pay 1 then 2 points.
- True zero, cancelled pre-events, final-zero skill awards, later zeroed vanilla events and excluded player-death sources do not independently display a bar.
- Hide suppresses both channels; a fresh manager reads the stored preference; show waits for a new gain.
- World-change/quit callbacks clear active or pending notifications; shutdown clears bars, pending events and the timer.

## Fixture nuance

The first fixture run used mcMMO's normal `EarlyGameBoost.Enabled: true`. Upstream mcMMO adds a positive early-game bonus even when the raw input is zero, so that was a real positive stored award and correctly displayed a notification. The fixture now sets `EarlyGameBoost.Enabled: false` solely in its disposable `plugins/mcMMO/experience.yml` to isolate true zero awards. No production setting or candidate code was changed. Together is enabled in the disposable config with the live multiplier/range/member settings; diminished returns remain disabled. The harness switches the event rate to 1.2 only in this isolated process for its multiplier assertions.

## Reproduce

Use an already provisioned disposable Paper folder with the exact official build's cache, libraries and patched server. Use the already accepted owner's EULA only where applicable; the runner neither creates acceptance nor downloads anything. Do not copy production worlds or player data.

Prepare `server.properties` with the isolation settings enforced by `run.py`, `level-name=xp-harness-world`, and `pause-when-empty-seconds=-1`. Install only the SHA-pinned XP-2 candidate and unchanged WeekendXP 1.0.2. In the disposable mcMMO config, enable Together (1.25, 32 blocks, minimum 1 other member); set `EarlyGameBoost.Enabled: false` in experience.yml. Existing normal mcMMO configs must have been generated already. Provide Java 25.0.4+7 and JetBrains annotations 26.0.2 from verified existing assets.

Run:

```sh
python3 run.py --server-dir /path/to/disposable-xp-server \
  --java-home /path/to/jdk-25.0.4+7 \
  --annotations-jar /path/to/annotations-26.0.2.jar
```

The runner verifies exact artifact hashes, the loopback endpoint, whitelist/authentication, plugin allowlist and prior EULA state. It compiles the test-only harness against the exact runtime API and candidate, starts the isolated process, waits up to 180 seconds, and stops that process on timeout. Normal completion stops the server itself and confirms that the port closed. Run again to check another complete boot.

## Limits

No real Java/Bedrock client, Geyser/Floodgate, production permissions/region plugins, authenticated connectivity, actual orb collision/Mending equipment, or client rendering was exercised. Native orb-source objects and final post-Mending-style event amounts are supplied by the fixture; this is not a full gameplay/Mending integration test. Preference persistence is checked in actual player persistent data within the synthetic session, not by an authenticated reconnect. The real Java/Bedrock player acceptance checklist remains required.

Cloud JVM authentication-key and Paper-update lookups emitted external DNS warnings. No workaround was used; local runtime assertions passed. Clean server shutdown and closed loopback listener are recorded in `evidence/exact-runtime.json`.
