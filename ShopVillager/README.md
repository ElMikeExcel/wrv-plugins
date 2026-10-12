# ShopVillager 1.0.1

Tapping the spawn shop villager opens the Spawn Shop (DeluxeMenus `spawnshop`) instead of trades. Only operators or players with `shopvillager.admin` can create, remove, or tag shop villagers.

- `/shopvillager create`: create the named, invulnerable, stationary shop villager at your location.
- `/shopvillager tag`: make the villager you are looking at within 5 blocks open the shop, and make it invulnerable. Its appearance and other settings are preserved.
- `/shopvillager remove`: remove tagged shop entities within 3 blocks.
- `config.yml` controls the console command used to open the menu. The default remains `dm open spawnshop %player%`.

Version 1.0.1 also clears click cooldown entries when players leave and skips delayed menu commands for disconnected players. Existing tagged villagers can be tagged again to apply invulnerability.

Build and run unit tests: `./build.sh` (Java 21 and the repository Maven wrapper). Tests use mocked Bukkit events; a Paper server with DeluxeMenus and the Java/Bedrock click paths still requires a staging check before deployment.
