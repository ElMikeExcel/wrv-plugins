# Installed organized shop: October 13, 2026 (America/Chicago)

The approved installation completed at 04:12:41 UTC on October 14 (11:12 p.m. Central October 13). ExcelShops 0.1.6 is enabled and hooked to EssentialsX Economy. DeluxeMenus registered `/shop` for `spawnshop` and loaded eight menus, including all five categories. Read-only checks returned version 0.1.6, all eight menu names, TPS 19.9/19.9/19.9 and zero online players at 04:13:32 UTC.

The live old JAR and root/main menu configuration matched the pinned preparation baselines before replacement. All eight active files were verified again using server-computed SHA-256 after startup. The original JAR and two overwritten configs remain available for rollback. No authoritative catalog, prices, player data, economy data or other plugin was replaced. All 21 product definitions are preserved except menu slot.

Startup had no new shop or YAML error. Pre-existing warnings remain: DeluxeMenus NMS-only item options, Essentials server-version support, optional EliteMobs model features, and the GS4 query listener sharing Geyser's UDP port. These same warnings existed in the pre-install log; the game and Geyser listeners started normally. No unrelated warning/settings change was included.

Real Java/Bedrock rendering, input, live purchases and complete production-plugin compatibility remain unverified. Automated/synthetic-player assertions and server startup are not client acceptance. No new player-testing request is outstanding. Source remains a draft review, separate from the verified live installation.

## Active file SHA-256

- `plugins/DeluxeMenus/config.yml`: `445c5baeba78559ca3c455e63d55100fc6694ca96c0125d1021f641043dc2408`
- `plugins/DeluxeMenus/gui_menus/spawnshop.yml`: `b70ab2f552b34b9c1abeb2a553797bd3d6513f44c25b1d188769c8b7630ec7f2`
- `plugins/DeluxeMenus/gui_menus/spawnshop_building.yml`: `53db933185ff43a3b6cccaeb0acb307f1c53193dfc4c6f648ee04262ad9385ac`
- `plugins/DeluxeMenus/gui_menus/spawnshop_enchanting.yml`: `6e7d7b90cc633df506c24128fd2400e555851cb011c378269da247d009fd2f69`
- `plugins/DeluxeMenus/gui_menus/spawnshop_food.yml`: `47b22f75759850912a1553184af448e768919a8b7d6e2b091d6316a3f75aa6f4`
- `plugins/DeluxeMenus/gui_menus/spawnshop_potions_protection.yml`: `5c136217ac213586c0742048176cee25538f75e9f5d5430d28f42dad76ff83ba`
- `plugins/DeluxeMenus/gui_menus/spawnshop_travel_animals.yml`: `1c0cd5f1080747fa271f71cb0cf914ace78d849cd20b27864f4f8a4f433908a7`
- `plugins/ExcelShops-0.1.6.jar`: `a5191eef70a4fdbd1326c14c36c9fd36afa25fe712762cc1850943a1a75c6d4e`

The public registration-only fixture intentionally is not the full live main config and therefore has a different hash. Its six menu files match the hashes above exactly.
