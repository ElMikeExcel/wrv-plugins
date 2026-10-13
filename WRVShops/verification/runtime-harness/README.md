# Disposable exact-runtime ExcelShops harness

**Never install any harness JAR on a real server.** The fixture creates synthetic players, an in-memory fake Vault economy, fake claim records, test chests and signs, and shuts down its server when checks finish. This folder is source, configuration, runner and concise evidence only. Do not commit binaries, worlds, logs, account data or the sibling disposable runtime folder.

## Runtime and scope

- Exact Paper 26.2 build 123, commit `50018799f282010f7eb15d32c29cadbbe7c9d740`
- Paper API `26.2.build.123-stable`
- Eclipse Temurin Java `25.0.4+7-LTS`
- Official VaultAPI 1.7 interfaces, with an original in-memory test provider
- Original synthetic claim provider exposing the public GriefPrevention API used by ExcelShops
- Loopback only `127.0.0.1:25579`; online authentication and whitelist required; RCON/query/management server disabled

The synthetic players use native Paper inventories and actual `ItemStack` metadata. A test-only delegating `Server` proxy permits ExcelShops to resolve their UUIDs without network clients. World blocks, chest storage, plugin loading, service lookup, scheduled callbacks, event dispatch, serialization and clean server restarts run on exact Paper.

This does not validate real Java/Bedrock client rendering, phone layouts, client packets, Geyser/Floodgate, the actual installed economy/GriefPrevention versions, player-save persistence, production permissions or another plugin's interference. No real player accounts or purchases are involved.

### Native typed-entry checks

For candidates containing `JavaNumberDialogs`, the harness also exercises the visible
Enter Amount buttons at quantity slot 22 and unit-price slot 24. Synthetic `Player`
proxies capture the actual Paper `showDialog` call. `NativeDialogProbe` reads the real
Paper dialog's serialized text control, initial amount, input length, explicit Use
amount/Cancel actions and close behavior, then submits synthetic responses through
Paper's registered callback manager. Its server implementation inspection is confined
to test source; no implementation or NMS dependencies are introduced into the plugin.

The 31 typed-entry groups cover a `0.50` unit-price draft, `19` bread bundles totaling
`$760.00`, unchanged final confirmation, zero/fraction/out-of-stock validation,
foreign-player/duplicate/stale responses, cancel preserving the latest amount,
native send failure, unrelated inventory opening, disconnect/reconnect, missing input,
and a real sixty-second scheduled timeout. Existing 17 plus/minus cursor regressions
still verify identical native inventories, unchanged synthetic cursor items, and no
close/reopen calls while adjusting numbers. Timeout checks add approximately one minute
to each normal boot. Server acceptance and synthetic callbacks do not establish real
Java client rendering, mouse-pointer behavior, Bedrock rendering or phone usability.

### Balance-display checks (0.1.5 candidates)

When `getBalanceDisplay` is present, ten additional cases inspect actual Paper inventory
headers, balance icons, quantity/review/final-confirmation costs and projected balances,
insufficient-funds warnings, owner setup, and actual native-dialog text. Changed synthetic
balances are re-read at review, confirmation and invalid-input retry. Viewing and Cancel
must not invoke debit/credit operations or deliver items. These are synthetic runtime
checks; they do not establish physical Java or Bedrock rendering/input acceptance.

## Safe reproduction

Requirements: Python 3, exact Java 25 JDK, built `ExcelShops` JAR and a new disposable Paper directory.

