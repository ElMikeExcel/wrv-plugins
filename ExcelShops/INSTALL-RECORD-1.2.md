# ExcelShops 1.2 suite — install record

> **INSTALL RECORD — published to `wrv-plugins` as a draft PR,
> October 16, 2026** (this repo's live-version-record pattern; draft
> PRs here stay unmerged). Published under the owner's explicit
> GitHub authorization for the Oct 16, 2026 overnight queue.
> The in-game checks in §9 are still open and are stated as open
> wherever they apply.

## Status at a glance

| Status | State |
|---|---|
| Built | **Yes** — October 15, 2026 |
| Automated tests | **363/363 green** (ExcelShops, via the project's `local-build.sh` on the final tree: 348 flow-pass tests + 15 compass tests) |
| Test server (scratch boot) | **Passed** — two boots (plugin set; live-faithful set with the live Essentials pairing), see §7 |
| Installed live | **Yes** — October 15, 2026, ~10:19 PM CDT, via auto-watch on an empty server (§8) |
| Java check | **NOT DONE YET** — owner's in-game checks pending (§9) |
| Bedrock check | **NOT DONE YET** — owner's in-game checks pending (§9) |

## 1. What this window installed

One restart swapped four jars and the Java menu file, and activated
a despawn setting saved earlier the same day.

| Artifact | From → To | SHA-256 |
|---|---|---|
| `ExcelShops-1.2.jar` | 1.1 → **1.2** | `f72f9f819797b6c9cab613aa755861119327e00e224b5639d0c6ee673a156a53` |
| `ExcelMenuJoin-1.3.0.jar` | MenuJoin 1.2.0 → **ExcelMenuJoin 1.3.0** (rename) | `89ff6bf2d92cdd9346121a746f8024c4c4ed1044512b9a0dd55fff4fd8f85fd7` |
| `ExcelBedrockMenu-1.5.0.jar` | BedrockMenu 1.4.2-menu.1 → **ExcelBedrockMenu 1.5.0** (rename) | `c1c75d89e85a006550261dc41f3b37f18cbb13d793fa62c1fbde114f10ddf87a` |
| `ExcelShopVillager-1.1.0.jar` | ShopVillager 1.0.0 → **ExcelShopVillager 1.1.0** (rename) | `8bd49829b272d183c934facad70b966e5263267ef8d7811bdb218962d9486a41` |
| `server_menu.yml` (DeluxeMenus) | prepared file, §5 | `94fdc85845b72793907958c8b90232a93ea9e3f98a095eb2f1aeea6fcb3c9991` |

Also activated in this restart: `spigot.yml` item-despawn-rate
**12000 ticks (10 minutes)**, saved earlier on Oct 15.

Identity note, because earlier planning got it wrong and the packet
corrected it against three primary sources: ExcelShops' `plugin.yml`
is `name: ExcelShops`, `main: com.winkreyes.shops.WRVShops`.
**WRVShops is the main class, not the plugin name** — the plugin
keeps its name, its data folder (`plugins/ExcelShops/`), and its
shop registry across the bump, exactly like 1.0 → 1.1.

## 2. ExcelShops 1.2 — what changed vs 1.1

- **Bedrock buy flow, 3 taps:** the item screen leads with
  "Buy {count} × {name} for {total}" and goes straight to the final
  confirmation; the middle Review screen is gone on mobile.
  "Change quantity" opens a preset screen (1/2/4/8/16/32/64 ≤ max,
  each labeled with its total) plus the existing exact-amount entry.
  The Java flow is unchanged (quantity → Review → confirm).
  Special products (MobVeil, Death Protection) are unchanged.
- **Compass shop shortcut (new; config `compass-shop-shortcut: true`,
  default on):** tapping your own shop's sign with the menu compass
  opens Manage (and does not open the server menu); tapping a sign
  on your own non-shop chest offers shop setup; someone else's shop
  is never intercepted. Accepts both compass stamps
  (`excelmenujoin:menu_compass` and legacy `menujoin:menu_compass`).
- **Titles:** the mobile root and the Java catalog now say
  "Excel Shop".

## 3. The three companions are renames, nothing more

ExcelMenuJoin 1.3.0, ExcelBedrockMenu 1.5.0, and ExcelShopVillager
1.1.0 are the standing house renames of MenuJoin, BedrockMenu, and
ShopVillager: new plugin names and main classes under the Excel
prefix, data folders migrated on first enable (§8 has the live
migration lines), behavior otherwise unchanged. Their sources are
included in this PR under their own folders
(`ExcelMenuJoin/`, `ExcelShopVillager/`); ExcelBedrockMenu's source
record is the separate 1.5.1 record (wrv-plugins PR #24), because
that tree moved straight on to the guild-button release.

ExcelMenuJoin also registers the PlaceholderAPI expansion
`menujoin` (`%menujoin_status%` → `ON`/`OFF`), which the menu
change in §5 is built on.

## 4. ExcelShopVillager / spawn shop

No behavior change in this window beyond the rename. The spawn
villager shortcut (console `dm open spawnshop %player%`, tag
`wrv_spawnshop`) is untouched.

## 5. Menu file changes (DeluxeMenus `server_menu.yml`)

Three changes, verified by diff against the live base (only these
lines differ) and by structural checks:

1. **Spawn button** keeps the namespaced console teleport applied
   live earlier on Oct 15 (`[console] execute in minecraft:overworld
   run minecraft:tp %player_name% -784 80 -336`) — folded into the
   prepared file so installing it could not regress that fix.
2. **Shop button display name:** `&a&lSpawn Shop` → `&a&lExcel Shop`
   (click commands and lore untouched).
3. **Auto-open toggle shows its state:** the single LEVER item at
   slot 16 became two items sharing the slot, gated by
   `view_requirement` string-equals checks on `%menujoin_status%`:
   `autojoin_on` (LIME_DYE, "✔ ON", priority 1) and `autojoin_off`
   (GRAY_DYE, "✘ OFF", priority 2). Same click commands
   (`[player] menujoin` + `[refresh]`), so tapping either state
   toggles.

## 6. Tests

**363/363 green** via `~/workspace/excelshops/local-build.sh`,
re-run on the final tree before packaging (348 flow-pass tests +
15 compass tests). The test sources are in this PR under
`ExcelShops/tests/`.

## 7. Smoke test (scratch Paper 26.2-123, same build as live)

Two boots of the full set with the prepared menu in place and
seeded legacy data folders:

- **Boot 1** (four jars + DeluxeMenus + PlaceholderAPI + Vault):
  all plugins enabled; all three data migrations fired; migrated
  configs byte-identical to the live ones; DeluxeMenus "8 GUI menus
  loaded!"; expansion `menujoin [1.3.0]` registered; ExcelShops
  generated its config with `compass-shop-shortcut: true`. With no
  Vault economy provider in this set, ExcelShops logged its
  designed fail-closed line ("entered protection-only mode;
  purchases/setup unavailable") — shops pause, stock protected;
  not a crash.
- **Boot 2** (boot 1 + the live EssentialsX 2.22.0 pair and data
  folder): `[ExcelShops] ExcelShops enabled. Legacy ChestShop
  confirmation: unavailable/absent. Economy: EssentialsX Economy`,
  Vault hook confirmed, the console `excelshop` probe answering
  normally, clean shutdown. The only ERROR lines in either boot
  are sandbox network artifacts and Essentials' known
  unsupported-version notice — both are live-server baseline noise.

**Verdict: PASS.**

## 8. Install — live execution

- **Route:** auto-watch cron, under the standing install rule:
  only on an empty server, with a restart.
- **Gate:** console `list` twice — **0/10** at 10:12:18 PM and
  10:13:58 PM CDT, October 15, 2026.
- **Sequence:** panel **Shut Down** (not console `stop`); the four
  old jars renamed to `*.bak-20261015`; the four new jars and the
  prepared `server_menu.yml` uploaded (the old menu kept as
  `server_menu.yml.bak-20261015`); panel Start.
- **Live log evidence (verbatim from the install record):**
  - `Done (17.448s)!` at 10:19:41 PM CDT
  - `[ExcelShops] Enabling ExcelShops v1.2`
  - `[ExcelMenuJoin] Enabling ExcelMenuJoin v1.3.0`
  - `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.0`
  - `[ExcelShopVillager] Enabling ExcelShopVillager v1.1.0`
  - `[ExcelShops] ExcelShops v1.2 enabled. 1 shop(s), 1 chest binding(s). Economy: EssentialsX Economy`
  - `[ExcelBedrockMenu] Migrated legacy data folder plugins/BedrockMenu -> plugins/ExcelBedrockMenu`
  - `[ExcelShopVillager] Migrated legacy data folder plugins/ShopVillager -> plugins/ExcelShopVillager`
  - `[PlaceholderAPI] Successfully registered internal expansion: menujoin [1.3.0]`
  - `[DeluxeMenus] 8 GUI menus loaded!`
  - No "protection-only mode" line; no stack traces naming the
    four plugins; no "Error occurred while enabling".
- A player rejoined at 10:20:05 PM, after `Done` — the gate was
  not disturbed.

## 9. Still to check in game — NOT DONE YET

The owner's checks, none of them claimed by this record:

- Bedrock: buy from the Spawn Shop in the 3-tap flow, with real
  money movement.
- Compass: tap your own shop sign → Manage opens; compass behaves
  normally everywhere else.
- Spawn button → lands at the overworld village spawn.
- Auto-open toggle shows ✔ ON / ✘ OFF correctly in both menus.
- Carried from 1.1: a second-player chest-shop purchase is still
  unverified.

## 10. Rollback

At a panel Shut Down, in `plugins/`: delete the four 1.2-suite
jars and rename each `*.bak-20261015` jar back; restore
`server_menu.yml.bak-20261015` over the live menu; Start. The
despawn change is a `spigot.yml` value (12000), revertible
independently. Data folders keep their migrated names; the old
jars read the same data (migration was a folder rename, verified
byte-identical configs in the smoke test).

## 11. Source set in this PR — and one honest exclusion

Included: `ExcelShops/` (src, tests, plugin.yml, config.yml,
spawn-catalog.yml, mobile-menus, pom.xml, LICENSE, local-build.sh),
`ExcelMenuJoin/` and `ExcelShopVillager/` (their rename source
trees as shipped). Excluded on purpose: the project tree's
`README.md` — it is a stale 1.1-era candidate document ("not
installed yet", written against 1.0/1.1) and would misdescribe
this release; this record file carries the 1.2 truth instead.
The full local tree of record is `~/workspace/excelshops/project`
plus the install packet at `~/workspace/excelshops/install-prep/`.

Website status at this record's publication: the site's plugin
table still lists ExcelShops 1.1, ExcelMenuJoin 1.2.0, and
ExcelShopVillager 1.0.0. The version-row + changelog delta is
prepared locally; applying it needs the browser web-upload route
(the connector CLI's ~128 KB single-argument cap cannot carry
the ~192 KB `index.html`), followed by byte-for-byte live
verification.
