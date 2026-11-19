# BUILD RECORD — M4: ExcelShops → ExcelItems migration (IDs 1–4 by reference)

- Worker: M4 build worker, board supervisor run 559
- Date: Wed Nov 18, 2026, ~7:50–8:15 PM CST (America/Chicago)
- Scope read: `~/workspace/board-r558/W3-MIGRATION-SCOPE.md` §3 M4,
  §4.1 G-1..G-6, §4.2 M4 gates, §4.4 surfaces, §6; supervisor
  decisions D124 (M4 delivery = owner console command,
  registry-resolved; version bump per house convention). M1/M2/M3
  contracts FROZEN (Downed 1.0.1, DeathProtection 0.2.1, MobVeil
  0.1.1; mint fully delegated to ExcelItemsAPI.create — Shops
  mints nothing, it only references).
- Deliverable: this folder. NOT filed, NOT installed, no GitHub
  writes, no panel action.

## Verdict

```
BUILT — ExcelShops 1.3.11 (M4 migration candidate)
  Candidate: ExcelShops-1.3.11.jar — 277,494 B /
    91b8559bf63cf2b03aff53a1d7180cffd881d505852ee3c08e0721becc735102
  Tests: 453/453 PASS (443 baseline revivepatch suite + 10 new
    M4 tests), in TWO independent build.sh runs, jars
    byte-identical across both runs.
  G-1 PASS EXACT · G-2 PASS EXACT · G-3 PASS ·
  M4 gate 1 (catalog invariant 22 + registry-ID guard) PASS (desk leg)
  M4 gate 2 (config-only addition demo) PASS (desk harness, reverted)
  G-4: mirror boot OWED (PC in use — desk smoke instead, see below)
  Status: Built (not Filed). Filing is the supervisor's dispatch.
```

## Version bump (D124 house convention, stated per instruction)

ExcelShops **1.3.10 → 1.3.11** (patch bump: a hard depend +
catalog rewire with no price/slot/behavior change — the 1.3.10
revivepatch no-bump precedent was a presentation patch on an
unchanged plugin.yml; this build changes plugin.yml itself, so
it bumps, exactly as M1–M3 did). plugin.yml version +
description carry 1.3.11; the AdminCommandTest version pin was
updated to match. ExcelItems does NOT bump (no API gap —
`definition(id)` / `definitions()` in 0.1.0 suffice).

## G-1 — live-premise fresh pull (PASS, receipts in RECEIPTS.md)

Homebase route, probe PASS attempt 1 (DESKTOP-IL05DQR, no
fallback), ~7:50 PM CST: live `ExcelShops-1.3.10.jar` pulled at
**268,988 B / `0893dd43…` — EXACT** vs the revivepatch of record;
live `ExcelItems-0.1.0.jar` **20,214 B / `85dbb9ea…` — EXACT**;
plugin.yml extracted from the pulled jar is byte-identical to
the patch tree's. The data-folder config.yml / spawn-catalog.yml
differ from the jar-default tree copies (live runtime evolution:
session-proof enabled, Wave-A offers, mending $1,500) — not a
build-base mismatch; the build base is the jar, and the migrated
loader is written for exactly this live state (see delta note 2).

## G-2 — base-gate byte-identical rebuild (PASS)

Unmodified revivepatch tree (`board-r509/excelshops-patch-tree`,
copied to `work/base-gate-tree/`) built with `work/build.sh`:
tests **443/443**, packaged jar **268,988 B / `0893dd43…` —
EXACT vs the G-1 live pull**. Log: `work/base-gate-build.log`.
The migration delta is therefore fully isolable.

## The migration delta (vs the base-gate tree; 5 code/config files + tests)

1. **`plugin.yml`** — version 1.3.11 (+ description note);
   **`depend: [ExcelItems]`** added (hard depend). softdepend
   list otherwise unchanged.
2. **`config.yml` + new bundled `special-products.yml`** — the
   special-product listings are now CONFIG. Each listing names
   product, category, **`excelitems_id` (1|2|3|4)**, price (a
   shop fact, per D123 D3 — never part of the item definition),
   the owner console-command template, and the owning plugin.
   Resolution order at load: the data-folder config's
   `special-products` section when present (complete and
   authoritative; `/excelshopadmin reload` re-reads it), else
   the bundled resource — which is what the current live config
   (no such section) will use. A test pins the two files'
   listings identical.
3. **New `SpecialProducts.java`** — parses + resolves the config
   against the registry (live `ExcelItemsAPI.definition(id)`;
   headless fallback to the registry's own builtin table, the
   M2/M3 pattern). Fail-closed on: unknown registry ID, missing
   category/delivery/owner, delivery without `%player_name%`,
   duplicate listing, empty config. Exposes the protected-key
   set (listing product ids ∪ listing registry keys ∪ ALL
   registered keys) for the shadow guard.
