# ExcelRestartCountdown (RestartCountdown)

**Created by us (custom add-on).** This is a documentation label; the runtime plugin name remains `RestartCountdown`. Recorded installed version: **1.1.0**; this branch's source version: **1.1.1**. The source/build details below describe this branch, not proof of the installed JAR's behavior. See the [plugin inventory](../PLUGIN-INVENTORY.md).

Version: **1.1.1**, restored and repaired from **1.1.0**.

- `/restartwarn [reason]` starts a two-minute warning sequence, from console or an operator with `restartcountdown.warn`.
- Chat **and on-screen titles** warn at 2:00, 1:00, 0:30 and every second from 10 through 1, then announce shutdown. The supplied reason is included in chat and the title subtitle.
- `/restartwarn cancel` aborts both the countdown and any pending final shutdown delay. A second countdown cannot start while either is pending.
- Disabling the plugin cancels both scheduled tasks.

The original timing is retained: one callback every 20 server ticks, beginning at 120, followed by a 40-tick delay after “Restarting now!” so the final messages can be delivered. Server lag can extend elapsed wall-clock time.

**This plugin shuts down the server. It does not start it again.** Starting the server remains a separate host/panel operation. Installing or building this source does not authorize running `/restartwarn`.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl RestartCountdown -am verify
```

Output: `RestartCountdown/target/RestartCountdown-1.1.1.jar`.

Tests cover the complete sequence, reason-bearing chat and titles, the final 40-tick delay, permission checks, active cancellation, cancellation during the shutdown grace period, duplicate starts and plugin disable cleanup. They use a mocked scheduler and never stop a real server.

## Recovery and repairs

Readable source and manifest came from historical [`restartcountdown/` at `113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/restartcountdown). The historical implementation broadcast only to chat. Titles restore the documented operational requirement for visible restart warnings. Version 1.1.1 also fixes the untracked final shutdown task, which previously made late cancellation ineffective and allowed a misleading second countdown.

Visual title rendering on Java and Bedrock, real tick timing under load, and hosting-panel shutdown/start behavior still require an authorized staging check before deployment.
