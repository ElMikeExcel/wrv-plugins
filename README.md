# Wink Reyes Valley — Custom Plugins

Custom Bukkit/Paper plugins built for the Wink Reyes Valley family Minecraft server (Paper 26.2, Java 21).

Each plugin lives in its own folder with source, `plugin.yml`, and a README. Built jars are **not** committed here — they live in Google Drive under `Minecraft Wink Reyes Valley > Build files`.

## Plugins

| Plugin | Version | What it does |
|---|---|---|
| [BedrockMenu](bedrockmenu/) | 1.4.1 | `/menu` that works on Bedrock via native Geyser forms (reflection, no compile-time Geyser dep), DeluxeMenus fallback for Java |
| [Leaderboard](leaderboard/) | 1.0.0 | `/leaderboard` shows wealth + mcMMO boards together |
| [MenuJoin](menujoin/) | 1.1.0 | Auto-opens `/menu` on join, gives Server Menu compass, `/menujoin` opt-out toggle |
| [NoPhantoms](nophantoms/) | 1.0.0 | Cancels all phantom spawns |
| [RestartCountdown](restartcountdown/) | 1.1.0 | `/restartwarn [reason]` — true 2-minute in-game restart countdown with shutdown |
| [WealthBoard](wealthboard/) | 1.3.0 | Top-3 richest title popup on join, auto-enrolls players in all Jobs |
| [WeekendXP](weekendxp/) | 1.0.0 | Sets mcMMO 1.2x XP on Sat–Sun (America/Chicago), resets on weekdays |

## Building

Each plugin compiles against the Paper API. Example:

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf PluginName-1.0.0.jar -C classes .
```

No build tool required — plain `javac` + `jar`. Paper API 1.21.x is the target.

## Notes

- No passwords, IP addresses, or credentials anywhere in this repo.
- BedrockMenu intentionally avoids a compile-time Geyser dependency (uses reflection with Geyser's own classloader) to dodge `LinkageError` loader constraint violations.
