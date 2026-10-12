# Wink Reyes Valley plugins

Source and recovery material for the family's Minecraft plugins. Each plugin has one canonical, case-safe folder. Built JARs are generated locally or in CI and are not committed or automatically deployed.

## Current source

| Plugin | Source version | Purpose |
| --- | --- | --- |
| [BedrockMenu](BedrockMenu/) | 1.4.3 | Bedrock native forms and Java menu, player teleport requests, Spawn Shop |
| [Leaderboard](Leaderboard/) | 1.0.0 | Wealth and mcMMO command shortcuts |
| [MenuJoin](MenuJoin/) | 1.2.1 | Join menu toggle, protected/recoverable menu compass, PlaceholderAPI expansion |
| [NoPhantoms](NoPhantoms/) | 1.0.0 | Cancel phantom spawns |
| [RestartCountdown](RestartCountdown/) | 1.1.1 | Two-minute chat-and-title warning, cancellable shutdown |
| [ShopVillager](ShopVillager/) | 1.0.1 | Tagged spawn-shop villager opens DeluxeMenus |
| [WealthBoard](WealthBoard/) | 1.3.1 | Join wealth popup and optional Jobs enrollment |
| [WeekendXP](WeekendXP/) | 1.0.1 | Chicago Saturday/Sunday mcMMO XP rate |
| [ExcelShops](WRVShops/) | 0.1.4 draft | Native and existing ChestShop BUY/SELL final confirmation |

[ChestShop confirmation guard](ChestShop-ConfirmationGuard/) is a separately branded LGPL-2.1 modification of upstream ChestShop build 474, not a tenth custom plugin. Its Maven module is an overlay; only the packaged full guarded JAR is deployable, paired with ExcelShops and accompanied by full corresponding source/notices. [Spawn Shop](SpawnShop-DeluxeMenus/) is a DeluxeMenus configuration/generator, not a Java plugin. [mcMMO Together bonus](mcMMO-TogetherBonus/) is an unchanged patch series for a separately pinned upstream repository.

## Build and test

Install a **full JDK 21** (a Java runtime alone is insufficient), Python 3, and a shell. On Linux/macOS or Windows with Git Bash:

```sh
./build.sh
```

`mvnw` downloads Maven 3.9.9 from Maven Central when needed and verifies its embedded SHA-512 checksum before running it. It requires curl or wget, tar, and sha512sum or shasum. The root build first downloads and hash-verifies the official pinned ChestShop binary/full source and installs its compile API, then runs Maven and packages the full guarded artifact plus LGPL source ZIP. Dependencies are downloaded by Maven from the repositories in `pom.xml`; no pre-populated `~/.m2` is needed. For Windows without a Unix shell, install Maven 3.9+ and run:

```text
bash ChestShop-ConfirmationGuard/bootstrap.sh
mvn clean verify
python ChestShop-ConfirmationGuard/package.py
python -m unittest discover -s ChestShop-ConfirmationGuard/tests_python -v
python -m unittest discover -s tests -v
python -m unittest discover -s SpawnShop-DeluxeMenus/tests -v
python -m unittest discover -s WRVShops/tests_python -v
```

Build one plugin with `./mvnw -pl MenuJoin -am verify` or its `build.sh`. Custom candidate artifacts appear at `<Plugin>/target/<Plugin>-<version>.jar`. ExcelShops builds require the pinned guard API/bootstrap; its wrapper handles this. The full guarded plugin is `ChestShop-ConfirmationGuard/target/ChestShop-3.13-pre-1-build474-confirmation.1.jar`; never install the overlay `ChestShop-ConfirmationGuard-1.0.0.jar`. All source modules target Java 21 bytecode and the **Paper 1.21.4 API**, pinned to timestamped build `1.21.4-R0.1-20250925.065901-231`. This compile target is not a statement of the live server's Minecraft/Paper version. Plugin descriptors declare minimum API 1.21.

The build manages Paper's transitive dependencies, PlaceholderAPI 2.11.6, VaultAPI 1.7, and Jobs v5.2.6.2. Server/plugin APIs are provided dependencies, not bundled into our JARs. Verify runtime versions separately before deployment. The root check verifies the nine custom plugins independently from the third-party overlay: descriptor versions, main classes, Java 21 bytecode, complete guarded binary/source/legal material, case-safe paths and unchanged historical recovery artifacts.

## Recovery and provenance

