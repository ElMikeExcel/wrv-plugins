# Verification levels and pre-deployment checklist

## Automated

`./build.sh` compiles all nine plugins with Java 21, runs JUnit/Mockito behavior tests, packages JARs, and runs repository, shop-generator and installation-pack tests. Mocks verify code paths without a running Minecraft server. They do not prove client rendering, permissions from the installed permission manager, plugin classloader behavior, host restart policy, or external plugin command acceptance.

The Maven release property emits Java 21 bytecode; the JAR audit checks class version 65, descriptor/main-class/version consistency, required config resources and that no test/server API classes are bundled. Archive checks protect the unchanged recovery artifacts. CI uses read-only repository permission and uploads candidate JARs plus the matching ExcelShops installation ZIP; nothing is deployed.

## Manual staging checks, not yet performed

Use a disposable staging copy with the exact intended server, Java, EssentialsX/EssentialsSpawn, Geyser/Floodgate, DeluxeMenus, PlaceholderAPI, Vault/economy, Jobs/CMILib and mcMMO versions. Do not connect these tests to production data.

1. Load every candidate and inspect startup errors, dependency order and command registration.
2. MenuJoin: join/rejoin and toggle before delayed open; fill inventory and repeatedly run `/compass`; confirm no dropped duplicates, no inventory overwrite, truthful retry message and one compass after freeing space. Check offhand/cursor, death with each keepInventory setting, respawn and right-click from Java and Bedrock.
3. BedrockMenu: test every button, Java fallback and Geyser forms; Spawn Shop must call `/shop`. Try spawn from overworld, Nether and End with/without EssentialsSpawn and with an invalid configured fallback world. Check actual safe landing, permissions, teleport cancellation, TPA cooldown/denial/disconnect and accept/decline flows.
4. RestartCountdown: on the staging process only, test chat plus on-screen titles containing the reason, minute warning and 10-to-1 sequence. Cancel during countdown and final 40-tick grace; disable during countdown. Confirm no shutdown after cancellation. Allow one complete staging shutdown and verify the host's separately configured restart policy.
5. WealthBoard: Vault absent/provider absent; Jobs absent/disabled/enabled; logout before delayed tasks; signed/localized currency; top-three ordering and all-job enrollment with the deployed Jobs version.
6. WeekendXP: controlled staging clock/reload on a Chicago weekday and weekend; confirm actual mcMMO global rate, transition timing (up to 30 minutes), command failure handling and intended reset policy. A successful Bukkit dispatch does not prove the external plugin changed its state.
7. ShopVillager/Spawn Shop: create/tag/remove with admin and non-admin accounts; Java/Bedrock interaction once per tap, no trade window, protected tagged villagers, permission checks, sufficient/insufficient funds and inventory space. Check exact enchanted-item metadata delivered by the shop API against the deployed Minecraft version before any economic use.
8. mcMMO: build the exact pinned upstream plus both patches separately. On staging verify Together bonus range/member count, gameplay-only XP sources, disabled defaults and `/party info` text. The nine-module build does not package mcMMO.

No production readiness or source-to-installed-JAR identity should be inferred until the relevant runtime checks and installed artifact comparison have actually been performed.


## Confirmed shops draft candidate

ExcelShops and the paired generated Spawn Shop menu are a separate, undeployed candidate. ExcelShops 0.1.1 passed 330 Java tests (265 for shops) and 23 Python checks, with zero failures/errors/skips. This includes 64 migration cases and 35 journal cases; separate JVM probes verify real POSIX lock retention after parsing, duplicate starts and cross-class access. The installation-pack checks verify paired menu/catalog resources, hashes, branding, compatibility keys and refusal to overwrite an existing package. Its [usage/setup guide](WRVShops/README.md) includes buyer controls, owner setup and maintenance, migration and manual recovery. Its [portable exact-runtime harness](WRVShops/verification/runtime-harness/README.md) records the candidate hash and all 11 boots: 852 assertions, including 614 for ExcelShops and 238 for the genuine legacy fixture. Clean migration, restart, missing economy/Vault, dirty recovery, conflicting target and missing legacy journal are covered. Every test server stopped normally; source data stayed unchanged.

Keep the verification layers separate: the harness uses real Paper 26.2 #123, Java 25.0.4+7, chest inventories, item metadata and event scheduling, but synthetic players/economy/claims. Actual Essentials/Vault/GriefPrevention/Geyser/Floodgate behavior and phone/Java clients remain unverified. The default protection settings require an actual owner claim, and existing legacy ChestShop shops are untouched. Replacing the old DeluxeMenus immediate-purchase file and installing its matching authoritative catalog are required deployment steps; a plugin JAR by itself does not change an old live menu.
