# ExcelDeathProtection 0.1.2 verification results

## Publication and release status

This is a source-only draft update to PR #13, prepared October 13, 2026. It preserves the exact 19 source, test, configuration, and documentation files from the tested offline candidate. No implementation or candidate-document bytes were changed for publication.

Some candidate documents say “not published.” That is their pre-publication snapshot status. This draft publication supersedes only that publication status. Version 0.1.2 has not been merged, installed, or deployed by this update. Java and Bedrock-mobile client acceptance remains pending. Historical 0.1.0/0.1.1 installation or player results do not validate 0.1.2.

The separate source manifest identifies each candidate file by SHA-256 and byte length. This report and that manifest are publication evidence, not additional plugin implementation. No JAR, archive, runtime world, playerdata, purchase journal, runtime logs, credentials, or private attachment links are published here.

## Exact candidate identity

- Input repository: `ElMikeExcel/wrv-plugins`, draft PR #13.
- Baseline commit: `ff224dea03d61e0f2227e65d8015d662c93c8d34`.
- Candidate version: `0.1.2`.
- Candidate JAR: `ExcelDeathProtection-0.1.2.jar`, 42,508 bytes.
- JAR SHA-256: `24b8866853fea7f86c70d8d002a9200fbe36340a8d1284bf76f28bc8684af7f4`.
- Offline candidate ZIP SHA-256: `db4cd44f097e69fbe28f2cb022d097fbcbd3a2883e2501ca67b3dc608f3df9ea`.
- Candidate source checksums: [SOURCE-MANIFEST-0.1.2.json](SOURCE-MANIFEST-0.1.2.json).

The publishing check rehashed the JAR and ZIP, verified every candidate source file against the manifest, and compared all 19 files byte-for-byte with their entries in the tested archive. The existing repository Spawn Shop files are unchanged.

## Build and automated tests

The recorded final fresh build used Maven 3.9.9, a Java 21 compile target, pinned Paper API 1.21.4, and Vault API 1.7. `build.sh` ran Maven verification with cached dependencies in offline mode. JAR class files target Java 21; test and runtime-fixture classes are not packaged in the plugin.

**48 automated tests passed: zero failures, errors, or skipped tests.**

| Suite | Tests |
| --- | ---: |
| PurchaseCoordinatorTest | 34 |
| PurchaseJournalTest | 11 |
| ResourceConfigurationTest | 3 |

Coverage includes tier prices, confirmation binding and expiry, eligibility and balance changes, inventory capacity, repeat/reentrant calls, inconsistent or throwing economy responses, rollback/refund failures, journal failure boundaries, restart locks, corruption detection, and YAML/resource parsing.

A previous packaging attempt found an unquoted YAML colon. It was corrected before the final tested candidate, and resource parsing regression coverage was added. The final recorded build passed. Legacy Bukkit text APIs produced deprecation warnings.

## Isolated Paper runtime

**128 assertions passed across six final fresh JVM runs**, all using the exact JAR checksum above. Each process exited successfully. Runtime was Paper 26.2 build 123 with Java 25.

| Mode | Passing assertions | Scope |
| --- | ---: | --- |
| normal | 74 | Prices, tier confirmation, duplicate blocking, cancellation, legacy metadata, bossbar lifecycle, effects, XP fields, inventory and refund failures |
| restart | 12 | Native player save/load in a new JVM, tier restoration, consumed state staying consumed |
| native | 23 | Native consumption/death/reset, bottle decrement and cancellation, one-time item/armor retention, XP reset, second unprotected death |
| no-economy | 6 | Safe purchase rejection when an economy provider is missing |
| faults | 10 | Injected throwing save calls, verified rollback/refund, uncertain-save locks |
| recovery | 3 | Ambiguous transaction locks survive another JVM without new money operations |

The fixture used synthetic server players, native Paper inventory/PDC/effect/bossbar code, a no-op packet sink, and a fake Vault economy provider. Native consumption/death/reset entry points and cross-JVM player save/load were exercised. These are isolated server checks, not real network or visual client sessions. No real-player purchases or production worlds were used.

The native fixture retained a Curse of Vanishing sword during protected deaths in both tiers. Candidate documentation records that exact-version observation and no longer claims protected cursed items must vanish. A harness assertion was corrected to accept AIR as an empty equipment slot; the final second death was unprotected and cleared inventory normally.

## Release gates still open

1. **Java and Bedrock/Geyser clients:** real menu/tap confirmation, bossbar and effect appearance, network login/relog/respawn, and HUD/plugin coexistence have not been tested for 0.1.2.
2. **Production integration:** inspect the actual server, economy provider, plugin versions, configuration migration, menu slots and generator, backups, and rollback plan before any separately authorized installation. The supplied shop file is an unassigned template, not a live menu.
3. **Durability:** Vault and playerdata are not crash-atomic. Paper `saveData()` can return normally despite disabled saving or swallowed storage errors. A paid grant can be lost after a silent save failure; a refunded grant can reappear after a silent rollback-save failure. The throwing-save fixtures do not prove disk-full, power-loss, or disabled-saving behavior.
4. **Manual reconciliation:** ambiguous journal entries intentionally block further purchases. No automatic replay, refund, grant, or per-player unlock exists. Rehearse the documented operator procedure with the real provider before release; do not clear a journal merely to bypass a lock.
5. **Compatibility:** verify competing death handlers, representative causes/dimensions, legacy effect attribution, and cursed-item behavior on the actual server/plugin combination.

See [the release checklist](VERIFICATION.md) and [purchase-journal guidance](../PURCHASE-JOURNAL.md). The recorded offline tests support this draft candidate; they are not a claim of production readiness or a GitHub CI pass. Test execution was completed before publication, not rerun as part of the source upload.
