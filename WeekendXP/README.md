# WeekendXP

Version: **1.0.2**, restored and repaired from **1.0.0**.

**Installed on Wink Reyes Valley on October 12, 2026**, initially paired with mcMMO **2.3.003-WRV-XP-1**, then retained unchanged with installed **2.3.003-WRV-XP-2** later that day. The XP-2 indicator appears after eligible accepted XP and hides three seconds after the latest gain by default; human display acceptance remains pending. Startup and the Monday weekday reset were verified; actual Java/Bedrock display, XP pickups, and weekend/Together gameplay acceptance remain pending. See the [deployment record](../mcMMO-TogetherBonus/DEPLOYMENT-2026-10-12.md). This docs-only status update preserves the tested build provenance and does not change binaries or merge the draft review.

Automatically applies the weekend XP schedule in `America/Chicago` to mcMMO skill XP and Minecraft's green XP bar:
- Saturday–Sunday: `mcmmo:xprate 1.2 true`, then vanilla XP event multiplier `1.2`
- Monday–Friday: `mcmmo:xprate reset`, then vanilla XP event multiplier `1.0`

Requires the matching custom mcMMO build exposing the vanilla XP bridge described below. mcMMO owns the single vanilla orb-XP pipeline: WeekendXP only updates its event multiplier and never listens for orb events or rounds XP independently. When the Together bonus is active at `1.25`, vanilla XP uses the combined `1.25 × 1.2 = 1.5` multiplier on weekends, with rounding handled once by mcMMO. The event multiplier also applies to players without Together (`1.2` on weekends).

mcMMO is a required dependency, so its commands are registered before WeekendXP enables. The schedule is applied immediately on plugin enable, then checked every 30 minutes. A midnight transition can therefore take up to 30 minutes at normal server tick rate. This is the recovered polling interval; it is not an exact-midnight scheduler.

The first weekday check always dispatches a reset and sets the vanilla event multiplier to `1.0`, including after a WeekendXP-only reload with stale same-process rates. An unchanged state is cached only after the mcMMO command dispatch succeeds, the bridge reports support, and its multiplier readback matches the requested value. A failed command, missing/unsupported bridge, throwing API or mismatched readback leaves the complete schedule retryable on the next scheduled check. A failed command does not change the vanilla multiplier. Disabling the plugin cancels its periodic task; it does not introduce a reset-on-disable policy.

## Runtime bridge and failure handling

WeekendXP resolves these public **instance** methods from the enabled mcMMO plugin's own class and caches the reflected methods for that plugin instance:

```java
public boolean isVanillaXpBonusSupported();
public void setVanillaXpEventMultiplier(double multiplier);
public double getVanillaXpEventMultiplier();
```

The multiplier must be finite and at least `1.0`. mcMMO must report support, accept the setting and return that exact setting before WeekendXP announces a verified vanilla event multiplier. There is no new compile-time artifact dependency or direct mcMMO class reference. A replacement plugin instance causes fresh method resolution.

An old or incompatible mcMMO build produces an explicit startup warning that greenbar XP is unconfirmed. The existing namespaced skill-XP command still runs, but the combined schedule is never marked successful and is retried every 30 minutes; this may repeat mcMMO's own event announcements until the incompatibility is fixed. Repeated identical bridge warnings are deduplicated, while the incomplete-schedule retry warning remains visible. Install both matching builds together on an authorized staging server before deployment.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl WeekendXP -am verify
```

Output: `WeekendXP/target/WeekendXP-1.0.2.jar`. mcMMO is required at runtime but not imported by the Java sources.

Tests cover weekday initialization, weekend transitions, Chicago versus UTC boundaries, winter time/DST fallback, command-before-bridge ordering, partial/throwing application retries, capability/signature checks, missing/replaced plugins, readback mismatch/failure, startup incompatibility warnings, unchanged 30-minute polling and task cancellation.

## Recovery and compatibility

Readable source and manifest came from historical [`weekendxp/` at `113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/weekendxp). Its original behavior matches the retained reference decompile. `decompiled/` remains unchanged and is excluded from compilation; `RECOVERY.md` preserves the superseded source-missing note.

The original boolean initialized to false, skipping the initial weekday reset, and was updated even when command dispatch failed. Version 1.0.1 fixes those states and declares the missing mcMMO dependency. Namespaced commands avoid unrelated plugins' command aliases. The explicit `true` event argument and decimal rate are supported by the [pinned mcMMO source](https://github.com/mcMMO-Dev/mcMMO/blob/058279c148a65bd708dc85067d3858f6adde1173/src/main/java/com/gmail/nossr50/commands/XprateCommand.java) used by the recovered custom patch base; arbitrary older mcMMO versions are not certified.

A Bukkit dispatch return value establishes command handling, not the actual resulting skill-XP multiplier. The vanilla bridge readback verifies mcMMO's event setting, not live orb pickup behavior. Verify `/xprate` output and actual skill/greenbar XP on an authorized staging server with the installed custom mcMMO build, including Together alone (`1.25`), weekend alone (`1.2`), both (`1.5`) and neither (`1.0`). The [official mcMMO documentation](https://github.com/mcMMO-Dev/mcmmo-wiki-repo/blob/master/xprate.md) describes XP-rate events as runtime settings; startup reapplication is not a claim that rates persist across full server restarts. Skill-XP reset restores mcMMO's configured baseline and can clear separately managed XP events, while vanilla reset restores its event multiplier to `1.0`. Coordinate manual XP events with this automatic schedule.
