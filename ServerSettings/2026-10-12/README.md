# Server settings changes: October 12, 2026

## Jobs rewards: money and job experience

At **21:38 UTC (4:38 p.m. America/Chicago)**, future activity-earned Jobs points were disabled and points amounts were removed from the player-facing English payment, income, bonus, statistics and payment-preview strings. `/jobs points` now explains that rewards use server money and job experience rather than displaying the legacy balance.

The supported `jobs reload` applied the change without a server restart. Money payments, job XP, reward rates, job definitions, daily quests, permissions and stored legacy points balances were not edited. No points-to-money conversion occurred. The separate Jobs item shop remains paused; admin points-management commands remain available.

Only the points payment switch and 25 English display strings changed. YAML parsed before and after; both files were reopened after reload. General config matched byte-for-byte, and messages matched semantically after the plugin normalized optional quotes in one help string. The reload loaded all 12 jobs, existing quests and one scheduler; the server remained online. Human action-bar, menu and earned-reward acceptance still needs a player check.

The [bounded patch](jobs-points-only.patch) includes only changed reward/display settings and no private connection settings or player data. The [verification summary](jobs-points-verification.json) records byte/semantic checks. Full configuration files, databases and private rollback copies are intentionally excluded. A future rollback needs a separate authorized change and must preserve stored balances and newer unrelated settings.

## Normal vanilla difficulty

At about **22:09 UTC (5:09 p.m. America/Chicago)**, Overworld, Nether and End were changed from Easy to **Normal**. Each world was queried independently and reported Normal. World data was saved successfully. The server's persistent Difficulty setting was saved and reloaded to confirm Normal remains set. No restart was needed; EliteMobs settings were not changed.

## Separate unfinished work

Neither the Skills/Jobs menu prototype nor the proposed Jobs XP bonus integration is installed. The [Jobs XP draft PR #5](https://github.com/ElMikeExcel/wrv-plugins/pull/5) has a passing mcMMO candidate CI build, but the full Jobs build remains blocked and no complete deployable pair is ready. Current Jobs pay/level progression and the installed XP pair remain as documented; do not treat these proposals as live perks.

Current player instructions and dated history are maintained on the [existing website](https://elmikeexcel.github.io/winkreyesvalley/). This record is documentation only; it changes no plugin binaries, private configurations, permissions or live server state.
