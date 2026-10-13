# mcMMO with Excel XP (mcMMO)

**Modified by us (upstream plugin).** The installed custom build is recorded as **2.3.003-WRV-XP-2** and retains the runtime name `mcMMO`. Upstream authorship and licensing remain intact. This folder preserves the historical Together bonus patch series; it is not a complete source snapshot of the installed build. See the [plugin inventory](../PLUGIN-INVENTORY.md).

## Preserved Together bonus patch

Party members get a skill XP multiplier when other party members are nearby (gameplay XP sources only). `/party info` shows "Together bonus: ACTIVE x1.25".
Config (mcMMO config.yml, Party:): Together_Bonus.Enabled (default false – must set true), Multiplier 1.25, Range 32.0, Min_Nearby_Members 1.

Upstream: https://github.com/mcMMO-Dev/mcMMO
Base commit: 058279c148a65bd708dc85067d3858f6adde1173 ("back to dev", right after 2.3.002), branch master.

## Build
    git clone https://github.com/mcMMO-Dev/mcMMO.git && cd mcMMO
    git checkout -b together-bonus 058279c148a65bd708dc85067d3858f6adde1173
    git am /path/to/patches/*.patch
    mvn -DskipTests package
Main class: src/main/java/com/gmail/nossr50/party/PartyTogetherBonus.java (also touches McMMOPlayer, ShareHandler, PartyInfoCommand, GeneralConfig, plus PartyTogetherBonusTest).
