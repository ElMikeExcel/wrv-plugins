# BedrockMenu

**Version:** 1.4.1
**Command:** `/menu`

A server menu that actually works on Bedrock (mobile/console). Sends a native Geyser form dialog to Bedrock players; falls back to the DeluxeMenus chest GUI for Java players.

## Why reflection?

The plugin talks to Geyser's API entirely through reflection using Geyser's own classloader. This avoids `java.lang.LinkageError: loader constraint violation` crashes that happen when a plugin compiles against Geyser API classes directly. There is no compile-time Geyser dependency.

## Buttons (v1.4.1)

| Button | Action |
|---|---|
| Spawn | Teleports directly to spawn (bypasses `/spawn` command issues) |
| Teleport to a player | Opens TeleportMenu — Bedrock Accept/Decline popup, Java clickable chat, player-head picker |
| Jobs & Money | `/jobs browse` |
| mcMMO Stats | `/mcstats` |
| Leaderboard | `/leaderboard` |
| Server Info | `/motd` |
| Auto-open on join | `/menujoin` (toggles MenuJoin auto-open) |

## Files

- `BedrockMenu.java` — main class, command handling, form dispatch
- `GeyserForms.java` — reflection-based Geyser form builder/sender
- `TeleportMenu.java` — teleport-to-player flow (Bedrock popup + Java chat UI)

## History

- 1.0–1.1: direct Geyser API usage — crashed with LinkageError
- 1.2.x: rewrote with reflection; fixed `validResultHandler` overload selection; added Leaderboard button
- 1.4.x: merged teleport-to-player flow, removed Home/Set Home (LuckPerms revoked those perms)

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf BedrockMenu-1.4.1.jar -C classes .
```
