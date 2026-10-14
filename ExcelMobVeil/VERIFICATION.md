# Fresh reconstruction verification: ExcelMobVeil 0.1.0

Verified October 14, 2026 UTC. This report describes the newly reconstructed artifact. It does not inherit any hash, build result or runtime assertion from the unavailable earlier workspace. Publication, installation, website deployment and actual player acceptance are separate outcomes.

## Exact artifact and build

JAR: ExcelMobVeil-0.1.0.jar

SHA-256: `66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc`

Fresh offline build: **180 tests passed**, zero failures/errors/skips. Breakdown: 71 gameplay, 13 timing, 49 transaction, 33 review-menu, 11 journal and 3 resource tests. The fresh independent gameplay review found no remaining release-blocking source finding. Build log and source/test manifests are included in the final archive.

Cached JDK 25 and Maven 3.9.9 were used, with Java 21 output bytecode and pinned provided Paper/Vault APIs. No new toolchain installation or download occurred. Mockito/Byte Buddy's experimental compatibility flag is test-only.

## Fresh native Paper matrix

**150 assertions passed across ten modes on the exact JAR above**, using Paper 26.2 build 123 and Java 25:

- stacking: PASS 15 assertions; 11.516 seconds; exited normally and released its loopback port.
- edges: PASS 22 assertions; 11.015 seconds; exited normally and released its loopback port.
- normal: PASS 26 assertions; 11.512 seconds; exited normally and released its loopback port.
- purchase: PASS 39 assertions; 12.012 seconds; exited normally and released its loopback port.
- lifecycle: PASS 19 assertions; 11.009 seconds; exited normally and released its loopback port.
- restart: PASS 4 assertions; 10.507 seconds; exited normally and released its loopback port.
- no-economy: PASS 3 assertions; 10.518 seconds; exited normally and released its loopback port.
- review-races: PASS 7 assertions; 10.01 seconds; exited normally and released its loopback port.
- coexist: PASS 11 assertions; 10.506 seconds; exited normally and released its loopback port.
- soak: PASS 4 assertions; 491.788 seconds; exited normally and released its loopback port.

Native potion use consumes a real bottle before activation finalizes. Main/offhand sequential drinks add exactly 480000 milliseconds to the existing deadline. Pending duplicates and canceled drinks do not add time. Attacking removes all accumulated time. Drinking and extending never invoke the economy. Deadline overflow rejects before another bottle is consumed.

Injected pre-consumption save failures retain the native potion. Observed post-consumption failures restore the exact old timer and proven single hand/offhand potion; unknown inventory states are not blindly refunded. These controlled exceptions do not prove crash durability or swallowed disk errors.

Farming, harvest, tilling, planting, block work, air swings and passive Thorns retain the veil. Armored standard targeting and Warden anger are suppressed; current targets clear. Enderman/slime handling, projectile attacks, uncancelled incoming damage, milk/death, effect ownership, malformed state and world-change events are exercised.

The restart test uses a separate JVM and native saved player data to retain stacked absolute deadlines. The final soak runs a single potion through a real eight-minute wall-clock expiry with no shortened deadline. It proves a single full-duration run; stacked arithmetic/native consumption/restart are separate tests, not a claimed sixteen-minute soak.

## Death Protection coexistence boundary

The installed Death Protection JAR could not be retrieved from the panel. Coexistence therefore uses a **fresh build of the exact reviewed source** at ElMikeExcel/wrv-plugins commit effb54c112c15d09249d4f50434cf58d34109f87. That baseline passed 99 fresh tests. Its JAR SHA-256 is `82d14a37cccaa212a78f8a8d96dde7c7de6b192057e5cd82b733eadfdcc0a468`, which differs from the earlier installed binary. This is not a claim that the exact live binary was tested.

The native coexistence run covers distinct item tags, simultaneous effects, attacking ending only the veil, and Death Protection retaining inventory/armor through its protected death while the veil expires. No Death Protection source or live plugin was changed.

## Isolation

Only a copied, already-approved disposable Paper fixture was used, with a byte-identical accepted EULA, synthetic native players, no client login, fake Vault balances and loopback-only binding. Existing cached software was reused. No new EULA acceptance, live-world change or production economy operation occurred. Every test JVM exited and released its port. Fixture/server binaries are not part of the plugin release.

Some fixture faults were intentionally injected to verify rollback. Expected offline version/auth-check DNS warnings are distinct from plugin failures. Final modes have no assertion failures or scheduled-task exceptions.

## Remaining limits

- Real Java/Bedrock/mobile rendering, Geyser translation, network packet timing and actual client relog/respawn remain unverified by this synthetic harness.
- Actual Vault 2.0.0 / EssentialsX 2.22.0 behavior and the full production plugin mix need live acceptance.
- EliteMobs custom bosses and independently scripted dragon behavior remain outside this first version's targeting coverage, as accepted by the owner.
- Incoming damage is not blocked: area/contact/environmental damage and already-fired projectiles can still hurt. Wardens may investigate sound or apply nearby darkness.
- Armor and held items remain visible to other human players under ordinary invisibility; that rendering was not visually checked by the headless fixture.
- Third-party effect vetoes, indistinguishable external invisibility and altered tagged potion metadata may conflict.
- Player.saveData cannot prove disk durability. Economy/playerdata and bottle/timer operations are not crash-atomic; uncertain outcomes require manual reconciliation.
- Production-scale performance and the one-tick interruption boundary between native drinking and finalization remain limits.

## Required durable handoff

The final bundle must be saved to Library, materialized into a separate directory, and its ZIP and embedded JAR hashes verified before installation. RELEASE-CHECKLIST.md, release_checkpoint.py and package_release.py implement the repeatable checkpoint/package/readback workflow. The final canonical Library identity and readback receipt accompany delivery separately; local paths alone are not a backup.
