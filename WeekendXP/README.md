# WeekendXP

Version: **1.0.1**, restored and repaired from **1.0.0**.

Automatically applies mcMMO's weekend XP schedule in `America/Chicago`:
- Saturday–Sunday: `mcmmo:xprate 1.2 true`
- Monday–Friday: `mcmmo:xprate reset`

mcMMO is a required dependency, so its commands are registered before WeekendXP enables. The schedule is applied immediately on plugin enable, then checked every 30 minutes. A midnight transition can therefore take up to 30 minutes at normal server tick rate. This is the recovered polling interval; it is not an exact-midnight scheduler.

The first weekday check always dispatches a reset, including after a WeekendXP-only reload with a stale same-process rate. Successfully dispatched unchanged states are not repeatedly announced. A failed or throwing dispatch is retried on the next scheduled check. Disabling the plugin cancels its periodic task.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl WeekendXP -am verify
```

Output: `WeekendXP/target/WeekendXP-1.0.1.jar`. mcMMO is required at runtime but not imported by the Java sources.

Tests cover weekday initialization, weekend transitions, Chicago versus UTC boundaries, winter time/DST fallback and failed/throwing command retries.

## Recovery and compatibility

Readable source and manifest came from historical [`weekendxp/` at `113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/weekendxp). Its original behavior matches the retained reference decompile. `decompiled/` remains unchanged and is excluded from compilation; `RECOVERY.md` preserves the superseded source-missing note.

The original boolean initialized to false, skipping the initial weekday reset, and was updated even when command dispatch failed. Version 1.0.1 fixes those states and declares the missing mcMMO dependency. Namespaced commands avoid unrelated plugins' command aliases. The explicit `true` event argument and decimal rate are supported by the [pinned mcMMO source](https://github.com/mcMMO-Dev/mcMMO/blob/058279c148a65bd708dc85067d3858f6adde1173/src/main/java/com/gmail/nossr50/commands/XprateCommand.java) used by the recovered custom patch base; arbitrary older mcMMO versions are not certified.

A Bukkit dispatch return value establishes command handling, not the actual resulting XP multiplier. Verify `/xprate` output and gameplay XP on an authorized staging server with the installed custom mcMMO build. The [official mcMMO documentation](https://github.com/mcMMO-Dev/mcmmo-wiki-repo/blob/master/xprate.md) describes XP-rate events as runtime settings; the startup fix is not a claim that rates persist across full server restarts. Reset restores mcMMO's configured baseline and can clear separately managed XP events, so coordinate manual XP events with this automatic schedule.
