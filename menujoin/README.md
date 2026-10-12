# MenuJoin

**Version:** 1.1.0
**Command:** `/menujoin` (toggle)

Opens the DeluxeMenus `server_menu` for each player shortly after they join, and gives them a named Server Menu compass that runs `/menu` on use.

- The auto-open opt-out flag is stored in the player's `PersistentDataContainer`, so it survives restarts with no files.
- `/menujoin` toggles the auto-open behavior per player.
- Includes a PlaceholderAPI expansion (`MenuJoinExpansion`).

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp src/main/resources/plugin.yml classes/
jar cf MenuJoin-1.1.0.jar -C classes .
```
