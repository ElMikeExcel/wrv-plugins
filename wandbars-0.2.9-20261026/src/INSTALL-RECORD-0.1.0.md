# ExcelWandBars 0.1.0 install record

Status: STAGED ONLY, NOT INSTALLED (Oct 24, 2026).

## Checks
* Unit tests: 66 passed, 0 failed.
* Box test server (Paper 26.3-214, Magic 11.2.5, live Magic config): boots
  with 0 SEVERE; scripted bot test 20 PASS, 0 FAIL (flip 1-2-3-1, closed bar
  no-op, hold = one flip, debounce, boat no-op, chest mode refused, inventory
  restored exactly, action-bar line received, /spellinfo list and detail,
  compass guard after /compass with the bar open, rejoin with the bar open,
  death and respawn).
* Known pre-existing Magic 11.2.5 bug (not this plugin): sneak + right click
  while on bar 2 or 3 logs an NPE in `Wand.cycleSpells` and does nothing. It
  also happens with Magic's own drop-key bar cycle while this plugin is off.

## Install (needs a restart)
1. Copy `ExcelWandBars-0.1.0.jar` into `plugins/` (check it against SHA256SUMS-0.1.0.txt).
2. Restart with the usual warning and countdown.
3. Check the log line `Sneak bar flip ON ... compass guard ON`.

## Rollback
Remove `plugins/ExcelWandBars-0.1.0.jar` and restart. `plugins/ExcelWandBars/`
holds only config.yml and can be left or removed. No other files change.
