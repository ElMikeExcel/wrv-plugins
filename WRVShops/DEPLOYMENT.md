# ExcelShops 0.1.1: exact deployment plan

Status: **prepared installation candidate; no live upload, permissions change, merge or restart authorized by this document**. This plan changes only the custom shop plugin and its paired Spawn Shop files. ChestShop and its signs, stock, permissions, taxes and economy rules stay untouched.

## Candidate payload

The reproducible packaging script `tools/package_candidate.py` builds a ZIP with:

- `plugins/ExcelShops-0.1.1.jar`
- `plugins/ExcelShops/config.yml`
- `plugins/ExcelShops/spawn-catalog.yml`
- `plugins/DeluxeMenus/gui_menus/spawnshop.yml`
- `PLAYER-CHECKLIST.md`, this plan, `MANIFEST.json` and `SHA256SUMS`

The three configuration files are a **new-install baseline**, not permission to overwrite existing settings or prices. The script checks descriptor identity, unchanged storage namespace, retained command alias, catalog/resource equality, absence of immediate money/give commands and per-file hashes. It never accesses a server. Follow the legacy-data branch below if WRVShops data exists.

## 1. Read-only live preflight, still required

A separate authorized server session must capture these facts without changing them:

1. List plugin JARs and versions. Check whether any `WRVShops` or `ExcelShops` JAR or data folder exists, including disabled/duplicate copies. Do not assume “never installed” means no data.
2. Inspect the complete old/new data directories if present, including hidden migration markers and `checkout/active-session`, `checkout/transactions.log`, `shops.dat`, `config.yml` and `spawn-catalog.yml`. Record hashes and preserve backups. Do not delete a marker to make startup pass.
3. Read the live DeluxeMenus `gui_menus/spawnshop.yml`, its menu registration, the `/shop` command owner, the compass/menu Spawn Shop action and ShopVillager's configured target. Confirm exact player entry points and detect any other immediate purchase commands.
4. Compare every live item, metadata, bundle size and price against the candidate. The included 18-offer catalog is the repository baseline. Any live price/catalog difference requires a deliberately regenerated, reviewed matching menu/catalog pair before upload; do not silently reset the economy to defaults.
5. Verify enabled Vault provider and its exact version, loaded GriefPrevention, ChestShop, Geyser/Floodgate and Essentials versions. Preserve any unsupported-version warnings. Actual provider/claim integration remains an acceptance gate.
6. Read normal-player permissions and claim settings, including container trust. Keep `wrvshops.use`, `wrvshops.create`, `wrvshops:shop_id` and `wrvshops:chest_identity` unchanged. No broad DeluxeMenus or trust grants are part of this deployment.
7. Confirm a recoverable, consistent backup covering plugin/config files, both possible shop data folders, world/chests, player inventories and economy data, plus a maintenance window and rollback decision owner.

Record the observed hashes, intended replacements and differences in a deployment record. Ask for explicit live installation/restart approval after this preflight; a candidate or test result is not that approval.

## 2. Test the exact intended package on staging

Use the intended exact live plugin combination with disposable accounts/money and a separate world. Verify both Java and Bedrock phone controls using PLAYER-CHECKLIST.md. Test `/shop`, compass, villager, `/excelshop` and old `/wrvshop` alias. A real purchase should use one common Vault balance and move exact physical stock only after final confirmation.

Do not hot-reload this plugin, accept a dirty recovery state as success, or run old/new shop plugin JARs together. A namespaced command may change from the old plugin-name namespace to `excelshops:*`; the ordinary `/wrvshop` compatibility alias is retained.

## 3A. Approved maintenance: no legacy WRVShops data exists

After explicit approval and the consistent backup:

