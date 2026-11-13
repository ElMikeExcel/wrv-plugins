# RIG RESULTS — ExcelMenu 3.0.1

**Seed:** `rig/seed-301/` = the post-3.0.0 live data folder
(rig-bm292 seed-r2: 93 entries, GROUPED `server_menu.yml` with
pins `[spawn, tpmenu]`, 7,182 B / `9af8ddef…`) with ONLY
`config.yml` replaced by the staged 3.0.1 config (3,215 B /
`c0467b5f…`, palette + `pin-color`).

## Model harness — ALL PASS (12 CHECKs, 0 FAIL)

`rig/RigHarness301.java` → `rig/harness-301.log`. The palette is
read from the seed's own config.yml, not hardcoded; the 3.0.1 pure
core renders against the live seed:

- Staged config carries the approved palette
  (travel `&a`, me `&d`, shops `&6`, help `&c`, settings `&e`,
  chest_tools `&8`, pin `&1`).
- Seed entries load: 93, 0 failed, magiclevel absent; both layouts load.
- Picker: `§1Spawn, §1Teleport to a player, §aTravel, §dMe,
  §6Shops, §cHelp, §eSettings, §8Chest Organization, Rearrange
  Menu` — 2 pins + 6 sections + Rearrange, no Back; no section
  button carries blue.
- Every section form carries its section colour, Back plain, body
  listing plain; Travel form excludes the root-only pins
  (`§aParty, §aAdventurers Guild, §aEliteMobs, Back`).
- Flat form: 35 buttons, pins blue first, entries section-coloured.
- Commands catalogue picker + group form stay plain (no colours
  configured for its groups).
- Java chest pages build (parity: Java untouched); header names
  print unchanged: page 1 `&b&lTravel, &a&lMe, &e&lShops,
  &6&lHelp, &7&lSettings, &f&lToggles, &a&lChest Organization`;
  page 2 `&e&lChest Organization, &b&lRearrange Menu,
  &c&lBack to Server Menu`.

## Paper boot — PASS

`rig/boot-301.sh` → `rig/boot-301.log`. Paper 26.3 + the 3.0.1 jar
+ seed-301 + live companions (Geyser, PlaceholderAPI, Magic,
Vault, mcMMO, ExcelMagicLevel, ExcelSkillsPanel 0.2.1):

- `[ExcelMenu] ExcelMenu 3.0.1 enabled … 93 entries, 2 registry
  menus, 13 definition menus live.`
- `Done (56.526s)!` — **SEVERE 0**, 0 ExcelMenu ERRORs, clean stop.
- Console smokes: `plugins` → 8 plugins; `excelmenu list` → 13
  definition menus; `excelmenu entries server_menu` from console
  answers the expected console-limitation line.
- The log's ERROR/WARN lines are sandbox-network noise (Mojang /
  PlaceholderAPI update checks failing on SSL in this offline
  sandbox), not plugin errors.

## Not exercised (honest scope)

No Bedrock client exists in this lane: the in-game visual proof
(picker + section form colours as Mike sees them) is a post-install
player proof owed by Mike, not a filing gate. No live server was
touched; no PC mirror (R3) run was needed — the change is
rendering + config data, fully covered by the model harness and
the desk Paper boot.
