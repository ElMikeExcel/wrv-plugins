# ExcelEMCompanion 0.1.0 — spec

One plugin (D186), an open module framework, two modules at ship.
Supersedes the standalone ExcelEMDropGuide plan in
~/workspace/dropguide-build-20261121/SPEC.md (whose design rulings
carry over unchanged into the drop-guide module).

## Core

- `ExcelEMCompanion` (main) owns: the shared `MenuRenderer`
  (Java chest GUI + Bedrock Cumulus forms from one `Views` model;
  per-player view stack; `core:back` / `core:close` / `core:detail`
  / `core:noop` actions), the module lifecycle driven by
  `modules.<id>.enabled`, and dynamic command registration through
  the Bukkit CommandMap. plugin.yml declares only `/companion`.
- A module implements `CompanionModule` and is added with one line
  in `ModuleRegistry` (see README, "How to add a module").
- Every menu/submenu carries Back; root menus carry Close — both
  platforms, enforced by the shared renderer + module view builders.

## Module: drop-guide (D185)

- `/drops` → Places (dungeons + adventure zones/regions) → Mobs →
  the mob's FULL drop list: every entry in its drops definition —
  fixed custom items, class-loot gear of every category, quest
  items, vanilla/material drops the configs list.
- Each drop is its item icon; hover lore (Java) / detail screen
  (Bedrock) carries the info. NO percentages or chances anywhere
  (chances are discarded at index generation; the index never
  carries one). Rarity presentation is EliteMobs' own
  (ItemQualityColorizer ladder); class-loot/generated drops state
  "Stats are rolled when this drops." Quest items are shown and
  tagged. Standard EliteMobs loot (near-universal) is a footer
  note, not a row; dropsVanillaLoot is true on 0 live boss files
  and dropsRandomLoot on 3 (rendered as generated-loot entries).
- Bedrock asymmetry (logged, a Cumulus limit): forms have no item
  icons or hover lore, so buttons are text and each drop's detail
  screen carries the full description.
- Data: `drops-index.yml` generated from the live EM configs
  (38 places, 407 mobs, 4,066 drops at build time); data-folder
  copy overrides the bundled one.

## Module: progress-tracker (D186/D187)

- `/progress` (alias `/tracker`) → hub → Quests / Dungeons /
  Bosses / Combat / Classes & Skills. Own progress only (v1).
- Sources and honesty rules: EM `PlayerData` + ACS profiles for
  counters and XP (completions/runs are totals, labeled as such);
  quest-cooldown registry for distinct quests; boss-lockout list
  for downed dungeons/bosses; ExcelProtect entity death records
  for per-boss kills and per-dungeon clears (killing blows;
  matched by world + cleaned nameplate; downed/not-downed shown
  where no reliable count exists — never a fabricated number).
- Denominators from `tracker-catalog.yml`, generated from the
  live configs: 111 quests, 31-file main chain, 14 dungeons.
- Expertise (D187): ×1 gray Novice, ×2–4 white Familiar, ×5–9
  green Experienced, ×10–19 blue Veteran, ×20–39 purple Expert,
  ×40–74 gold Master, ×75+ light-blue Mastered — the count number
  carries the color, identical scale for bosses and dungeons, and
  a compact legend (color + range + label per tier) rides as an
  info item in the bosses and dungeons menus, with the same lines
  on the Bedrock detail screen.

## Decided points (Iris, under decide-and-log; Mike may veto)

1. Region scope: adventure zones/regions are top-level places
   alongside dungeons (pool data covers both). [from D185]
2. Quest items shown in drop lists, tagged as quest items.
3. Standard-loot footer instead of a per-mob row.
4. Mobs whose only loot is standard EM loot (7) are excluded from
   the guide (they have no listable drops); summon/dummy mobs with
   no drops (275 files) likewise excluded — counted in the
   generator report, never silent.
5. Root menus carry Close instead of Back (nothing above them).
6. Expertise thresholds/labels as above (D187).
7. Per-dungeon clears = killing blows on the objective boss
   (EM completes a dungeon on its objectives dying, so this is
   the sourced completion count, labeled as server records).
8. Class-loot drop categories use the filename token first
   (EM groups crossbows under BOWS; players look for "Crossbow").
9. Spear icons fall back to TRIDENT where the Paper Material enum
   lacks the spear materials in this build.
