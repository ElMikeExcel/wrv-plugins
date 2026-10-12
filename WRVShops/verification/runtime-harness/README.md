# Disposable exact-runtime shop harness

**Never install any harness JAR on a real server.** The fixture creates synthetic players, an in-memory fake Vault economy, fake claim records, test chests and signs, and shuts down its server when checks finish. This folder is source, configuration, runner and concise evidence only. Do not commit binaries, worlds, logs, account data or the sibling disposable runtime folder.

## Runtime and scope

- Exact Paper 26.2 build 123, commit `50018799f282010f7eb15d32c29cadbbe7c9d740`
- Paper API `26.2.build.123-stable`
- Eclipse Temurin Java `25.0.4+7-LTS`
- Official VaultAPI 1.7 interfaces, with an original in-memory test provider
- Original synthetic claim provider exposing the public GriefPrevention API used by WRVShops
- Loopback only `127.0.0.1:25579`; online authentication and whitelist required; RCON/query/management server disabled

The synthetic players use native Paper inventories and actual `ItemStack` metadata. A test-only delegating `Server` proxy permits WRVShops to resolve their UUIDs without network clients. World blocks, chest storage, plugin loading, service lookup, scheduled callbacks, event dispatch, serialization and clean server restarts run on exact Paper.

This does not validate real Java/Bedrock client rendering, phone layouts, client packets, Geyser/Floodgate, the actual installed economy/GriefPrevention versions, player-save persistence, production permissions or another plugin's interference. No real player accounts or purchases are involved.

## Safe reproduction

Requirements: Python 3, exact Java 25 JDK, built `WRVShops` JAR and a new disposable Paper directory.

1. Obtain the exact official Paper artifact using [Paper build metadata](https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/123), with SHA-256 `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`. Name it `paper-26.2-123.jar`.
2. Copy `server.properties.loopback` into the disposable folder as `server.properties`. Never use a production folder or world.
3. Run official Paperclip patch-only preparation: `$JAVA_HOME/bin/java -Dpaperclip.patchonly=true -jar paper-26.2-123.jar`. This extracts pinned server runtime/libraries. Existing verified binaries from a prior disposable test can instead be copied into a new directory; do not reuse its worlds, plugins or player data.
4. Establish acceptance of the [Minecraft EULA](https://www.minecraft.net/en-us/eula) before supplying `eula.txt` containing `eula=true`. The runner never creates acceptance.
5. Supply the already-resolved official dependency artifacts: VaultAPI 1.7 (SHA-256 `47f6a400a8ab22012e6a320f7bfc63621ce4a246e1c60502ff7fc91d532d2601`) and JetBrains annotations 26.0.2 (SHA-256 `2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297`). Obtain them through the repository's declared dependency sources.
6. Run:

```sh
python3 verification/runtime-harness/run.py \
  --server-dir /path/to/new-disposable-server \
  --plugin target/WRVShops-0.1.0.jar \
  --vault-api /path/to/VaultAPI-1.7.jar \
  --annotations-jar /path/to/annotations-26.0.2.jar \
  --java-home "$JAVA_HOME"
```

The runner verifies dependency hashes and isolation settings, rejects unexpected plugin JARs or a port already in use, compiles original test providers/harness, and performs four complete boots: normal startup, a clean restart, missing Economy service, and Vault plugin/API entirely absent. Each run writes a manifest of artifact hashes and a console log only under the disposable server. A bounded timeout requests clean stop of the process it started, then terminates only that child if needed. It never finds or stops another server.

Provider fixtures are deliberately named `Vault` and `GriefPrevention` so WRVShops exercises its actual declared dependency and reflection paths. They are clearly marked `0.0.0-test-only`, are not substitutes for the real plugins, and must never be distributed as production dependencies.

## Evidence

Final candidate SHA-256: `16de2e1477f561987717412b4c338eda136cd1025fad85e3c692ad29fc663aaa`.

The accepted four-boot run on 2026-10-12 UTC passed:

- 35 groups / 240 assertions on the normal boot
- 35 groups / 240 assertions after a full clean restart, including persisted registry, exact metadata, physical stock and clean checkout-journal state
- 7 assertions in a separate full boot with no registered Economy service
- 11 assertions in a separate full boot with the Vault plugin and API classes absent

The `evidence` directory contains final accepted result summaries, artifact manifests and shutdown/port verification. All four child server processes exited normally; loopback port 25579 was free afterward. The production source also compiled against the exact Paper API with Java 25 `javac --release 21`, with deprecated-API notes only.

The accepted run reused its disposable persisted fixture, so both normal boots included the four-assertion restart check. A brand-new disposable fixture instead performs the one-assertion preparation path on its first boot (three fewer assertions); its second boot must verify persistence. No live state was copied into this fixture.

### Coverage

- Original metadata, lore, enchantments and PDC survive real Paper storage and serialization; equivalent item variants group as one price entry using semantic item equality.
- Sign placement and ordinary sign editing open Yes/No onboarding; declining keeps storage ordinary. Choosing items, price, review and final publication are separate steps.
- Browsing, quantity changes, review, Back, Cancel, close and disconnect never spend money. Duplicate, stale and re-entrant confirmation callbacks cannot buy twice.
- Exact decimal totals, original spawn bundle sizes/prices, final funds/capacity/access checks, stock/price/revision/ownership changes, competing buyers and distance changes are exercised.
- Chest stock decreases exactly by delivered quantity. Different metadata is retained. No overflow is dropped into the world.
- Actual registered listener events protect physical opening, inventory shift/number/drop/drag interactions, hoppers, breaks, sign edits, fire, block explosions, pistons and adjoining chests. Owner stock access rechecks distance.
- Definitive payout failure rolls back both inventories before refund. Uncertain debit, failed refund and partial inventory write halt safely and retain recovery status after journal reopening.
- Recorded placer, actual claim owner at chest and sign, revoked create permission, changed claim owner, admin claims, unavailable/failing claim API, wilderness, incorrectly attached signs, free-price legacy ChestShop signs and double-chest refusal are covered with the synthetic GP API.
- Protection-only startup retains existing chest/sign protection when Economy is unavailable, including complete absence of Vault classes.

The cloud JVM logged external Minecraft-authentication and Paper-update DNS warnings. Those endpoints are not part of the local synthetic tests; authenticated network-client connectivity remains unverified.

The first harness iteration incorrectly assumed byte-identical serialization for semantically equal items. Exact Paper showed the representations can differ even in native storage. That exposed and verified a fix to UI item grouping; conservative transaction snapshot hashes can still invalidate a changed representation and require a fresh review. An initial restart-fixture failure was fixed by explicitly loading the neighboring disposable chunks used by the registry's safe adjacent-sign checks.
