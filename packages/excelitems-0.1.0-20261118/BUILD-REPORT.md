# ExcelItems 0.1.0 — BUILD REPORT

Built Wed Nov 18, 2026, by board supervisor run 555 worker W3, per
backlog item P-ITEM-REGISTRY-1. STAGED ONLY — not filed to MANIFEST,
not installed, live server and panel untouched, and no other plugin
(ExcelShops / ExcelDowned / ExcelMobVeil / ExcelDeathProtection)
was modified in any way.

## Spec sources read (before building)

1. `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
   line 981 — the full P-ITEM-REGISTRY-1 section: registry as data
   with permanent ID + name, identity in the item's own data (PDC),
   behavior written once in code and registered against the ID,
   shops/menus/loot reference items by ID in config (replacing the
   code-pinned MobileCatalog special-product pattern), first
   migration = the four existing special products with legacy
   identity marks on already-held items still recognized.
2. `~/MEMORY.md` line 26 — CUSTOM ITEM REGISTRY — DESIGN LAW
   (Mike, Nov 17, 2026, ~9:28 PM + ~9:29 PM amendment): ExcelItems
   ships as a STANDALONE dependency plugin; ExcelShops/ExcelDowned/
   future Excel plugins hard-depend on it (plugin.yml depend);
   ExcelItems stays FREE, small, stable, boring — only genuinely
   shared services live there; selling model = ExcelItems free,
   products on top paid (Vault/PlaceholderAPI pattern).
3. Build conventions mirrored from the most recent Excel build,
   `~/workspace/tdguard-build-20261118/` (BUILD-RECORD.md + the
   src/ExcelTDGuard tree): Bukkit-free core + thin Bukkit layer,
   plain-JVM Check harness, deterministic build.sh (fixed entry
   order, fixed jar timestamp 2026-11-18T00:00:00Z, no manifest),
   double-build + clean-room rebuild from the source zip.

No contradiction was found between the backlog spec and the task
brief; the backlog's "do NOT re-platform the finished revive build
before its mirror rig" caution is honored — this unit touches no
other plugin.

## The four existing products — construction of record

Read from the owning plugins' mint code and the shop surfaces:

- **Veil Potion** — `~/workspace/wrv-handoff-2026-10-14/extracted/MobVeil-0.1.0-verified-rebuild-reference/ExcelMobVeil/src/com/winkreyes/excelmobveil/ExcelMobVeil.java`
  `createPotion`: Material.POTION, base PotionType.WATER, colour
  0x63A8AA, EnchantmentGlintOverride, display AQUA+BOLD "Veil
  Potion", 6 lore lines, PDC key `excelmobveil:potion` BYTE 1.
  Shop side: `~/workspace/excelshops/project/src/com/winkreyes/shops/MobileCatalog.java`
  (id `veil_potion`, Route.VEIL, console `mobveil buy %player_name%`)
  and the spawn-shop menu (POTION, price $2,000 in menu lore).
- **Death Protection basic** — `~/workspace/deathprotection-020-build-20261112/build-0.2.0/ExcelDeathProtection/src/com/winkreyes/exceldeathprotection/ExcelDeathProtection.java`:
  basic is NOT a physical item. `deliverBasic` sets Tier.BASIC on
  the player immediately (boss-bar indicator); the shop menu's
  CHEST (`~/workspace/downed-100-filing/staged/spawnshop_potions_protection.yml`,
  name "&a&lDeath Protection", $15,000, console
  `deathprotection buy basic %player_name%`) is a display icon only.
- **Death potion (premium)** — same 0.2.0 source, `createPotion` +
  `updatePotionText`: Material.POTION, colour 0x7D3C98, glint,
  display LIGHT_PURPLE+BOLD "+Death Protection Potion", 8 lore
  lines, PDC `exceldeathprotection:potion` BYTE 1 +
  `exceldeathprotection:lore_version` INTEGER 2 (LORE_VERSION = 2).
  Shop id `deathpotion`, Route.PROTECTION_POTION, console
  `deathprotection buy premium %player_name%`, price $30,000.
  (The older 0.1.1 tree at `~/workspace/excel-death-protection/`
  carries the same potion shape with the earlier 7-line lore; the
  registry pins the current 0.2.0 text.)
- **Revive Potion** — `~/workspace/downed-016-build-20261117/work/build-1.0.0/src/com/winkreyes/exceldowned/RevivePotionItem.java`
  `create`: Material.POTION, colour 0xFF4D6D, display
  LIGHT_PURPLE+BOLD "Revive Potion", 5 lore lines (last line
  formats the ExcelDowned config price, default 5000), PDC
  `exceldowned:revive_potion` BYTE 1 +
  `exceldowned:revive_potion_lore` INTEGER 1 (LORE_VERSION = 1).
  Shop side: staged menu `revive_potion` slot 6, console
  `downed revivepotion buy %player_name%`, price $5,000 (config
  source of truth in `~/workspace/downed-100-filing/staged/ExcelDowned-config-1.0.0.yml`).

Prices are SHOP facts and are deliberately NOT stored in the item
definitions (the revive lore's price line is the one exception — it
is part of the minted item's own text; see decision D3).

## What was built

ExcelItems 0.1.0, package `com.winkreyes.excelitems`, 8 main classes
+ 1 inner enum, 864 lines of Java:

- Core (pure JVM): `core/ItemDefinition` (the data record: permanent
  ID, key, display name, material, potion colour/base, glint, lore,
  kind ITEM|ACTIVATION, legacy mark, handler id; validated at
  construction), `core/ItemRegistry` (register with duplicate-ID /
  duplicate-key / duplicate-legacy-mark rejection, byId/byKey,
  identify from primary or legacy marks, nextId, and the config
  assignment validator), `core/BuiltinItems` (the four definitions).
- Bukkit layer: `ExcelItems` (bootstrap + services-manager API
  registration + fail-closed assignment validation at enable),
  `ItemFactory` (mint + identify), `ItemsCommand`
  (`/excelitems status|list|give|identify`, permission
  `excelitems.admin`, default op).
- API: `api/ExcelItemsAPI` (create by ID/key, identify an ItemStack
  from its own data, register a definition, register/look up a
  behavior handler, nextId) + `api/ItemBehavior`. Other plugins get
  the API via `ExcelItemsAPI.get()` (Bukkit services) after
  declaring `depend: [ExcelItems]`.

Identity model: minting stamps the primary mark
`excelitems:id` (INTEGER) AND the definition's legacy mark (same
namespace/key/BYTE 1, plus the legacy lore-version stamp where one
exists), so a registry-minted item is recognized by its legacy
owning plugin unchanged. Identification reads the primary mark
first; only an item with NO primary mark falls back to legacy marks
— already-held pre-registry items stay recognized, and a stale or
forged primary mark never falls through to a second identity.

Persistence/config shape: `config.yml` carries the ID assignments
(`items: {1: veil_potion, 2: deathprotection_basic, 3: deathpotion,
4: revive_potion}`) as the persisted record of permanence. At
enable the assignments are validated against the code-registered
definitions; any unknown ID, key mismatch, or missing ID disables
the plugin with SEVERE lines (fail closed). New items take
`nextId()` (max + 1) and a config line in the same pass; retired
IDs are never reused.

## Registry IDs assigned (permanent)

| ID | Key | Kind | Material | Legacy mark |
|----|-----|------|----------|-------------|
| 1 | veil_potion | ITEM | POTION (0x63A8AA, WATER, glint) | excelmobveil:potion |
| 2 | deathprotection_basic | ACTIVATION | CHEST (display only) | — none (no item exists) |
| 3 | deathpotion | ITEM | POTION (0x7D3C98, glint) | exceldeathprotection:potion + lore_version 2 |
| 4 | revive_potion | ITEM | POTION (0xFF4D6D) | exceldowned:revive_potion + revive_potion_lore 1 |

IDs follow shop-menu order (veil, basic, premium potion, revive).

## Automated tests (plain JVM, house Check harness)

**94 checks, 0 failures** — also re-run green on the clean-room tree.

| Suite | Checks | Pins |
|---|---|---|
| RegistryTest | 43 | register/create/identify round-trip by ID, key, primary mark and legacy mark for all builtins; duplicate-ID, duplicate-key and duplicate-legacy-mark rejection; id 0/negative and malformed-key rejection; unknown-ID / unknown-key / unknown-mark handling (empty lookups, require* throws); primary mark never falls through to legacy; nextId semantics |
| BuiltinItemsTest | 34 | the four definitions pinned to the mint-code facts above (materials, colours, names, lore line counts, legacy namespaces/keys, lore versions, basic = ACTIVATION with no legacy mark) |
| AssignmentsTest | 7 | shipped assignments validate clean; swapped key, unknown ID, missing ID, non-numeric key, empty/null map each flagged |
| PluginYmlGuardTest | 10 | name/main/version pins, command + permission, NO hard depend (ExcelItems IS the dependency), config carries the 4 assignments |

Full output: `./run-tests.sh` in `src/ExcelItems/` (this run's
totals above; test run exit code 0).

## Deterministic build

- Build ID: `ExcelItems-0.1.0` (fixed entry order, fixed timestamp
  2026-11-18T00:00:00Z, no manifest — house pattern).
- **Double-build: two consecutive builds byte-identical.**
- **Clean-room rebuild from the source zip: byte-identical, tests
  94/94.** Three-way match (build 1 = build 2 = clean-room).
- Jar census: exactly 9 class entries (8 classes + ItemDefinition$Kind)
  plus `plugin.yml` and `config.yml`.

## Design decisions made in this unit (for the supervisor's ledger)

- **D1 — Basic is ACTIVATION kind, not an item.** The brief's four
  products include one with no physical item (basic protection
  activates on purchase). Rather than inventing an item, the
  definition records kind ACTIVATION with the menu's CHEST as its
  display/representative stack; `create()` yields that display
  stack and delivery stays with ExcelDeathProtection. Alternative
  considered: excluding basic from 0.1.0 — rejected, the backlog
  names all four as the first migration.
- **D2 — Minting dual-stamps primary + legacy marks.** A
  registry-minted item carries both `excelitems:id` and its legacy
  mark, so legacy owners recognize it during the migration era.
  Alternative: primary-only stamping — rejected, it would break
  recognition by the un-migrated plugins the moment anything mints
  through the registry.
- **D3 — Prices stay out of definitions.** Price is a shop/config
  fact (the revive price already drifted menu-vs-config once in
  the record); definitions carry construction only. The revive
  definition's last lore line pins the default "$5,000" text
  because that line is part of the minted item itself.
- **D4 — Assignments live in config.yml and fail closed.** The
  persisted ID shape is the plugin's own config, validated at
  enable; drift disables the plugin rather than re-pointing an ID.
  Alternative: a separate items.yml data file — rejected for 0.1.0
  (one more file to gate at filing; config.yml is the house shape).
- **D5 — Behavior handlers are a routing table only in 0.1.0.**
  `registerBehavior(id, handler)` exists and definitions carry
  their handler ids, but no behavior code moved out of the owning
  plugins — per the task scope (definitions only; migration of
  ExcelShops/ExcelDowned is a later unit).

## Artifact (structured evidence)

- Jar: `ExcelItems-0.1.0.jar` — **20,214 bytes**
  SHA-256 `85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d`
  MD5 `ed68d6b14e04055ab851c0c317052974`
- Source zip: `ExcelItems-0.1.0-source.zip` — **22,709 bytes**
  SHA-256 `0c5f20b9e2a1cd8b6894296df5880bd977ebbccb36bd040c9d6dd6042d027001`
  MD5 `b3357c1611ce1ebcb3b7762dc030b7a7`
- Hashes also in `SHA256SUMS` beside this report.
- Source tree: `src/ExcelItems/` (src/, tests/, plugin.yml,
  config.yml, build.sh, run-tests.sh). Rebuild: `./build.sh`;
  tests: `./run-tests.sh`.

## Not done here (by task instruction)

No MANIFEST filing, no live/panel contact, no migration of
ExcelShops/ExcelDowned (their plugin.yml depend lines, MobileCatalog
re-pointing, and mint re-routing are the later migration unit), no
backlog/state/board edits. Owed downstream (supervisor chain):
mirror rig (boot SEVERE 0, `/excelitems list`, give+identify of
each ID in game, a legacy-held veil/death/revive item identifying
via its legacy mark), then G1 filing, then the migration units.
One compile note: the Bukkit layer compiles with the house
deprecation note only (legacy display-name/lore APIs, the same
idiom every current WRV plugin mints with).