1. Obtain the exact official Paper artifact using [Paper build metadata](https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/123), with SHA-256 `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`. Name it `paper-26.2-123.jar`.
2. Copy `server.properties.loopback` into the disposable folder as `server.properties`. Never use a production folder or world.
3. Run official Paperclip patch-only preparation: `$JAVA_HOME/bin/java -Dpaperclip.patchonly=true -jar paper-26.2-123.jar`. This extracts pinned server runtime/libraries. Existing verified binaries from a prior disposable test can instead be copied into a new directory; do not reuse its worlds, plugins or player data.
4. Establish acceptance of the [Minecraft EULA](https://www.minecraft.net/en-us/eula) before supplying `eula.txt` containing `eula=true`. The runner never creates acceptance.
5. Supply the already-resolved official dependency artifacts: VaultAPI 1.7 (SHA-256 `47f6a400a8ab22012e6a320f7bfc63621ce4a246e1c60502ff7fc91d532d2601`) and JetBrains annotations 26.0.2 (SHA-256 `2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297`). Obtain them through the repository's declared dependency sources.
6. Run:

```sh
python3 verification/runtime-harness/run.py \
  --server-dir /path/to/new-disposable-server \
  --plugin target/ExcelShops-0.1.1.jar \
  --legacy-plugin /path/to/WRVShops-0.1.0.jar \
  --vault-api /path/to/VaultAPI-1.7.jar \
  --annotations-jar /path/to/annotations-26.0.2.jar \
  --java-home "$JAVA_HOME"
```

The runner verifies dependency hashes and isolation settings, rejects unexpected plugin JARs or a port already in use, and compiles original test providers/harness. Without `--legacy-plugin` it performs the original four complete boots: normal startup, a clean restart, missing Economy service, and Vault plugin/API entirely absent. With the exact original candidate supplied it performs eleven full boots, including the migration sequence below. Each run writes a manifest of artifact hashes and a console log only under the disposable server. A bounded timeout requests clean stop of the process it started, then terminates only that child if needed. It never finds or stops another server.

Provider fixtures are deliberately named `Vault` and `GriefPrevention` so ExcelShops exercises its actual declared dependency and reflection paths. They are clearly marked `0.0.0-test-only`, are not substitutes for the real plugins, and must never be distributed as production dependencies.

## Migration sequence

`--legacy-plugin` requires original WRVShops 0.1.0 SHA-256 `16de2e1477f561987717412b4c338eda136cd1025fad85e3c692ad29fc663aaa` and a fresh disposable fixture. The runner never overwrites a pre-existing shop data folder.

1. Boot the original WRVShops JAR to create an authentic legacy registry, journal and physical world fixture. Shut down normally. Add opaque synthetic evidence files to prove that unknown files are copied too.
2. Replace only that isolated plugin JAR with ExcelShops. Run normal checks, clean restart, missing Economy and missing Vault. Verify old shop IDs, exact item metadata, all seven persisted stock items, original `wrvshops` PDC tags, new command identity and old command alias.
3. Restore the runner's own legacy snapshot, add a synthetic dirty-session marker, then boot ExcelShops and restart it. Recovery must remain locked, stock and economy must be untouched, and the destination journal must remain byte-identical to the old journal. The marker must remain present after normal shutdown.
4. Restore the legacy snapshot again, create a conflicting nonempty ExcelShops target folder, then boot and restart. Migration must fail closed without changing either folder; old PDC-tagged chests and signs must remain protected even before registry initialization.

5. Restore the genuine legacy snapshot, remove only its synthetic transaction log, then boot and restart. Existing shop state with missing journal evidence must stop before migration staging, never create a new target or substitute journal, and continue protecting the old tagged stock.

Every renamed-plugin boot compares all legacy file hashes, directory/file types and permission modes. Dirty and conflict scenarios check protected physical opening, owner opening, chest/sign breaks, hoppers, player interactions, truthful command feedback and unchanged item stock. The snapshots contain only this run's disposable synthetic data. The runner waits for each child process to exit and checks loopback port 25579 after every boot.

## Verified ExcelShops 0.1.4 typed-entry candidate

Tested JAR SHA-256: `565ae9e91e7a4516908e2d245f87ef1936aae23d055f084dc4159112e445f83c`.

The 2026-10-13 serial run passed **4 full server boots / 1,104 assertions / zero failures**
on exact Paper 26.2 #123 and Temurin 25.0.4+7-LTS:

- Normal boot: 83 groups / 543 assertions
- Clean restart: 83 groups / 543 assertions
- Missing Economy service: 7 assertions
- Missing Vault plugin and API: 11 assertions

Both normal boots passed all 31 native typed-entry groups and all 17 existing
plus/minus cursor groups. Each exercised the actual sixty-second entry timeout.
All four server children exited normally with code 0; loopback port 25579 was
available after every boot and independently checked again at completion.
The authorized existing disposable fixture and its unchanged EULA acceptance
were reused. No production server, accounts or real money were involved.

The 26 production Java source files also compile against the exact Paper API
with `javac --release 21`, producing 75 classes with deprecated-API notes only.
The normal artifact remains Java 21-compatible and uses only public API reflection
for optional dialogs. Native dialog inspection in this harness is test-only.

`evidence-excel-0.1.4/` records the result summary, all four hashed manifests,
per-group results, exact API compile/source hashes and shutdown verification.
The clean/dirty/conflict legacy migration suite was not rerun for this UI-only
candidate. Its earlier evidence below is historical. Client rendering and the
other limitations stated above remain unverified.

The final candidate includes the Bedrock-safe Java-dialog routing guard. This
runtime fixture has no actual Geyser/Floodgate provider and verifies the native
Java branch; separate synthetic API fixtures cannot establish real-device support.
An earlier concurrent build/runtime attempt timed out after 81 passing groups,
with no failed assertion and no completed boot. It is not acceptance evidence.
Its dirty synthetic world, registry and journal were archived intact outside the
repository, then the existing clean accepted fixture was restored and byte-checked
before this serial four-boot pass. No recovery marker was cleared in place, and
no new EULA acceptance or live-server action occurred.

## Historical ExcelShops 0.1.1 candidate

Final tested JAR SHA-256: `5d8628c0855f6664e1434b69234359bfe0c5cd0598885fb391e851f1ad2850e1`.

The accepted 2026-10-12 UTC run passed all **11 full server boots / 852 assertions / zero failures** on exact Paper 26.2 #123 and Temurin 25.0.4+7-LTS:

- Original WRVShops baseline: 35 groups / 238 assertions, establishing the genuine old registry, journal and world fixture
- ExcelShops clean migration: 35 groups / 245 assertions
- ExcelShops clean restart: 35 groups / 245 assertions
- Missing Economy service: 7 assertions
- Missing Vault plugin and API: 11 assertions
- Dirty legacy migration and restart: 19 assertions each
- Conflicting data folders and restart: 17 assertions each
- Missing legacy transaction journal and restart: 17 assertions each

The renamed candidate accounts for 614 assertions; 238 prepare and verify the original legacy fixture. All legacy file hashes, directory/file types and permission modes remained unchanged during every renamed-plugin boot. Dirty migration retained the exact old transaction journal and recovery marker across restart. Conflict and missing-journal startup stayed in protection-only mode without changing either data folder; missing evidence never produced a fresh substitute journal or reached staging. Old `wrvshops` PDC-tagged stock remained protected in every paused state.

Every child process exited normally with code 0. Port `127.0.0.1:25579` was free after each boot and after final verification. The final production sources also compile against the exact runtime API with `javac --release 21`: 56 class files, deprecated-API notes only.

`evidence-excel-0.1.1/` contains the accepted result, all 11 artifact/run manifests, per-case results, source compile hashes, official runtime provenance, legacy folder hash manifest and shutdown verification. The older `evidence/` directory is preserved unchanged as historical WRVShops 0.1.0 evidence.

These are synthetic economy/claims/players on real Paper storage. Real Java and Bedrock client rendering, actual economy/GriefPrevention/Geyser/Floodgate versions, real player saving and live deployment remain unverified and require the separate staging checklist. No live server or real money was used.

## Historical WRVShops 0.1.0 evidence

The following evidence describes the original pre-rename candidate. The accepted renamed candidate results are above.

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
