# XP pair deployment: October 12, 2026

## Installed artifacts and provenance

The approved XP pair was installed on Wink Reyes Valley on **October 12, 2026**, then loaded in a normal server restart. The deployed artifacts match the reviewed source at [`6a4827c0da0303bf7872898a39a0e770d6ba6a89`](https://github.com/ElMikeExcel/wrv-plugins/commit/6a4827c0da0303bf7872898a39a0e770d6ba6a89) and the approved artifact hashes. The custom mcMMO base remains upstream `058279c148a65bd708dc85067d3858f6adde1173` plus the repository's ordered patches.

| Installed artifact | Runtime version | SHA-256 verified on host |
| --- | --- | --- |
| `mcMMO-2.3.003-WRV-XP-1.jar` | `2.3.003-WRV-XP-1` | `0dbb196d53bf17d917d485129684f7b599980946c499fd9889c9aa13d38582a5` |
| `WeekendXP-1.0.2.jar` | `1.0.2` | `4452ccd90bea409fca37740d3e4e9967ef6dd56e9280000521eec868cf0254d0` |

This record changes documentation only. It does not rebuild or replace those artifacts, merge [draft PR #2](https://github.com/ElMikeExcel/wrv-plugins/pull/2), or deploy other prepared plugins.

## Checks completed on the live server

Times below are UTC, with Central daylight time (America/Chicago, UTC−5) in parentheses.

- **14:42:54 (09:42:54 CDT):** installation/start sequence recorded.
- **14:43:12 (09:43:12 CDT):** startup reached `Done`; server returned Online with all 27 plugins loaded.
- **14:43:24 (09:43:24 CDT):** a player rejoined successfully. This confirms reconnection, not XP acceptance.
- **14:44:44 (09:44:44 CDT):** loaded versions and `/xpbonus` registration checked. The console received its player-only response; no player bossbar was visually tested by this check.
- **14:46 (09:46 CDT):** full startup review and Monday schedule checks completed. WeekendXP reported a verified vanilla event multiplier of `1.0`; mcMMO's weekday skill-rate reset was also observed. No weekend event was forced for this check.
- **14:48:13 (09:48:13 CDT):** Together and indicator settings checked: Together enabled, multiplier `1.25`, range `32.0`, minimum one other nearby member; `General.XP_Bonus_Indicator.Enabled=true`.

The installed source uses the same mcMMO party and world, an online teammate strictly less than 32 blocks away, and the existing permission/eligibility gates. WeekendXP retains Saturday/Sunday scheduling in America/Chicago with a 30-minute poll. The configured combined factor is `1.20 × 1.25 = 1.50`; configuration and startup evidence are not a live measurement of that combined reward.

No XP-specific startup errors were observed. Existing unrelated Essentials version-support, DeluxeMenus NMS, and query-port warnings remained; this deployment did not resolve or suppress them. World and player data were preserved.

## Still pending: human acceptance

These checks were **not** completed by startup or console verification:

- Real Java and Bedrock phone bossbar visibility/readability, overlap with other bars, and `/xpbonus status|show|hide|toggle` interaction.
- Two real nearby party members entering/leaving the range, changing worlds, or losing eligibility, with measured skill and green-bar rewards.
- Positive gameplay-orb pickup after Mending, fractional carry, and excluded/custom/death-orb behavior with the installed plugin stack.
- Actual weekend rollover and measured weekend-only `1.20×`, Together-only `1.25×`, and combined `1.50×` awards.

The previously completed automated/source tests remain build evidence, not a substitute for these human checks. The orb-provenance limitations and gameplay restrictions in the [Together instructions](README.md) and [WeekendXP instructions](../WeekendXP/README.md) still apply.

Player instructions are also maintained on the [existing server website](https://elmikeexcel.github.io/winkreyesvalley/#mcmmo-xp-preview). Map and shop candidates remain separate deployment decisions.
