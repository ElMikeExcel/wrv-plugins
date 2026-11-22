# ExcelEMCompanion 0.1.0 — build record (Nov 21–22, 2026)

One plugin, open module framework (D186), two modules:
drop-guide (D185) and progress-tracker (D186/D187).

## Artifact

- Jar: `ExcelEMCompanion-0.1.0.jar` — 134,742 bytes
- SHA-256: `200ae1fd4056379a10bfe69858fb932ef5e8c17c98abf7fa4fc44d40d70fb7b6`
- Deterministic build (`./build.sh`); Java 21; Paper 26.x.
- softdepend: EliteMobs, Geyser-Spigot, ExcelProtect — the plugin
  loads and both modules work (tracker in its unavailable-panel
  degradation) even if a softdepend is absent.

## Data (generated from the live EM config pull, Nov 21)

- drops-index.yml: 38 places, 407 mobs, 4,066 drops (kinds:
  classloot 3,458; fixed 353; classloot-generic 222; quest 30;
  random 3). Zero unresolved references; zero chance values.
- tracker-catalog.yml: 111 quests, main chain 31, 14 dungeons,
  38 places / 407 bosses with files + worlds.
- Generators: tools/generate_index.py, tools/generate_tracker_catalog.py
  (reports: GENERATOR-REPORT.md, TRACKER-CATALOG-REPORT.md).

## Unit tests — 137/137 PASS (`./run-tests.sh`)

IndexTest (drop index invariants incl. no-chance guards and the
Sewers/Shredder/Sewage Keys specimen), DropGuideMenusTest
(navigation, Back/Close, pagination, Bedrock details),
TextUtilTest (incl. nameplate cleaning), TrackerCatalogTest
(111/31/14 derived denominators), ExpertiseTest (tier boundaries,
colors, legend), TrackerMenusTest (the Jester fixture: 63 / 61 of
111 / 26 of 31 / 20 / 1,622 / 25; per-boss colors; downed
fallback; no fabricated counts), ProtectKillsTest (aggregation
rules: own killing blows only, gz days, malformed lines skipped).

## Mirror rig — 36/36 PASS, SEVERE 0 (three boots)

Paper 26.2 + real EliteMobs 10.9.10 + the live EM config tree +
the REAL ExcelProtect entities records for Nov 20–21, 2026.
Receipts: receipts/rig-full.json, rig-dropoff.json,
rig-trackeroff.json; harness: rig/run-scenarios.py.

- Full (30/30): both modules enabled; /drops, /progress, /tracker,
  /companion all registered. Fixture seeded through EM's own API
  (no player-data file copied — see flags in the parent report)
  and read back through the tracker's EmData path: **63 quest
  completions, 61 distinct of 111, 20 dungeon runs, 1,622 kills,
  highest elite 25, 1 death, main chain 26 of 31, 8 of 14 dungeons
  downed** — all exact. Per-boss counts from the real records
  match the independent Python count (receipts/
  protect-sewers-independent-count.txt) exactly: **Shredder ×2,
  Noxious Ninja ×21, Spore Spreader ×34, Sewers clears ×2**.
  Live renderer navigation: /drops → Sewers → Shredder (Sewage
  Keys as BONE_MEAL) → Back; /progress → Dungeons (Expertise
  Legend item present in the rendered inventory) → Bosses →
  Sewers bosses (legend present) → Back to Places.
- Drop-guide OFF (3/3): only progress-tracker enabled; /drops does
  not exist; /progress + /tracker work.
- Tracker OFF (3/3): mirror image.

## Bedrock

Menus render through the established Cumulus/GeyserForms pattern
(reflection, no compile-time Geyser dependency). The rig has no
Geyser, so the live form tap-through is a post-install proof per
the ship-it rule; content parity is proven at the model level —
every drop button carries a detail screen with the same text the
Java lore shows, the expertise legend's detail carries the same
lines, and the renderer's Bedrock branch was exercised by the
standalone drop-guide build's pattern (identical helper). Logged
asymmetry: Cumulus forms cannot render item icons.

## What's New drafts (for the install pass, both surfaces)

Website + in-game ExcelWhatsNew, same text:

"NEW: Drop Guide & Progress Tracker — /drops shows what each boss
and dungeon drops (pick a place, pick a boss, see its full drop
list). /progress shows your own record: quests, dungeons, boss
kill counts and dungeon clears — the count's color is your
expertise, from gray Novice to light-blue Mastered. Works on Java
and Bedrock."
