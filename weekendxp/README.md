# WeekendXP

**Version:** 1.0.0

Applies the mcMMO weekend XP boost automatically:

- **Saturday–Sunday:** `/xprate 1.2`
- **Monday–Friday:** `/xprate reset`

Uses the `America/Chicago` timezone and re-checks every 30 minutes in case the server stays up across midnight. Matches the Jobs weekend boost schedule (Sat–Sun).

## Build

```bash
javac -cp paper-api.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf WeekendXP-1.0.0.jar -C classes .
```