4. **`MobileCatalog.java`** — the code-pinned pattern is gone:
   - `Route` is now `{ORDINARY, SPECIAL}` — one SPECIAL route;
     which item a special sells is its registry ID (data), never
     a code branch. The per-id `switch` on product-id strings
     and the per-route command `switch` are deleted.
   - The potions_protection shelf's code-pinned product list is
     deleted; the shelf is built from the config listings in
     config order (veil, basic, deathpotion, revive — shelf
     order unchanged).
   - Special name/lore come from the **registry definition**
     (plain(displayName) / definition lore); the menu entry is
     still validated — but the expected command is the
     listing-resolved `menuCommand()`, not a hardcoded string,
     and the menu-displayed price must equal the config price
     (drift fails closed both ways).
   - **Catalog invariant re-expressed against registry IDs:**
     the constructor requires every baseline registry ID 1–4
     to be listed at least once (a dropped listing fails
     closed) and the shipped configuration yields exactly the
     22-product baseline (18 ordinary + 4 specials), pinned by
     test. The old `products.size() != 22` code pin is gone —
     a config that lists an additional registered ID loads at
     23 (see the demo), which is the design law working.
   - **Never-created/repriced/shadowed guard re-expressed
     against registry IDs:** `derive()` throws the existing
     fail-closed error when a spawn-catalog offer id matches
     ANY protected key (a listing's product id or any
     registered item key); specials are never repriced by
     derive (only ORDINARY products reconcile to catalog
     prices) and never created by it.
5. **`ShopUi.java`** — `mobileSpecialReview`'s two hardcoded
   switches (route→command, route→dependency plugin) replaced
   by the ONE registry-resolved delivery path:
   `product.deliveryCommand(name)` + `product.ownerPlugin()`,
   both from the listing. All other ShopUi behavior (fence,
   token consumption, name safety checks) untouched — the
   existing dispatch tests pass against the new path,
   including exactly-one-dispatch of `mobveil buy Buyer` and
   `deathprotection buy basic|premium Buyer`.

**Unchanged by design (verified):** bundled `mobile-menus/*`
inside the candidate jar are **byte-identical to the live jar's**
(diff clean) — shelf slots/prices unchanged (D112: Revive
Bedrock-validation slot 5 in the bundled menu; the Java
ExcelMenu live files are not in this jar and are untouched,
Revive Java slot 6 / Lost & Found untouched). spawn-catalog.yml
default, LICENSE, and every other source file byte-identical to
the base tree. No ExcelItems classes are bundled into the
candidate jar (`unzip -l` shows zero excelitems entries); the
live-pulled ExcelItems 0.1.0 jar (hash-pinned `85dbb9ea…`) is
compile-classpath only.

**One player-visible text delta (named, not hidden):** the
mobile details screen for a special now shows the registry
definition's lore (the item's own lore) instead of the menu
copy — e.g. veil reads "Drink more to stack time; attacking
ends it all." where the menu copy said "Attacking ends all
accumulated time." Names, prices, slots, and purchase flow are
unchanged. The one ShopUiTest lore pin was updated to the
registry wording for exactly this reason.

## G-3 + M4 gates — automated tests (453/453, two runs)

443 baseline revivepatch tests pass (2 pins updated for the
intended changes: plugin.yml version 1.3.11 in
AdminCommandTest; registry-lore wording in ShopUiTest) + the
MobileCatalogTest revive pin re-expressed (route SPECIAL,
excelitemsId 4). **10 new tests** in `RegistrySpecialsTest`:

- **M4 gate 1a — catalog invariant at 22:**
  `catalogInvariantIsTwentyTwoWithFourRegistrySpecials` —
  22 total / 18 ordinary / 4 SPECIAL, potions_protection shelf
  order [veil_potion, deathprotection_basic, deathpotion,
  revive_potion].
- **M4 gate 1b — registry-ID guard:**
  `specialsResolveByRegistryIdWithDefinitionNameAndLore`
  (IDs 1–4, definition name/lore/prices $2,000/$15,000/$30,000/
  $5,000, owners, delivery commands; ID 2 verified ACTIVATION —
  Shops lists it for activation delivery only, mints nothing),
  `catalogOfferCollidingWithAnyRegistrySpecialFailsClosed`
  (all four ids, not just veil), `unknownRegistryIdInConfig
  FailsClosed` (id 99), `droppedBaselineListingFailsClosed`,
  `menuCommandDriftAgainstListingFailsClosed` (incl. the
  `revivepotion give` free-mint spelling), `menuPriceDrift
  AgainstConfigPriceFailsClosed`, `pluginYmlHardDependsOn
  ExcelItemsAtNewVersion`, `configAndBundledResourceListings
  Agree`.

