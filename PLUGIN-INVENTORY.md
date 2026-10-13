# Excel plugin identification inventory

This inventory identifies the server add-ons we created or modified. **ExcelName (OriginalName)** is a documentation label, not a request to rename or replace a running plugin. “OriginalName” also helps locate older source folders and records. Existing authorship and license notices remain authoritative.

Plugin IDs, Java packages, commands, aliases, permissions, data folders, persistent storage keys, JAR names and version behavior are unchanged by this documentation. ExcelShops and ExcelPlayerMap already use their Excel names at runtime; no additional rename or migration is planned. The earlier [runtime-rename PR #6](https://github.com/ElMikeExcel/wrv-plugins/pull/6) was closed without merging and is superseded by these labels.

## Created by us: custom add-ons

Recorded installed versions below reflect the October 12–13, 2026 inventory. They are separate from the source snapshot in this branch. A matching version alone does not establish that a source build is byte-identical to an installed JAR.

| Documentation label | Runtime plugin name | Recorded installed version | Source in this branch |
| --- | --- | --- | --- |
| [ExcelBedrockMenu (BedrockMenu)](BedrockMenu/README.md) | `BedrockMenu` | 1.4.2 | 1.4.3 |
| [ExcelMenuJoin (MenuJoin)](MenuJoin/README.md) | `MenuJoin` | 1.2.0 | 1.2.1 |
| [ExcelWeekendXP (WeekendXP)](WeekendXP/README.md) | `WeekendXP` | 1.0.2 | 1.0.1; installed source is linked below |
| [ExcelNoPhantoms (NoPhantoms)](NoPhantoms/README.md) | `NoPhantoms` | 1.0.0 | 1.0.0 |
| [ExcelRestartCountdown (RestartCountdown)](RestartCountdown/README.md) | `RestartCountdown` | 1.1.0 | 1.1.1 |
| [ExcelLeaderboard (Leaderboard)](Leaderboard/README.md) | `Leaderboard` | 1.0.0 | 1.0.0 |
| [ExcelWealthBoard (WealthBoard)](WealthBoard/README.md) | `WealthBoard` | 1.3.0 | 1.3.1 |
| [ExcelShopVillager (ShopVillager)](ShopVillager/README.md) | `ShopVillager` | 1.0.0 | 1.0.1 |
| [ExcelShops (WRVShops)](WRVShops/README.md) | `ExcelShops` | 0.1.4 | 0.1.4; already branded |
| [ExcelPlayerMap (WRVPlayerMap)](https://github.com/ElMikeExcel/wrv-plugins/blob/0329e01cef749719e653b876329cdf7115dff8d3/WRVPlayerMap/README.md) | `ExcelPlayerMap` | 0.2.1 | Separate branch; already branded, module not present here |

### Source and installed-version boundaries

- **WeekendXP:** [commit `55fcc2f`](https://github.com/ElMikeExcel/wrv-plugins/commit/55fcc2fe19afc649508927436db4bf222bbb83f6) records the authoritative installed 1.0.2 source. This branch's 1.0.1 source and README describe a different snapshot; its 1.2× weekend rate is not a statement of the live schedule. No 1.0.2 source is imported by this documentation change.
- **ExcelPlayerMap:** the [pinned map README](https://github.com/ElMikeExcel/wrv-plugins/blob/0329e01cef749719e653b876329cdf7115dff8d3/WRVPlayerMap/README.md) provides the separate module's instructions. Its absence from this branch does not mean it is absent from the server.
- **ExcelShops:** the [deployment record](WRVShops/DEPLOYMENT-2026-10-12.md) distinguishes verified 0.1.4 installation/startup from pending real-device acceptance.
- **Other restored modules:** the individual READMEs retain their source-build and repair descriptions. Newer source versions in this branch are not evidence that those repairs have been deployed. This inventory does not authorize a build, install, restart or data migration.

## Modified by us: upstream code

### mcMMO with Excel XP (mcMMO)

The installed custom version is **2.3.003-WRV-XP-2**. The runtime name remains `mcMMO`; original upstream authors, licenses and attribution remain intact. This label identifies our XP changes without presenting mcMMO as an original custom add-on.

The [mcMMO Together bonus folder](mcMMO-TogetherBonus/README.md) preserves an earlier patch series and its pinned upstream base. It is not a complete source snapshot of the installed XP build. The separate [Jobs XP draft PR #5](https://github.com/ElMikeExcel/wrv-plugins/pull/5) is a candidate, not the installed mcMMO/Jobs pair.

## Configured by us: settings only

### Jobs

The installed **Jobs 5.2.6.3** remains an upstream plugin with a settings customization: new activity-earned points are disabled, and the edited player-facing reward displays no longer show points. Money and job experience remain the rewards; legacy point balances are preserved. This is **not an active code fork** and does not receive an Excel plugin-name label.

See the [October 12 settings record](ServerSettings/2026-10-12/README.md) for the bounded changes and verification limits. The separate Jobs Excel XP code proposal in [draft PR #5](https://github.com/ElMikeExcel/wrv-plugins/pull/5) is modified-code candidate work and is **not installed**.

## Historical or off-server work

- **Jobs with Excel XP (Jobs), candidate only:** modified upstream source proposed in [draft PR #5](https://github.com/ElMikeExcel/wrv-plugins/pull/5), not installed. The live Jobs 5.2.6.3 entry above remains settings-only customization.
- **ChestShop confirmation guard:** a historical upstream modification, removed from the server. It is not actively Excel-branded. Keep the [guard source/build documentation](ChestShop-ConfirmationGuard/README.md), upstream attribution and LGPL-2.1 notices for provenance; the [deployment record](WRVShops/DEPLOYMENT-2026-10-12.md) records its removal.
- **ExcelProgression:** an off-server prototype, not an installed plugin.
- **Spawn Shop / DeluxeMenus:** [configuration and generator work](SpawnShop-DeluxeMenus/README.md), not a new Java plugin or a renamed DeluxeMenus distribution.

This inventory adds identification and status context. Detailed feature descriptions, historical build/test/deployment records and existing runtime behavior remain unchanged.
