# Spawn Shop (DeluxeMenus)
- spawnshop.yml -> plugins/DeluxeMenus/gui_menus/spawnshop.yml (generated; don't hand-edit prices)
- server_menu.yml -> plugins/DeluxeMenus/gui_menus/server_menu.yml (adds Spawn Shop at slot 11)
- deluxemenus-config.yml -> plugins/DeluxeMenus/config.yml (registers spawnshop)
- make_spawnshop.py regenerates spawnshop.yml. `python3 make_spawnshop.py 1.25` scales all base prices by 25% before rounding.
Needs PAPI Player and Vault expansions. Buys take Vault money (Jobs earnings); no sell-to-server, no netherite/elytra. Prices are tuned to ~$600/hr for an all-jobs player.

## Regeneration and checks

Run from this directory, for example `python3 make_spawnshop.py 1.25`. Prices retain the original rounding to friendly 5/10/50 increments, so a multiplier is applied before rounding. `--output /path/to/menu.yml` can write somewhere explicit without replacing the local menu.

The multiplier must be finite and greater than zero; invalid or overflowing values fail before writing the output file. The default `1.0` generation is byte-for-byte identical to the preserved menu, including all 18 items, commands, amounts, and prices.

From the repository root run `python3 -m unittest discover -s SpawnShop-DeluxeMenus/tests -v`. These tests check generation, rounding, invalid-input safety, both click paths, and the preserved economy. They do not exercise a live Vault, PlaceholderAPI, DeluxeMenus, or Minecraft server. Verify the enchanted-book give commands against the exact server version in staging before deployment.
