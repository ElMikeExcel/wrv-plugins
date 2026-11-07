# ExcelClaims 0.1.2 — per-player expiration (Mike's design, Nov 7, 2026)

Mike's ruling (Nov 7, 2026, 10:23 AM CST, in the Minecraft side chat): "Does it require us to have a clock. Feels like we should just have a config and set each individuals to infinite for now and then we still have the option to do as many days as we want per player in Excel claims that would be an upgrade to grief prevention way of doing things"

## Answer to the question
No clock is required. Expiration is a policy feature GP built for large public servers (clearing abandoned land). Nothing technical depends on it. On WRV it only ever needs to exist as an option.

## Design
- The three global GP clocks (chest 7 / unused 14 / all-claims 60) are REPLACED by a single inactivity threshold that is settable per player. Deliberate deviation from GP parity — this is the upgrade Mike named: GP cannot do per-player at all.
- Config (config-first, easy to change and add to):
  - `expiration.default-days: never` — the fallback for any player without an override. `never` (or -1) = infinite.
  - `expiration.players:` — map of player UUID -> `never` or a day count. Per-player value wins over the default.
  - The old exemption thresholds (10,000 total / 5,000 bonus blocks) retire with the trio: a per-player `never` IS the exemption mechanism now.
- Effective rule: a top-level player claim expires only when (a) the owner's effective threshold is finite, (b) days since the owner's last-seen basis >= threshold, (c) the basis is KNOWN. The 0.1.1 safety floor is retained verbatim: basis = newest positive evidence (stored stamp / live Bukkit last-played / claim stamps); unknown basis (<= 0) never expires under any setting, counted in one log line per sweep run.
- Console/admin command: `/excelclaims expire <player> [days|never]` — no value reports the player's effective setting; a value writes the per-player config entry and applies live (no restart). Permission: griefprevention.adminclaims (same as import/export).
- Live state at 0.1.2 install: packet config step sets `default-days: never` — everyone is infinite "for now," per Mike, with per-player finite values available the moment they're wanted.
- Ordering: 0.1.2 is an increment on 0.1.1 (needs its seeding + guard); installs only over 0.1.1, in its own window. 0.1.1 still lands as packeted (its 7/14/60 restore is safe under the fix and is superseded by 0.1.2's config step).

## Tests owed with the build
Unit matrix: default never -> nothing expires even with a known 20,000-day absence; per-player 30 -> expires at 31 days, not at 29; override beats a finite default; unknown basis never expires under a finite per-player setting (incident shape); command set/report round-trip. Rig: mixed-fixture soak (one never, one finite-expired, one finite-active) + reboot persistence + FACADE-SMOKE unchanged.
