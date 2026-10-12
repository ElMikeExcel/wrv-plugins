# RestartCountdown

**Version:** 1.1.0
**Command:** `/restartwarn [reason]` (permission: `restartcountdown.warn`)

A true 2-minute in-game restart countdown. Broadcasts at 2:00, 1:00, 0:30, then counts down 10..1, then shuts the server down.

- `/restartwarn <reason>` — starts the countdown; the reason is shown in every broadcast (e.g. `/restartwarn fixing the mobile menu`)
- `/restartwarn cancel` — aborts a running countdown

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf RestartCountdown-1.1.0.jar -C classes .
```
