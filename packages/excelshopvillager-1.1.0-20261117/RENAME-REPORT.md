# WRV Plugin Renames — Build Report (2026-10-15)

**Scope:** Full identity renames for three homemade plugins, staged only.
**Nothing was installed, the live server was not touched, and nothing was pushed to GitHub.**
All three jars were built locally, verified by reading their `plugin.yml` back out of the
built jars, and boot-tested together on a scratch copy of the local Paper 26.2 runtime
(see `smoke-test-boot.log`).

| Old | New | Old version | New version | Jar |
|---|---|---|---|---|
| MenuJoin | **ExcelMenuJoin** | 1.2.0 | **1.3.0** | `ExcelMenuJoin-1.3.0.jar` |
| BedrockMenu | **ExcelBedrockMenu** | 1.4.2-menu.1 | **1.5.0** | `ExcelBedrockMenu-1.5.0.jar` |
| ShopVillager | **ExcelShopVillager** | 1.0.0 | **1.1.0** | `ExcelShopVillager-1.1.0.jar` |

SHA-256 of the staged jars:

```
89ff6bf2d92cdd9346121a746f8024c4c4ed1044512b9a0dd55fff4fd8f85fd7  ExcelMenuJoin-1.3.0.jar
c1c75d89e85a006550261dc41f3b37f18cbb13d793fa62c1fbde114f10ddf87a  ExcelBedrockMenu-1.5.0.jar
8bd49829b272d183c934facad70b966e5263267ef8d7811bdb218962d9486a41  ExcelShopVillager-1.1.0.jar
```

Renamed sources are staged under `src/ExcelMenuJoin/`, `src/ExcelBedrockMenu/`,
`src/ExcelShopVillager/` (each with `plugin.yml` and a `build.sh`). `build.sh` in this
folder is the harness that produced the jars (JDK 25 javac, `--release 21`, Paper API
1.21.4 snapshot + PlaceholderAPI 2.12.3 from the ExcelShops staged libs).

---

## 1. ExcelMenuJoin 1.3.0 (was MenuJoin 1.2.0)

**Source used:** GitHub repo `ElMikeExcel/wrv-plugins` (default branch, commit
`c4fca028f223c5104d9fb25c0a36f429c50a7879`), `MenuJoin/` project: `MenuJoin.java`,
`MenuJoinExpansion.java`, `plugin.yml`.
**Currency:** the local `~/workspace/menujoin/` tree is STALE (plugin.yml 1.1.0, no
`/compass` command) and was **not** used. The repo source is the true 1.2.0: its header
reads "MenuJoin 1.2.0 - rebuilt from decompiled 1.1.0 (Argus) plus respawn/death/drop/compass
features", and its `plugin.yml` (version 1.2.0, includes the `compass` command and
`menujoin.compass` permission) matches the live `MenuJoin-1.2.0.jar` in the pre-guild
backup exactly.

**What changed:**
- Package `com.winkreyesvalley.menujoin` → `com.winkreyesvalley.excelmenujoin`;
  classes `MenuJoin` → `ExcelMenuJoin`, `MenuJoinExpansion` → `ExcelMenuJoinExpansion`.
- `plugin.yml`: name `ExcelMenuJoin`, version `1.3.0`, canonical command renamed to
  `excelmenujoin`, permissions renamed to `excelmenujoin.toggle` / `excelmenujoin.compass`
  (defaults unchanged: `true` / `true`).
- All features carried over unchanged: compass on join and respawn, compass removed from
  death drops, compass cannot be dropped, `/compass` replacement command, auto-open toggle.

**Compat shims (all verified in the compiled classes and in the boot test):**
- (a) PlaceholderAPI expansion identifier stays **`menujoin`** → `%menujoin_status%` in
  DeluxeMenus keeps working untouched. Boot log: `Successfully registered internal
  expansion: menujoin [1.3.0]`; `papi list` shows `menujoin` active.
- (b) `/menujoin` is a registered **alias** of `/excelmenujoin`. Boot test: both commands
  answer from the console with the toggle handler's "Only players can use this command."
- (c) Compass PDC: new compasses are stamped with the new key
  `excelmenujoin:menu_compass` **and dual-stamped** with the legacy key
  `menujoin:menu_compass`. `isMenuCompass` accepts either stamp. On join, any carried
  compass holding only the legacy stamp gets the new stamp added (restamp) and keeps the
  legacy stamp, so no reader of either namespace ever loses recognition.
- (d) Toggle state: MenuJoin stores it in the **player PDC**, not in files (verified: the
  production `plugins/MenuJoin/` folder does not exist in the pre-guild backup). The
  renamed plugin reads `excelmenujoin:autojoin_disabled` first and falls back to the
  legacy `menujoin:autojoin_disabled`; writes set/remove **both** keys, so the state is
  identical for new and legacy readers (including a rollback to MenuJoin 1.2.0). A
  data-folder copy migration is also implemented (below) in case files ever exist.
