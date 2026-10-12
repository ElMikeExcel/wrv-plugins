# Spawn Shop (DeluxeMenus)
- spawnshop.yml -> plugins/DeluxeMenus/gui_menus/spawnshop.yml (generated; don't hand-edit prices)
- server_menu.yml -> plugins/DeluxeMenus/gui_menus/server_menu.yml (adds Spawn Shop at slot 11)
- deluxemenus-config.yml -> plugins/DeluxeMenus/config.yml (registers spawnshop)
- make_spawnshop.py regenerates spawnshop.yml. `python3 make_spawnshop.py 1.25` = all prices +25%.
Needs PAPI Player and Vault expansions. Buys take Vault money (Jobs earnings); no sell-to-server, no netherite/elytra. Prices are tuned to ~$600/hr for an all-jobs player.
