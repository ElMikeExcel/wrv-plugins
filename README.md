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

[Spawn Shop](SpawnShop-DeluxeMenus/) is a DeluxeMenus configuration/generator, not a Java plugin. [mcMMO Together bonus](mcMMO-TogetherBonus/) is an unchanged patch series for a separately pinned upstream repository.

## Build and test

Install a **full JDK 21** (a Java runtime alone is insufficient), Python 3, and a shell. On Linux/macOS or Windows with Git Bash:

```sh
./build.sh
```

`mvnw` downloads Maven 3.9.9 from Maven Central when needed and verifies its embedded SHA-512 checksum before running it. It requires curl or wget, tar, and sha512sum or shasum. Dependencies are downloaded by Maven from the repositories in `pom.xml`; no pre-populated `~/.m2` is needed. For Windows without a Unix shell, install Maven 3.9+ and run:

```text
mvn clean verify
python -m unittest discover -s tests -v
python -m unittest discover -s SpawnShop-DeluxeMenus/tests -v
```

Build one plugin with `./mvnw -pl MenuJoin -am verify` or its `build.sh`. Candidate artifacts appear at `<Plugin>/target/<Plugin>-<version>.jar`. All source modules target Java 21 bytecode and the **Paper 1.21.4 API**, pinned to timestamped build `1.21.4-R0.1-20250925.065901-231`. This compile target is not a statement of the live server's Minecraft/Paper version. Plugin descriptors declare minimum API 1.21.

The build manages Paper's transitive dependencies, PlaceholderAPI 2.11.6, VaultAPI 1.7, and Jobs v5.2.6.2. Server/plugin APIs are provided dependencies, not bundled into our JARs. Verify runtime versions separately before deployment. The root check also verifies descriptor versions, main classes, Java 21 class versions, absence of case-colliding paths, and preservation of the historical recovery artifacts.

## Recovery and provenance

The canonical source restores material from commit `113611b4c753d43617d9daad0c35027db0d6c13b` into main's layout at `c4fca028f223c5104d9fb25c0a36f429c50a7879`. The cleanup removed unique NoPhantoms, RestartCountdown and WealthBoard sources alongside capitalization duplicates. Newer MenuJoin 1.2.0 recovery behavior and the BedrockMenu 1.4.2 shop change were retained before applying the listed repairs.

BedrockMenu's archived 1.4.1 classes/decompile, the 1.3.0 history, and its 1.4.2 ASM patch remain intact. Normal builds compile readable source. Historical 1.4.2 payload recovery is separate: see [BedrockMenu](BedrockMenu/). Leaderboard/WeekendXP decompiles remain provenance references, not production source roots.

## Installation is a separate step

Nothing here merges, deploys, restarts or changes the live server. Keep backups of the existing plugin JARs/configs and test candidate JARs on a staging copy with the actual server and plugin versions first. See [TESTING.md](TESTING.md) for the runtime checklist and [REPAIR-REPORT.md](REPAIR-REPORT.md) for repair verification and limits.

- Runtime dependencies and command assumptions are listed in each plugin README and `plugin.yml`.
- RestartCountdown calls server shutdown after warnings. It does not start the process again; restart behavior belongs to the host or supervisor.
- Don't overwrite a populated DeluxeMenus config wholesale. Merge this repository's menu registration into the existing file.
- Never commit credentials, server secrets, world saves, player databases or local dependency caches.

## Player-map prototype

[ExcelPlayerMap](WRVPlayerMap/README.md) enhances newly activated ordinary maps with pixel-rendered names and distant-player arrows while retaining their vanilla identities, terrain, sharing and cartography. ExcelPlayerMap 0.2.1 is installed; its compact-label update and safe migration from WRVPlayerMap 0.2.0 passed server checks. Human post-upgrade display and gameplay acceptance remains pending. See its README for exact acceptance limits.
