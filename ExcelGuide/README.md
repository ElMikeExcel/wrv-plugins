# ExcelGuide 0.3.0 (source record)

Wink Reyes Valley's in-game Guide for Paper 26.3 (Java and Bedrock via
Geyser/Floodgate). This folder records the 0.3.0 source: peer learning
rebuilt with an owner review queue (`/guide review`), strict filters,
"A player said: ..." serving with no names, and a first-use tip that
repeats until the player taps OK.

- CHANGELOG.md - full change list, replay results and tests
- INSTALL.md - backups, jar swap, verify steps
- ROLLBACK.md - going back to the 0.2.3 jar
- SOURCE.md - build inputs and history
- run-tests.sh / build.sh - plain javac build and test harnesses
- rig-tests/ - Mineflayer scripts used on a local test server
  (tip, restart persistence, rollback to 0.2.3, forward again)

Not in this repo: jars, `knowledge.yml` and `intents.yml` (see
SOURCE.md), live data files, and any player names or UUIDs. The UUIDs
in the tests are fixed placeholders (all 1s, 2s, 0...a, and so on).
