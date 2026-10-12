# Leaderboard

**Version:** 1.0.0
**Command:** `/leaderboard`

Shows every server leaderboard in one place. Runs each plugin's native leaderboard command as the player so the output is always the real, formatted board.

## Boards shown

- **Wealth** — Essentials `/baltop`
- **mcMMO power** — `/mctop`

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf Leaderboard-1.0.0.jar -C classes .
```