1. Stop the server normally and verify process exit. Do not kill it during purchases.
2. Place only `ExcelShops-0.1.1.jar`; remove an old custom shop JAR from the load directory only if it was identified and included in the approved replacement list. Do not remove ChestShop.
3. Install reviewed config/catalog into `plugins/ExcelShops/`. If ExcelShops data already exists, preserve it and its settings; do not overwrite with package defaults.
4. Replace only the reviewed `plugins/DeluxeMenus/gui_menus/spawnshop.yml`. Preserve the existing menu registration and unrelated menus. Both the menu and authoritative catalog must match the reviewed package.
5. Start normally. Verify `ExcelShops 0.1.1`, provider hook, no migration/recovery error, correct command ownership, prices and storage protection. The old immediate `[takemoney]` / console give menu must no longer be active.
6. Perform separately authorized, bounded acceptance checks. Confirm live state with direct evidence, then record hashes, backups, time and observed results. Do not call unperformed purchases or phone checks passed.

## 3B. Approved maintenance: legacy WRVShops data exists

The whole folder is one migration unit. A legacy registry or active-session marker without its checkout journal is incomplete evidence and refuses migration before staging; recover the complete backup or reconcile offline first. Never copy just `shops.dat`, never reset the checkout directory, and never pre-fill a conflicting ExcelShops directory with package defaults.

1. Stop the server normally and verify the old server process has exited, then back up the **entire** legacy folder plus world/player/economy state.
2. Resolve any pre-existing ExcelShops directory or incomplete migration with its actual contents and owner approval. The plugin refuses ambiguous nonempty targets, even if someone calls them defaults. Preserve both sides; do not “fix” it by deletion.
The WRVShops 0.1.0 journal has a historical POSIX-lock weakness: a readable/open legacy journal lock alone does **not** prove its process stopped. A copied dirty-session marker still pauses ExcelShops, but migration is not a live synchronization tool. Verify process exit independently.

3. Load only the new ExcelShops JAR. Do not leave a WRVShops plugin JAR loaded or disabled in the same load set. On startup, before defaults, the migration takes a process lock and any existing legacy journal lock, stages a complete checksummed copy, checks source stability and publishes the new folder atomically. The old folder remains byte-for-byte untouched.
4. The migration copies config, catalog, registry, journal and recovery markers together. `wrvshops` PDC keys and permissions remain the same, so existing managed chest identities stay valid. Old `[WRV Shop]` signs continue to work; editing/publishing a shop uses `[Excel Shop]` thereafter.
5. A copied `active-session`, incomplete transaction or uncertain journal **must still pause checkout**. Reconcile while stopped using the original evidence and the documented recovery process; never remove only the marker or automatically replay/refund transactions. After completed reconciliation, preserve the archived checkout directory and prepare the reviewed empty replacement `checkout/transactions.log` offline before restarting; migrated core files cannot simply disappear.
6. A successful receipt allows subsequent ExcelShops writes without recopying stale legacy data. Migrated core config/catalog/registry/journal files must remain present; ordinary updated bytes are permitted. Receipts bind absolute source/destination paths, so relocating the server directory requires an offline migration review. A changed legacy source, incomplete stage, conflicting target, symbolic/hard link, held journal lock or failed copy/checksum causes protection-only mode. Keep the originals and investigate; no overwrite/retry is an accepted migration strategy.
7. Only after the migrated state and prices are verified should any separately reviewed catalog/menu adjustment be applied during the coordinated maintenance plan. Do not overwrite migrated settings with the ZIP's baseline config/catalog.

## Rollback

- If no new purchases or stock/price edits occurred, stop normally and follow the approved backup rollback with the prior JAR/menu/config and consistent data snapshot. Preserve failed-stage evidence.
- Once ExcelShops has changed money, player items or chest stock, the untouched legacy folder is stale. **Do not revert by simply loading the old JAR against that old folder.** Stop and reconcile a coordinated world/player/economy/shop state, then make a reviewed rollback decision.
- Never run both custom versions simultaneously, silently migrate third-party ChestShop shops, delete a dirty checkout marker, or change claim trust to get around a blocked shop.

## Verification record

The latest source/CI hash, candidate hashes, unit counts and exact-runtime evidence are linked from README.md and `verification/runtime-harness/`. Fake economy/claims and server-side player fixtures test code paths; they do not replace the read-only live preflight or human Java/Bedrock acceptance. No live changes are performed by the build, package script or isolated harness.