- **One deliberate behavior change (flagged):** `onCompassUse` now returns early when the
  interact event was already cancelled by an earlier handler. MenuJoin 1.2.0 did not check
  this, which is what would have made the menu pop open over ExcelShops' shop-sign taps.
  This is the cancellation fix that was already scoped for MenuJoin in this same window;
  it changes nothing unless another plugin cancels the event first.

**Data migration:** on first enable, if `plugins/ExcelMenuJoin/` has no files and
`plugins/MenuJoin/` exists, its contents are copied across (never overwrites existing
new-folder content). Boot test seeded a marker file: copied, log line
`Migrated legacy data folder plugins/MenuJoin -> plugins/ExcelMenuJoin`.

---

## 2. ExcelBedrockMenu 1.5.0 (was BedrockMenu 1.4.2-menu.1)

**Source used:** local source tree `~/workspace/bedrockmenu/src/` (the 1.4.1 source),
with the 1.4.2 button patch applied at source level.
**Currency:** the repo holds no 1.4.2 `.java` source — 1.4.2 was produced by ASM-patching
the compiled 1.4.1 classes (`BedrockMenu/tools/Patch.java`, which rewrites the
`BUTTONS`/`COMMANDS` arrays in `<clinit>`), and the live line `1.4.2-menu.1` is a further
hand tweak of `plugin.yml` (version string + description) on top of that. Evidence chain:
(1) `Patch.java` gives the exact 1.4.2 arrays; (2) the live jar's class strings match those
arrays exactly; (3) the local 1.4.1 classes' strings differ from the live jar **only** by
those arrays (plus whitespace noise in `strings` output), so the local source is a faithful
1.4.1. The compiled `ExcelBedrockMenu` class was string-diffed against the live
1.4.2-menu.1 class: the only differences are the rename identity strings, the enabled-log
line, and the new migration code. The village fallback coordinates (-784, 70, -272) are
hardcoded constants in the source and are unchanged.

**What changed:** package `com.winkreyes.bedrockmenu` → `com.winkreyes.excelbedrockmenu`;
classes `BedrockMenu` → `ExcelBedrockMenu` (`TeleportMenu`, `GeyserForms` renamed by
package only, logic untouched); `plugin.yml` name/version/description based on the **live
jar's** `plugin.yml` (not the repo's `plugin-1.4.2.yml`, whose version/description differ
from what is actually installed). The 8-button menu is exactly the live one: Spawn,
Teleport to a player, Spawn Shop (`shop`), Jobs & Money, mcMMO Stats, Leaderboard,
Server Info, Auto-open on join (which runs the `menujoin` command — still valid via the
ExcelMenuJoin alias). **No behavior changes.**

**Data migration:** same copy-on-first-enable helper, from `plugins/BedrockMenu/`.
Preserves its `config.yml` exactly (boot test: byte-identical copy). Note: no live
BedrockMenu class actually reads that `config.yml` (`auto-open: {}`) — it appears to be a
leftover — but it is migrated anyway, untouched.

---

## 3. ExcelShopVillager 1.1.0 (was ShopVillager 1.0.0)

**Source used:** GitHub repo `ElMikeExcel/wrv-plugins`, `ShopVillager/` project
(`src/com/winkreyes/shopvillager/ShopVillager.java`, `plugin.yml`, `config.yml`).
There is **no** ShopVillager source anywhere under `~/workspace` outside the repo.
**Currency:** the repo source matches the live jar (class strings confirm the
`wrv_spawnshop` scoreboard tag, the `open-command` config read with default
`dm open spawnshop %player%`, the debug flag, and the enable message). Note for the
record: contrary to the original brief, this plugin does **not** call ExcelShops' UI — a
tap dispatches the configured console command (DeluxeMenus `dm open spawnshop %player%`
by default).

**What changed:** package `com.winkreyes.shopvillager` → `com.winkreyes.excelshopvillager`;
class `ShopVillager` → `ExcelShopVillager`; `plugin.yml` name/version, canonical command
`excelshopvillager` with **`shopvillager` kept as an alias**, permission renamed to
`excelshopvillager.admin` (default `op`, unchanged). The villager binding tag
`wrv_spawnshop` is **unchanged** — bindings live on the entities in world data, so the
existing spawn villager keeps working with zero re-tagging. The villager's visible name
("Spawn Shop", set by the `create` subcommand) is **not** changed in this task, per
instructions. **No behavior changes.**

**Data migration:** same copy-on-first-enable helper, from `plugins/ShopVillager/`,
running **before** `saveDefaultConfig()` so the migrated `config.yml` is never
overwritten by the bundled default. Boot test: byte-identical copy of the live
`config.yml`.

---

## Tests and verification

**Automated tests:** none of the three projects ship any automated tests (checked in both
the repo projects and the local trees — there are no test sources to run). Verification
was therefore done the Moon-playbook way instead:

