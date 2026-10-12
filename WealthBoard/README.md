# WealthBoard

Version: **1.3.1**, restored and repaired from **1.3.0**.

Two seconds after joining, online players receive a brief title with the three highest balances. Five seconds after joining, players who remain online are enrolled in every available Jobs job through the existing Jobs API flow. Full boards remain available through `/leaderboard`; no permanent sidebar is created.

Requires Vault **and an enabled economy provider**. Jobs is optional (`softdepend`); enrollment is skipped when it is absent or disabled. This plugin does not alter Jobs limits, job payout formulas or economy settings.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl WealthBoard -am verify
```

Output: `WealthBoard/target/WealthBoard-1.3.1.jar`. Paper, VaultAPI and Jobs are compile-time/provided dependencies and are not bundled. See the Maven files for pinned build API versions. The Jobs integration retains `getJobsPlayer`, `isInJob` and `joinJob`, checked against the [official Jobs API source](https://github.com/Zrips/Jobs/blob/v5.2.6.2/src/main/java/com/gamingmesh/jobs/PlayerManager.java).

## Recovery and repairs

Readable source and manifest came from historical [`wealthboard/` at `113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/wealthboard).

Version 1.3.1:
- Skips delayed enrollment when a player has logged out, or Jobs is disabled.
- Isolates the optional Jobs bridge and logs incompatible API linkage instead of breaking the scheduled task.
- Preserves the economy provider's currency, negative signs and localized formatting. Fallback formatting retains cents and large values rather than truncating to a Java integer.
- Excludes NaN/infinite balances from the ranking and gives equal balances a stable name-based order.

Unit tests cover offline delayed tasks, operation without Jobs, top-three ranking and malformed balances, and currency formatting. Real Vault/provider registration and Jobs enrollment with the installed plugin versions still need an authorized staging test. Balance queries remain synchronous because Bukkit and economy providers are not generally thread-safe; large player histories should be performance-tested before changing that design.
