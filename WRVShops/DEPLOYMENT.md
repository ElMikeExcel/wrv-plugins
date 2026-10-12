# ExcelShops 0.1.2 + guarded ChestShop: deployment plan

Status: **the separately approved October 12 installation is recorded in [DEPLOYMENT-2026-10-12.md](DEPLOYMENT-2026-10-12.md).** The later standalone restart removed unneeded ChestShop while retaining ExcelShops; see that same receipt for current status. The procedures below preserve the earlier reviewed paired deployment plan and its pre-installation gates, including optional compatibility paths that are not currently installed. This document itself authorizes no further live upload, permission change, merge or restart; a future deployment needs its own approval. Preserve existing sign prices, data, economy rules and protection; do not silently convert third-party shops into the native registry.

## Candidate payload and identity

`tools/package_candidate.py` creates a reproducible installation ZIP containing:

- `plugins/ExcelShops-0.1.2.jar` (original custom MIT code)
- `plugins/ChestShop-3.13-pre-1-build474-confirmation.1.jar` (full modified third-party plugin, LGPL-2.1)
- `plugins/ExcelShops/config.yml` and the matching `spawn-catalog.yml`
- `plugins/DeluxeMenus/gui_menus/spawnshop.yml`
- `THIRD-PARTY/ChestShop/NOTICE`, `COPYING.LESSER` and the complete corresponding-source ZIP
- `PLAYER-CHECKLIST.md`, this plan, `PURCHASE-PAUSE-SPEC.md`, the offline `tools/prepare_purchase_pauses.py` generator, `MANIFEST.json` and `SHA256SUMS`

The full guarded JAR retains the upstream plugin name `ChestShop`, main class, authorship, data directory and protection listeners. Its distinct filename/version identify the modification. **Never install `ChestShop-ConfirmationGuard-1.0.0.jar`: that Maven overlay has no complete plugin and is only a build API artifact.** Never install the original and guarded ChestShop JARs together. Distribute the LGPL notices and full corresponding source with the modified plugin; the source ZIP includes the pinned upstream source, modifications, an applicable patch and standalone rebuild instructions.

The three configuration files are a **new-install baseline**, not authority to reset live settings/prices. The catalog/menu retain the same 18 repository-baseline offers. Packaging validates both expected binary hashes, exact upstream binary/source provenance, complete unchanged upstream content outside the reviewed patch, matching source/legal material, aliases, namespace keys and menu/catalog equality. It rejects an original or overlay-only ChestShop JAR and never accesses a server.

## 1. Read-only live preflight, still required

A separately authorized session must establish:

1. Exact loaded JARs/versions and hashes, including duplicate/disabled WRVShops, ExcelShops or ChestShop copies. Record actual plugin load order and command ownership.
2. Complete `plugins/WRVShops/`, `plugins/ExcelShops/` and `plugins/ChestShop/` directories where present. Preserve `shops.dat`, config/catalog, hidden migration receipts and **both `checkout/` and `legacy-checkout/` logs and active-session markers**. Never delete a marker to make startup pass.
3. Current legacy signs, account names, BUY/SELL/bundle prices, item metadata, stock, single/double-chest links, permissions, claims/container trust, tax/discount and partial-trade settings. This candidate must preserve them or refuse the trade; it must not silently reset policy.
4. The current DeluxeMenus `spawnshop.yml`, menu registration, `/shop` owner, compass and villager target, and any other instant money/give entry points. Compare every live offer to the repository baseline. A difference requires a deliberately regenerated, reviewed matching menu/catalog pair before upload.
5. Exact Vault JAR/provider and EssentialsX versions, GriefPrevention, Geyser and Floodgate. Guard revision 1 supports the verified EssentialsX **2.22.0** Vault provider with ChestShop `TAX_AMOUNT=0`, `SERVER_TAX_AMOUNT=0`, `PRICE_PRECISION=2` and no configured server economy account/name/UUID. Nonzero taxes, unsupported precision/server-account settings or unsupported provider/version/guard combinations fail closed. Do not change live tax or permission settings just to bypass a block.
6. Preserve `wrvshops.use`, `wrvshops.create`, `wrvshops:shop_id` and `wrvshops:chest_identity`. No broad DeluxeMenus permission or container-trust grant is part of this deployment.
7. A consistent, recoverable backup of all plugin/config/data folders, world/chests, player inventories and economy accounts; a maintenance window; and a rollback owner. Verify process exit independently before any migration.

Record intended replacements, hashes and differences. Ask for specific live installation/restart approval after the preflight. Candidate tests are not that approval.

