# ExcelEliteLadder

Applies the approved Elite Seal Ladder to EliteMobs' `MobCombatSettings.yml` when ExcelGuildGate
seals break. It waits for an empty server, then makes a dated backup, edits only the six ladder
lines, verifies the result (restoring on any failure), and runs `em reload`. Up only.

- Build: `./build.sh` (manual javac, Paper API + snakeyaml at compile time; deterministic).
- Tests: `./run-tests.sh` (plain JVM, core only).
- Admin: `/eliteladder status | check | retry` (op).
- Depends on (soft): EliteMobs, ExcelGuildGate.

See CHANGELOG-0.1.0.md for the full behavior and CHANGELOG-0.1.1.md for the Seal 5 spawn change (0.12).
