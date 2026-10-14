# Newly rebuilt ExcelMobVeil: fresh runtime verification

## Result

All **150 freshly executed assertions across 10 runs passed**, including the actual eight-minute single-potion soak. Candidate SHA-256:

`66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc`

This is a new candidate and new evidence after the earlier shared workspace was lost. No older artifact hash or pass result is reused. Every run below recorded this exact new hash, exited normally, and released its loopback port.

| Run | Assertions | Result |
|---|---:|---|
| stacking | 15 | PASS |
| edges | 22 | PASS |
| normal | 26 | PASS |
| purchase | 39 | PASS |
| lifecycle | 19 | PASS |
| restart | 4 | PASS |
| no-economy | 3 | PASS |
| review-races | 7 | PASS |
| coexist | 11 | PASS |
| soak | 4 | PASS |

The actual soak process took 491.788 seconds including startup/shutdown. It independently asserts at least 480,000 milliseconds elapsed, observes active veil before the deadline, and verifies both veil state and invisibility expired afterward.

## Verified outcomes

- Actual main-hand and offhand native item use consumes one potion and returns the correct bottle. Each successful additional drink adds exactly 480,000 milliseconds to the old deadline. Three native drinks prove no one- or two-potion cap. Queued duplicates and canceled extensions preserve their extra bottle; attacking clears all accumulated time. Timestamp overflow is rejected before consumption. Drinking/extension made zero economy calls.
- A thrown pre-consumption save failure cancels without consumption. A deliberately observed post-consumption save failure restores the proven main/offhand bottle and prior timer. Silent disk failure and changed-inventory rollback remain limitations.
- Real `/mobveil buy` command dispatch and native review clicks show quantity one, $2,000 price, fresh fake balance and projected balance; exactly one Confirm charge delivers one stored, inactive potion. Duplicate Confirm, forbidden clicks, drag, insufficient funds, full inventory, Back/close/quit, review expiry, changed price, withdrawal failure and one-shot delivery refund paths are covered. Review-expiry uses an injected clock.
- Actual netherite armor remains equipped. Activation clears existing zombie targeting and Warden anger; standard/native event-bearing target, Warden anger, Enderman and Slime hooks are checked. A real watchdog clears a direct API target that bypasses target events.
- Harvesting, planting, tool interactions and air/block swings preserve veil; simulated farming noise cannot add Warden anger. Passive Thorns preserves veil. Melee attempts and offensive projectiles end it. Canceled launch and harmless pearl launch retain it.
- Incoming damage is not canceled, so no damage immunity is provided. Ordinary invisibility overlap, external removal/replacement, and malformed typed PDC/item markers are checked.
- Native milk, canceled milk, native death/reset, world-change event, scheduler expiry, quit/join, stacked save/reload in a separate JVM, expired saved state, and absent economy behavior are covered. Restart preserves the exact original stacked deadline.

## Coexistence

A fresh coexist test passed against ExcelDeathProtection 0.1.3 reconstructed from reviewed PR13 commit effb54c112c15d09249d4f50434cf58d34109f87. Its tested JAR SHA-256 is 82d14a37cccaa212a78f8a8d96dde7c7de6b192057e5cd82b733eadfdcc0a468. This is NOT the original live binary: exact live-binary coexistence remains unverified. Source and artifact provenance are recorded in coexist-provenance.json.

## Isolation

Fresh copy of the surviving approved leaderboard-runtime-prep fixture; cached Paper 26.2 build 123 and Java 25. The accepted EULA was copied byte-for-byte, without new agreement acceptance. No downloads or installation. Server bound only to127.0.0.1:25591 with online mode and whitelist retained. Only synthetic native players, no-op packet sinks, disposable world data and a fake Vault ledger were used. No real account, purchase, balance, live server or production data was modified.

Paper/auth metadata DNS warnings are separate from plugin behavior. Final runtime logs contain no unexpected scheduled-task exceptions or assertion failures. Deliberate fault-injection warning messages are expected.

## Limits

No real Java/Bedrock client rendering, Geyser translation, network login/relog, real client farming/teleport/respawn, production economy provider, custom boss scripts, every mob species, third-party event overrides, or power-loss durability guarantee. Farming and world transition are registered event-path tests. Synthetic players are listed online for watchdog tests without network login. Armor/held-item appearance is not visually verified. Direct API targets can persist until the periodic watchdog. There is no immunity from already-launched, area, or unrelated damage. Runtime evidence applies only to this cached Paper build.

## Evidence

Per-run results JSON, native assertion lists, complete runtime logs, reconstructed harness source and runners are in this directory. FixtureVault and MobVeilProbe are test-only; never distribute them as production plugins.
