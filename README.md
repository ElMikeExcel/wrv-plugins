# Wink Reyes Valley – custom plugins

Source for every custom plugin on the Wink Reyes Valley Minecraft server (Paper 26.2, Java 25 runtime, Geyser/Floodgate for Bedrock). Built jars live on Google Drive in **Minecraft Wink Reyes Valley / Build files**.

| Folder | Plugin | Version | Status | Java target | Source quality |
|---|---|---|---|---|---|
| BedrockMenu/ | BedrockMenu (/menu, Bedrock forms + Java GUI, teleport menu, Spawn Shop button) | 1.4.2 (staged; 1.4.1 live) | build.sh reproduces the staged jar byte-for-byte | 21 (1.4.2); 1.4.1 was 25 | No original 1.4.1 .java exists (Argus built it). Repo has the 1.4.1 compiled classes, CFR-decompiled reference source, the ASM patch that made 1.4.2, and Steve's 1.3.0 Maven source as history |
| MenuJoin/ | MenuJoin (menu compass, respawn compass, /compass, auto-open toggle, PAPI %menujoin_status%) | 1.2.0 (live) | build.sh | 21 | Original source |
| ShopVillager/ | ShopVillager (tap spawn villager -> Spawn Shop, /shopvillager create) | 1.0.0 (staged) | build.sh | 21 | Original source |
| SpawnShop-DeluxeMenus/ | Spawn Shop (DeluxeMenus menu, not a jar) + server_menu.yml + DeluxeMenus config.yml + generator | staged | `python3 make_spawnshop.py [multiplier]` | n/a | Original |
| mcMMO-TogetherBonus/ | mcMMO fork: party "Together" XP bonus | 2.3.002 + 2 commits | git format-patch against upstream | per mcMMO pom | Patches only (no mcMMO copy) |
| Leaderboard/ | Leaderboard (/leaderboard) by Argus | 1.0.0 | – | – | Decompiled only; original source not found |
| WeekendXP/ | WeekendXP (mcMMO 1.2x XP Sat–Sun) by Argus | 1.0.0 | – | – | Decompiled only; original source not found |

Not found anywhere on the box or Drive (no source, no jar): **NoPhantoms, RestartCountdown, WealthBoard**. Ask Argus whether these are custom and where the source is (RestartCountdown may be a third-party plugin).

Third-party plugin used by tonight's update (not custom, jar on Drive only): ChestShop 3.13-pre-1.

## Building
Each plugin folder has its own `build.sh` (JDK 21+). Dependencies are taken from `~/.m2` (paper-api 1.21.4, PlaceholderAPI 2.11.6, ASM 9.9.1); override with env vars shown in each script. Output goes to `<plugin>/build/`.

Compile with `--release 21` so jars run on Java 21 and 25. No secrets, passwords, or server addresses are in this repo.