- **M4 gate 2 — config-only addition demo: PASS.**
  `configOnlyAdditionDemo_secondCategoryListingResolves`:
  registry ID 1 listed in a SECOND category (food) by config
  only — one extra `veil_potion_food` listing in the
  special-products config + the matching menu entry, both in
  in-memory YAML; the catalog code under test is the shipped
  code (no .java change). Result: the food shelf gains the
  listing resolving to definition 1 (name "Veil Potion",
  registry lore, $2,000 config price, `mobveil buy <name>`
  delivery), total 23 products; the shipped catalog still
  loads at 22 and the shipped config.yml contains no demo
  listing (asserted in the same test — the demo is reverted
  by construction, nothing to undo in the tree).

## G-4 — co-resident load check (OWED — desk smoke instead)

Homebase mirror boot NOT run tonight, per the standing rule
(background-first, ZERO console inputs while a human uses the
PC): `tasklist` over SSH at ~8:05 PM CST shows the java.exe
client (pid 31772, ~4.4 GB — the same process M2/M3 recorded)
still running; the mirror was left completely untouched.

Desk smoke executed instead (all PASS): full catalog load from
the shipped config + menus in the JVM harness (the M4 gate-1
tests above — this is the exact constructor path
`MobileCatalog.load` runs at enable, so a catalog-invariant
error or registry-ID failure would surface here as it would
in the boot log's "Mobile shop descriptions unavailable"
warning); candidate jar listing verified (plugin.yml 1.3.11 +
`depend: [ExcelItems]`, special-products.yml present, menus
byte-identical to live, no excelitems classes bundled).

**Rig owed before filing (exact):** mirror boot with
`ExcelShops-1.3.11.jar` + live `ExcelItems-0.1.0.jar`
co-resident on `wrv-testserver` — ExcelItems enabling ×1,
ExcelShops enabling ×1 AFTER ExcelItems, SEVERE 0, no "Mobile
shop descriptions unavailable" warning, "ExcelShops purchases
available." present, RCON `excelitems status` still 4
registered / next free 5; then restore the mirror to found
state. Under D122 the supervisor decides whether the desk leg
satisfies the filing gate or the mirror leg runs first on a
free-PC window (same posture as M2/M3).

## G-6 — surfaces check (named, per scope §4.4 M4)

- **Player-facing change:** none to prices, slots, names, or
  purchase flow. One text delta: mobile details-screen lore
  for specials is now the item's own registry lore (above).
- What's New (in-game + website): **no entry owed** — the
  special shelf's presentation (menu slots/prices/names) is
  byte-identical; the details-lore alignment is an internal
  consistency fix, not a feature or behavior change. If the
  supervisor prefers an entry for the lore alignment, it is a
  one-line text placement, not a code change.
- Menus (ExcelMenu + ExcelBedrockMenu): verified — bundled
  menus byte-identical to live (Revive Bedrock slot 5); Java
  live menu files untouched by this build (Revive slot 6,
  Lost & Found slot 5 untouched).
- Tips: checked — shop tips remain true (no behavior change).
- Guide: HELD under hibernation; owed on wake: shop chunk
  refresh only if a taught shop flow changes — none does.
- Records (wrv-plugins draft PR + Drive deposit): owed at
  close-out/filing time (G2 duplicate check first), not this
  build run.

## Proof owed AFTER install (not gates, per D122)

Bedrock form rendering of the migrated shelf, in-game buy
flow per special, `/excelshopadmin reload` with a data-folder
special-products section present — post-release checks via
the board's Needs Testing.

## Artifacts (this folder; full hashes in SHA256SUMS.txt)

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelShops-1.3.11.jar` (candidate) | 277,494 B | `91b8559bf63cf2b03aff53a1d7180cffd881d505852ee3c08e0721becc735102` |
| `ExcelShops-1.3.11-repro.jar` | 277,494 B | `91b8559b…` (identical) |
| `ExcelShops-1.3.11-source.zip` (src+tests+menus+ymls, 73 entries) | — | in SHA256SUMS.txt |
| `pulls/` | G-1 live pulls (Shops jar, ExcelItems jar, live config, live spawn-catalog, extracted live plugin.yml) | in SHA256SUMS.txt |
| `RECEIPTS.md` | G-1 receipts + live-data evolution notes | — |
| `work/` | base-gate tree + build log (443/443, jar EXACT), migrated tree, build logs ×2 (453/453 each) + JUnit logs | — |

No filing, no install, no GitHub writes, no panel action, no
Homebase writes of any kind were performed by this worker (the
PC was read-only: probe + rclone pulls to a temp staging dir +
one tasklist read).
