# Verification levels and pre-deployment checklist

## Automated

`./build.sh` compiles all eight plugins with Java 21, runs JUnit/Mockito behavior tests, packages JARs, and runs repository and shop-generator tests. Mocks verify code paths without a running Minecraft server. They do not prove client rendering, permissions from the installed permission manager, plugin classloader behavior, host restart policy, or external plugin command acceptance.

The Maven release property emits Java 21 bytecode; the JAR audit checks class version 65, descriptor/main-class/version consistency, required config resources and that no test/server API classes are bundled. Archive checks protect the unchanged recovery artifacts. CI uses read-only repository permission and uploads candidate JARs only.

## Manual staging checks, not yet performed

Use a disposable staging copy with the exact intended server, Java, EssentialsX/EssentialsSpawn, Geyser/Floodgate, DeluxeMenus, PlaceholderAPI, Vault/economy, Jobs/CMILib and mcMMO versions. Do not connect these tests to production data.

1. Load every candidate and inspect startup errors, dependency order and command registration.
2. MenuJoin: join/rejoin and toggle before delayed open; fill inventory and repeatedly run `/compass`; confirm no dropped duplicates, no inventory overwrite, truthful retry message and one compass after freeing space. Check offhand/cursor, death with each keepInventory setting, respawn and right-click from Java and Bedrock.
3. BedrockMenu: test every button, Java fallback and Geyser forms; Spawn Shop must call `/shop`. Try spawn from overworld, Nether and End with/without EssentialsSpawn and with an invalid configured fallback world. Check actual safe landing, permissions, teleport cancellation, TPA cooldown/denial/disconnect and accept/decline flows.
4. RestartCountdown: on the staging process only, test chat plus on-screen titles containing the reason, minute warning and 10-to-1 sequence. Cancel during countdown and final 40-tick grace; disable during countdown. Confirm no shutdown after cancellation. Allow one complete staging shutdown and verify the host's separately configured restart policy.
5. WealthBoard: Vault absent/provider absent; Jobs absent/disabled/enabled; logout before delayed tasks; signed/localized currency; top-three ordering and all-job enrollment with the deployed Jobs version.
6. WeekendXP: controlled staging clock and normal restart on a Chicago weekday and weekend; confirm both mcMMO global and vanilla event rates, combined 1.5x Together factor, transition timing (up to 30 minutes), bridge readback/failure handling and intended reset policy. Command dispatch alone does not prove actual skill XP behavior. Use the paired custom mcMMO build.
7. ShopVillager/Spawn Shop: create/tag/remove with admin and non-admin accounts; Java/Bedrock interaction once per tap, no trade window, protected tagged villagers, permission checks, sufficient/insufficient funds and inventory space. Check actual enchanted-item syntax against the deployed Minecraft version before any economic use.
8. mcMMO: build the exact pinned upstream plus all ordered patches separately. The eight-module build does not package mcMMO; CI has a separate pinned-upstream job. Follow [the detailed XP staging checklist](mcMMO-TogetherBonus/VERIFICATION.md), including fractions, Mending, source exclusions, sharing, temporary `/xpbonus` indicator and Java/Bedrock client behavior.

No production readiness or source-to-installed-JAR identity should be inferred until the relevant runtime checks and installed artifact comparison have actually been performed.