1. **Clean compiles** of all three renamed source trees (javac `--release 21`); only
   deprecation notes, same class as the originals (legacy `NamespacedKey(String, String)`
   constructor used deliberately for the legacy-namespace lookups).
2. **plugin.yml read-back from each built jar** (name, version, main class all show the
   new identity — see the table above; hashes recorded at the top).
3. **Bytecode parity for BedrockMenu** against the live jar (details in §2).
4. **Boot smoke test** (Paper 26.2 build 123, scratch copy of the local runtime;
   plugins: DeluxeMenus 1.14.1, PlaceholderAPI 2.12.3, and the three renamed jars; legacy
   data folders seeded with the real live configs plus a marker file). Full log:
   `smoke-test-boot.log`. Results:
   - All three plugins load and enable under their new names/versions; zero errors or
     warnings mentioning them.
   - All three data migrations fired and copied correctly (see §§1–3).
   - PlaceholderAPI registered the expansion under the legacy identifier:
     `menujoin [1.3.0]`; `%menujoin_status%` hook logged by ExcelMenuJoin.
   - Console probes: `excelmenujoin`, `menujoin` (alias), `compass` →
     "Only players can use this command."; `excelshopvillager`, `shopvillager` (alias) →
     "Run this in-game."; `menu` → "Only players can use /menu."
   - The only ERROR lines in the log are sandbox network artifacts (PlaceholderAPI could
     not download its hash list / update info through the proxy) — unrelated to these
     plugins and expected in this environment.
5. **Not covered by the smoke test** (needs a real client, so it is an install-day
   in-game check): compass tap behavior, the PDC toggle read on a real pre-rename player
   profile, Bedrock form rendering, and the villager tap. The code paths are unchanged
   from the live versions except where this report says otherwise.

---

## Install-day steps (for the future install window)

Do all three in the same window (one restart), alongside ExcelShops 1.2:

1. Stop the server (panel Shut Down, with the usual nobody-online check).
2. Delete the old jars: `MenuJoin-1.2.0.jar`, `BedrockMenu-1.4.2-menu.1.jar`,
   `ShopVillager-1.0.0.jar`.
3. Add the three new jars from this folder. Verify SHA-256 against the values above
   after upload.
4. **Leave the old data folders in place** (`plugins/MenuJoin/`, `plugins/BedrockMenu/`,
   `plugins/ShopVillager/`). Each renamed plugin copies what it needs on first enable;
   the copies are non-destructive and the originals stay as the rollback record. (In
   practice `plugins/MenuJoin/` does not exist — its state is in player PDC and is
   handled by the dual-key compat in §1.)
5. Start the server and check the log for:
   - `Migrated legacy data folder plugins/BedrockMenu -> plugins/ExcelBedrockMenu`
   - `Migrated legacy data folder plugins/ShopVillager -> plugins/ExcelShopVillager`
   - `Successfully registered internal expansion: menujoin [1.3.0]`
   - the three "enabled" lines, and no stack traces naming the Excel plugins.
6. In-game checks: compass tap opens the menu; a pre-rename compass (legacy stamp only)
   still opens the menu and gains the new stamp on join; `/menujoin` and `/excelmenujoin`
   both toggle auto-open and the DeluxeMenus toggle display (`%menujoin_status%`) follows;
   Bedrock `/menu` shows all 8 buttons including Spawn Shop; tapping the spawn villager
   opens the shop; with ExcelShops 1.2 in the same window, tapping a shop sign while
   holding the compass goes to ExcelShops' manage screen instead of opening the menu
   (that is the cancellation fix in §1 working).
7. After a verified first boot, the old data folders may be archived/deleted.
   **Rollback:** remove the new jars, restore the old jars, restart. Player toggle state
   survives the rollback because ExcelMenuJoin dual-writes the legacy PDC key; the copied
   data folders are untouched originals.

## Coordination notes / non-blockers

- **ExcelShops compass check:** ExcelShops looks for `menujoin:menu_compass`. Because
  ExcelMenuJoin dual-stamps compasses (new + legacy keys) and restamps old ones without
  removing the legacy stamp, ExcelShops' existing check keeps working with **no change
  required**. If ExcelShops is ever revised, it should accept `excelmenujoin:menu_compass`
  as well; the legacy stamp can be dropped only after that.
- **Permission nodes renamed** (`menujoin.*` → `excelmenujoin.*`,
  `shopvillager.admin` → `excelshopvillager.admin`) with identical defaults. If LuckPerms
  ever gets explicit grants on the old nodes, they would need re-granting; today the
  defaults carry everything.
- **DeluxeMenus `server_menu.yml`** needs no edit for these renames: `%menujoin_status%`
  and its `[player] menujoin` click command both keep working (expansion identifier +
  command alias shims).
- BedrockMenu's production line had two `plugin.yml` variants (repo tool file vs the
  live jar). The rename follows the **live jar**, which is what players actually run.