The canonical source restores material from commit `113611b4c753d43617d9daad0c35027db0d6c13b` into main's layout at `c4fca028f223c5104d9fb25c0a36f429c50a7879`. The cleanup removed unique NoPhantoms, RestartCountdown and WealthBoard sources alongside capitalization duplicates. Newer MenuJoin 1.2.0 recovery behavior and the BedrockMenu 1.4.2 shop change were retained before applying the listed repairs.

BedrockMenu's archived 1.4.1 classes/decompile, the 1.3.0 history, and its 1.4.2 ASM patch remain intact. Normal builds compile readable source. Historical 1.4.2 payload recovery is separate: see [BedrockMenu](BedrockMenu/). Leaderboard/WeekendXP decompiles remain provenance references, not production source roots.

## Installation is a separate step

Nothing here merges, deploys, restarts or changes the live server. Keep backups of the existing plugin JARs/configs and test candidate JARs on a staging copy with the actual server and plugin versions first. See [TESTING.md](TESTING.md) for the runtime checklist and [REPAIR-REPORT.md](REPAIR-REPORT.md) for repair verification and limits.

- Runtime dependencies and command assumptions are listed in each plugin README and `plugin.yml`.
- RestartCountdown calls server shutdown after warnings. It does not start the process again; restart behavior belongs to the host or supervisor.
- Don't overwrite a populated DeluxeMenus config wholesale. Merge this repository's menu registration into the existing file.
- Never commit credentials, server secrets, world saves, player databases or local dependency caches.

## Confirmed shops

[ExcelShops](WRVShops/README.md) adds guided sign-on-chest setup, read-only item browsing, quantity and price review, and a separate final purchase confirmation. Its paired [Spawn Shop menu](SpawnShop-DeluxeMenus/README.md) preserves existing bundle prices while removing immediate money/give commands. ExcelShops was first installed as 0.1.2 on October 12, 2026 and now runs standalone at 0.1.3 after the unneeded ChestShop compatibility plugin was removed; see the [live deployment record](WRVShops/DEPLOYMENT-2026-10-12.md). Startup and the economy connection were verified. A limited player report passed Spawn Shop access/prices/cancel/torch purchase and owner setup/publication; second-player chest-shop purchases and broader client acceptance remain pending. ExcelShops 0.1.3 is now installed with in-place inventory plus/minus updates; the [cursor-fix verification and retest checklist](WRVShops/CURSOR-FIX-0.1.3.md) separates passed automated/startup checks from the still-pending human Java cursor retest. The earlier limited player report applies to 0.1.2. When separately installed, the optional guarded ChestShop artifact keeps existing signs, prices, data, permissions, protection and double chests, while sign clicks open an explicit Buy/Sell chooser, quantity, review and final confirmation. Missing/disabled ExcelShops leaves guarded legacy trading denied. Nonzero ChestShop taxes and unsupported provider/version combinations fail closed. The isolated harness uses actual EssentialsX 2.22.0 but substitutes Vault 1.7.3; the later live receipt identifies Vault-Updated 2.0.0 and a connected EssentialsXEconomy provider, but broader human phone/Java transaction acceptance remains pending. Where legacy ChestShop trading is actually in use, rollback must retain guarded ChestShop or disable that trading; never silently restore instant purchases. The current native/Spawn-only deployment does not need that plugin.

The separately tested [0.1.4 typed-entry candidate](WRVShops/AMOUNT-ENTRY-0.1.4.md) adds **Enter Amount** for exact Java prices and quantities, with private native text input, retry/cancel/timeout handling and unchanged review/final confirmation. It is not installed; live remains 0.1.3. Bedrock retains native number forms. Real client rendering requires player acceptance.

A separate whole-server gate remains: the observed Jobs 5.2.6.3 item shop and DeluxeMenus `advanced_menu`/`requirements_menu` examples lack confirmation. Reversible pause preparation is approved; the package includes a precise spec and an offline generator, not full credential-bearing configs. The separately approved October 12 installation applied these scoped pauses and verified their stored settings/menu load state. Real-player acceptance and any unenumerated purchase routes remain separate checks. See the [deployment scope gate](WRVShops/DEPLOYMENT.md#outstanding-whole-server-confirmation-gate) before describing the pair as covering every purchase route.

## Public server settings notes

[October 12 Jobs rewards and Normal difficulty](ServerSettings/2026-10-12/README.md) records the bounded points/display edit, preserved balances, and verified three-world difficulty. Only the safe settings patch and verification summary are included, never complete private configs or player data.