## Outstanding whole-server confirmation gate

This pair covers ExcelShops native shops, guarded ChestShop trades and the matching Spawn Shop menu. **It does not establish that every purchase path on the server requires confirmation.** The read-only preflight found two additional paths requiring a separate decision:

- Jobs **5.2.6.3** has its item shop enabled with four offers, including one priced at **10,000 points plus $10,000**. The [pinned official ShopManager source](https://github.com/Zrips/Jobs/blob/6fe3c272c3c970d4a94fbf7615953803eb722503/src/main/java/com/gamingmesh/jobs/config/ShopManager.java#L203-L283) gives items/runs commands before deducting points/cash, without a purchase confirmation or cancellable purchase event.
- The registered DeluxeMenus `advanced_menu` and `requirements_menu` purchase examples lack final confirmation. The observed advanced example gives a head before `eco take 666` and is gated by both `deluxemenus.admin` and `deluxemenus.shopexample`. Both registrations must be paused, not just a permission grant or command alias.

The owner approved preparing a reversible pause of Jobs itemshop and both examples for the later approved installation. **At plan preparation no live writes had occurred.** The later approved pause application is recorded in the dated deployment receipt; this plan itself authorizes no additional writes. The [precise change spec](PURCHASE-PAUSE-SPEC.md) and offline generator create new private staged copies, exact backups and hashes from fresh exports. They preserve source and all unrelated bytes, reject changed/ambiguous/linked inputs or existing outputs, and leave the actual example files intact. No exported full Jobs/DeluxeMenus config is shipped or committed because it may contain credentials. Apply only during separately approved stopped-server maintenance, then verify all three retained menu registrations remain loaded and neither example is automatically restored. This pair alone must not be described as closing every purchase route until the pause is actually applied and verified.

The same preflight observed Essentials buy/sell/trade signs disabled and no paid command costs. All 18 live Spawn Shop prices matched this pack at that observation, and ChestShop's server account was empty/zero UUID with taxes zero and precision two. These observations are not write approval: recheck them immediately before the approved maintenance and preserve the recorded policy.

## 2. Stage the exact reviewed pair

Use disposable accounts, money and world data, with the intended exact plugin combination. Bind test evidence to **both final JAR hashes**. Follow PLAYER-CHECKLIST.md with a real Java player and a Bedrock phone player.

Existing ChestShop signs must open an explicit Buy/Sell chooser, then quantity, review and separate final confirmation. Clicking either mouse button or crouching must not trade immediately. Verify Buy, Sell, free/paid offers, both halves of a double chest, preserved protection/permissions, cancellation, stale forms, rapid Confirm, stock/price/account changes, insufficient money/capacity and clean restart. Native shops and Spawn Shop keep their own confirmed flow and matching bundle economics.

Verify default-deny legacy trading with ExcelShops missing, disabled, failed or recovery-paused while ChestShop stays loaded to protect its stock. Verify unsupported taxes/providers fail before any preview money operation. A guarded transaction is serialized with native checkout; ambiguous upstream effects are journaled and pause the whole checkout service. Never automatically retry/refund an ambiguous result.

Do not hot-reload either plugin or run duplicate custom/third-party JARs. The ordinary `/wrvshop` alias remains; the old plugin-name command namespace may differ.

## 3A. Approved maintenance: no legacy WRVShops data

Only after explicit approval and the coordinated backup:

1. Stop the server normally and confirm process exit. Do not kill it during trades.
2. Remove only the specifically approved old custom shop JAR and original ChestShop JAR from the load directory. Put the **full guarded ChestShop JAR and ExcelShops 0.1.2 JAR** in place together. Preserve the entire ChestShop data directory, signs, chests, stock and settings. Never install the overlay JAR.
3. Prepare and review the offline pause files from current exports using PURCHASE-PAUSE-SPEC.md; include only the explicitly approved staged Jobs/DeluxeMenus config replacements in the maintenance action. Preserve the exact exported backups privately. Install the reviewed native config/catalog into `plugins/ExcelShops/` only for a genuinely new installation. Preserve an existing ExcelShops folder and settings; do not overwrite them with baseline defaults.
4. Replace only the reviewed DeluxeMenus `gui_menus/spawnshop.yml`, preserving registration and unrelated menus. The menu and authoritative catalog must match. An old immediate `[takemoney]`/console give menu must not remain active.
5. Start normally. Verify actual `ChestShop 3.13-pre-1 (build 474)+confirmation.1` and `ExcelShops 0.1.2` identities, the guard/provider connection, command ownership, unchanged prices, protection and no migration/recovery errors.
6. Run only the separately authorized bounded live acceptance checks. Record direct results, time, backups and both JAR hashes. Do not call an unperformed trade or phone check passed.

## 3B. Approved maintenance: legacy WRVShops data exists

The whole folder is one migration unit. A native legacy registry or active-session marker without its required journal is incomplete evidence. Recover a complete backup or reconcile offline; never copy only `shops.dat`, reset either checkout directory, or prefill a conflicting ExcelShops target with package defaults.

1. Stop the old server normally, independently verify process exit, and back up its entire data folder plus coordinated world/player/economy state. The WRVShops 0.1.0 journal's historical POSIX-lock weakness means a readable/unheld lock alone does not establish process exit.
2. Resolve any pre-existing target or incomplete migration based on its actual contents and owner approval. The plugin refuses ambiguous nonempty targets. Preserve both sides instead of deleting them to force progress.
3. Install only the approved new custom JAR plus the full guarded ChestShop JAR. The migration runs before defaults, locks required evidence, stages a complete checksummed copy, checks source stability and publishes atomically. The source folder remains untouched.
4. Carry config, catalog, registry, **both journal directories if present**, and all recovery markers/receipts together. Keep existing namespace keys and permissions. Old native `[WRV Shop]` labels remain supported; new publication uses `[Excel Shop]`. Third-party ChestShop signs remain in their original format and data system.
5. Any copied dirty marker, incomplete or uncertain transaction must still pause checkout. While stopped, reconcile against both journal logs and actual inventories/balances exactly once, retaining original evidence. Only after verified corrections, archive each affected whole journal directory and prepare its reviewed empty replacement log; never just delete `active-session` or fabricate terminal records.
6. A completed receipt permits normal new-folder updates without copying stale legacy data again. Required migrated core files must remain present. Receipts bind absolute paths; moving a server directory needs offline review. Changed source, linked files, held locks, incomplete staging, conflicting targets or checksum/copy failures require investigation, not forced retries.
7. Apply separately reviewed menu/catalog changes only after migrated state/prices are verified. Never overwrite migrated settings with baseline defaults.

## Recovery and rollback

Vault balances, player saves and Minecraft worlds are not one atomic database. Preserve `checkout/` and `legacy-checkout/` evidence and consistent account/inventory/world backups. An apparently completed trade after a dirty restart still needs review; there is no automatic replay or speculative compensation.

- **Keep guarded ChestShop installed during any rollback that leaves the server running.** Without a compatible ExcelShops adapter it denies new trades while retaining upstream protection. Alternatively leave the server stopped and disable all trading under an approved plan.
- **Never silently restore an original instant-purchase ChestShop JAR or the immediate-purchase DeluxeMenus menu as rollback.** Returning unsafe trade behavior requires a separate explicit decision; it is not the default recovery path.
- If no money/item/stock changes occurred, an approved backup restoration may replace custom configuration/code, but the guarded-trading requirement above still applies. Preserve failed-stage evidence.
- Once either shop flow changed balances, items, stock or prices, the untouched legacy folder is stale. Do not load the old custom JAR against it. Stop and reconcile a coordinated world/player/economy/shop snapshot before any reviewed rollback.
- Never run duplicate versions, convert third-party data silently, delete recovery markers, or alter claim trust to bypass a blocked shop.

## Evidence and remaining acceptance gates

The current native summary is [11 boots / 852 assertions](verification/runtime-harness/evidence-excel-0.1.2/summary.json); the paired real-plugin suite passed 10 boots / 6,136 assertions with zero failures, recorded by `verification/legacy-runtime-harness/`. Combined acceptance totals 21 boots / 6,988 assertions and does not include the separate unsafe immediate-trade reproduction. Both suites bind the final JAR hashes listed in README. historical 0.1.1 synthetic-harness evidence under `verification/runtime-harness/` is unchanged and does not validate this new pair. The isolated real-ChestShop harness uses actual EssentialsX **2.22.0** and GriefPrevention, with official **Vault 1.7.3-b131 as a substitute**. At the time of the isolated tests the live Vault 2.0.0 artifact was unidentified. The later deployment receipt records its Vault-Updated identity and successful startup provider connection; it does not retroactively make the isolated substitute the same binary or prove a live transaction. Essentials' unsupported-server warning is not removed by this candidate.

Human Java and Bedrock phone controls, real player save behavior, Geyser/Floodgate, the exact live Vault integration and unrelated live plugins remain explicit pending gates until directly checked. Builds, installation packs and disposable harnesses make no live change.
