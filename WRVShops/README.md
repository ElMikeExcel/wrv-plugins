# ExcelShops 1.1: mobile details and quantity candidate

**Prepared for a controlled rollout; not installed yet.** The last verified live release is version-only 1.0. This candidate starts from that exact source and preserves the earlier 0.1.6 history below. Its descriptor, Maven metadata, and packaging use the quoted two-part version `1.1`. This is separate from the behavior-identical version-only 1.0 release and requires its own installation/publication approval.

For positively identified Bedrock players, the five native category lists lead to item-first labels, then an item-specific description and quantity hub. Ordinary items have Enter exact amount, Review this quantity, Back to the same category, and Cancel. Exact input repeats the selected item details and returns to the hub; invalid typed numbers get a fresh bound input prompt. Native close or an invalid form response cancels. Existing separate review and final Confirm remain the only purchase path. Fixed-quantity special products show full native details before their unchanged guarded review commands.

Java and chest-shop flows retain their inventory behavior. All six installed-menu files are bundled byte-for-byte as read-only display resources; they are never written over DeluxeMenus or the live catalog. The public organized 0.1.6 history remains under [organized-0.1.6](../SpawnShop-DeluxeMenus/organized-0.1.6/README.md). Preserve the current live catalog, configuration, player data, and all 21 offers.

Read [MOBILE-DETAILS-CANDIDATE.md](MOBILE-DETAILS-CANDIDATE.md) for navigation, verified integration boundaries and safe rollback. The final JAR passes 760 Java tests, 47 Python checks, an independent byte-identical rebuild, 1,299 assertions over four pinned-Paper boots, and 50 additional checks with official DeluxeMenus/Geyser/Floodgate/PlaceholderAPI binaries. The latter exercise public API/Cumulus builders and actual DeluxeMenus holder registration/cleanup with synthetic players. Geyser encryption initialization was limited by isolated JVM DNS failures to Minecraft discovery/auth services; actual Bedrock form delivery, touch behavior and live transactions remain unverified. No physical-client success is claimed.

- Candidate 1.1 SHA-256: `f28c5c4ba5d231219735228d1d556b39de57cc8a965d5fb62eddb31dbceab9e4`.
- Last verified installed 1.0 SHA-256: `6acede0322655ad03818d781ff6676fee3c2e79a3659ed4c54db46608f361106`.
- Deployment scope is one JAR only. Do not replace current menus, catalog, configuration or player/economy data from the historical paired package; ChestShop remains removed.

## Preserved 1.0 deployment and earlier history

# ExcelShops 1.0: installed version-only milestone

**Installed and version-confirmed October 14, 2026, at 06:03:51 UTC (1:03 a.m. Central).** This source PR remains draft and unmerged. The final quoted-version source passes 769 Java tests and 80 Python checks. Its exact artifact passes 1,281 assertions across four isolated Paper 26.2 build 123 boots; all owned processes exited cleanly. A separate clean-source rebuild produced the identical JAR. All 76 production classes are byte-identical to installed 0.1.6. These are automated/synthetic-player checks, not a new physical-client acceptance claim. This candidate changes only the version metadata, packaging references, and documentation from the exact verified installed ExcelShops 0.1.6 source (JAR SHA-256 `a5191eef70a4fdbd1326c14c36c9fd36afa25fe712762cc1850943a1a75c6d4e`). It adopts the user-selected two-part custom version `1.0`. All production Java source, catalog/configuration resources, and behavior remain unchanged. The mobile interaction redesign is separate and is not included. The existing six DeluxeMenus launchers must remain unchanged. Plugin and Maven metadata both identify this candidate as `1.0`; parent and other plugin/upstream versions are untouched.

The installed 0.1.6 history is retained in [the exact installation record](../SpawnShop-DeluxeMenus/organized-0.1.6/INSTALLATION.md) and `verification/navigation-0.1.6`. Those records remain the exact historical 0.1.6 deployment. Current 1.0 verification is supplied in the separate version-only bundle. The older deployment and balance records below are retained as historical source context. Preserve the existing live catalog and data. The source baseline catalog is not a deployment replacement for the current server catalog.

## October 14 version-only deployment

- Active artifact: `plugins/ExcelShops-1.0.jar`, SHA-256 `6acede0322655ad03818d781ff6676fee3c2e79a3659ed4c54db46608f361106`.
- The staged and active hashes were verified; console reports ExcelShops 1.0 after startup. The matching source CI artifact has the identical JAR hash.
- Previous artifact is preserved as `plugins/ExcelShops-0.1.6.jar.pre-1.0-20261014.disabled`, SHA-256 `a5191eef70a4fdbd1326c14c36c9fd36afa25fe712762cc1850943a1a75c6d4e`.
- This deployment changed only the version-only JAR. The six organized DeluxeMenus files, registration, live catalog/configuration, and player/economy data were not replaced.
- The native mobile description/quantity/Back improvement is separate 1.1 development work and is **not installed** by this release. The owner reports that the category layout looks good on a phone but hold-to-read remains clunky. No new physical-client test is claimed or requested.

## Historical 0.1.4/0.1.5 documentation


**New 0.1.5 balance display candidate is prepared off-server, not installed.** See [balance instructions, examples and device limits](BALANCE-DISPLAY-0.1.5.md). It adds visible balance headers and current balance / purchase cost / projected remainder, with insufficient-funds warnings, while retaining typed entry and separate confirmation. Live status below remains 0.1.4.

**0.1.4 is installed; real-device acceptance remains pending.** It adds a clear **Enter Amount** button and private native Java text field for exact prices and purchase quantities. Bedrock keeps native numeric forms. See [typed-entry instructions and verification](AMOUNT-ENTRY-0.1.4.md). The host hash, loaded version and startup checks were verified at about 8:13 p.m. Central on October 12, 2026 (October 13 UTC). Java keyboard/mouse and Bedrock touch, controller and Windows still need human checks.

**Installed standalone on Wink Reyes Valley on October 12, 2026.** ExcelShops 0.1.4 runs native player shops and the matching Spawn Shop without ChestShop. The guarded ChestShop plugin was installed earlier that afternoon, then removed after the owner confirmed it was unnecessary. Standalone startup, the EssentialsX economy connection and scoped purchase-menu pauses were checked. An earlier 0.1.2 user-reported Java PC check passed Spawn Shop access/prices/cancel/torch purchase and owner chest-shop setup/publication. A second-player chest-shop purchase and broader platform/balance/stock checks remain pending. The installed 0.1.4 retains the 0.1.3 fix for inventory recreation during price/quantity adjustments. Startup is verified; a human Java cursor retest is still pending. See the [live deployment record](DEPLOYMENT-2026-10-12.md). ExcelShops is original MIT-licensed code. Native ExcelShops and Spawn Shop do not require ChestShop. When legacy ChestShop compatibility is used, it requires the separately branded, LGPL-2.1 **ChestShop 3.13-pre-1 build 474 + confirmation.1** artifact in the same installation. That full modified third-party JAR retains the upstream plugin name `ChestShop`, authorship, data directory and protection listeners. Its source/notice bundle must accompany redistribution. The Maven overlay `ChestShop-ConfirmationGuard-1.0.0.jar` is a compile artifact, not a deployable plugin.

Native Excel shops and Spawn Shop require separate review and final confirmation. The optional guarded ChestShop integration described below adds that flow to legacy BUY/SELL trades when installed; that compatibility plugin is no longer active on Wink Reyes Valley. The optional bridge preserves legacy signs, prices, accounts, permissions, stock, protections and double chests rather than converting them. Standalone ExcelShops does not activate or replace third-party ChestShop sign trading. Nothing is deployed by this repository or its tests.

Plugin name and JAR: **ExcelShops**. Source folder/package remain `WRVShops` / `com.winkreyes.shops` for compatibility. `/excelshop` is the branded command; `/wrvshop` remains a working alias. Existing `wrvshops.use` / `wrvshops.create` permissions and the `wrvshops:shop_id` / `wrvshops:chest_identity` storage keys are intentionally unchanged.

See the [buyer/seller checklist](PLAYER-CHECKLIST.md), [live deployment record](DEPLOYMENT-2026-10-12.md) and preserved [deployment plan](DEPLOYMENT.md).

## Players: buy from a chest

1. Open a chest labeled `[Excel Shop]` (older `[WRV Shop]` labels are still recognized), or interact with its sign.
2. The read-only chest screen shows the listed items, unit prices and current stock. You cannot drag free stock into your inventory.
3. Choose an item, then a quantity. The next screen shows quantity, price and total, including enchantments/custom lore where relevant.
4. Review the purchase, then choose **Confirm purchase** on the separate “Are you sure?” screen.
5. Only the final confirmation can move money or items. Back, Cancel, closing a screen and disconnecting do not buy anything.

Stay within six blocks of the shop. Quotes expire after 60 seconds by default. If price, ownership, item metadata or chest stock changed, review a fresh quote. Full inventories, insufficient funds, unavailable accounts and unavailable protection checks fail without a purchase. A partially filled matching stack counts as available inventory room.

### Java and Bedrock controls

- Both platforms browse the physical chest's contents through a read-only inventory menu. Left and right clicks do the **same** action; no Java-only left/right buy/sell convention.
- Java: choose **Enter Amount**, type a whole-number quantity such as `19`, then **Use amount**. Review and the separate final confirmation still follow. Clearly labeled quantity buttons (`−64`, `−8`, `−1`, `+1`, `+8`, `+64`) remain available for small adjustments.
- Bedrock uses native Geyser number-entry forms for quantity and unit price, followed by separate review and final confirmation forms. Enter a whole-number quantity or a positive price with at most two decimals. If forms are unavailable, the same inventory buttons are used.
- Java prices: choose **Enter Amount**, replace the text with `0.50` (for fifty cents per item), then **Use amount**. This saves only the draft. Review and **Confirm and publish shop** are still required. Price buttons remain for small changes. No command or public chat is needed.
- Platform routing checks both Geyser and Floodgate, independently of the forms setting. Bedrock and uncertain clients never receive the Java-only fallback dialog. If their native form is unavailable, shared buttons remain usable. See the [Java/touch/controller acceptance matrix](AMOUNT-ENTRY-0.1.4.md#cross-platform-acceptance-matrix).
- Native Java text dialogs require Paper 1.21.7+ and a compatible Java client; the installed Paper 26.2 supports the API. Unsupported/failed dialog APIs return to the amount buttons. Older clients behind protocol translation are not detected reliably. Real Java and Bedrock phone rendering/input with the live Geyser/Floodgate builds remains a required player check; automated tests do not prove it.
- **Cancel** in the Java text dialog returns to the previous amount without saving the typed text. Use the Cancel button in the Java dialog; Escape is disabled to make cancellation explicit. Java text entry expires after 60 seconds. Invalid input can be corrected in a fresh field. Closing or abandoning purchase review never buys anything.

## Optional ChestShop compatibility (not currently installed on Wink Reyes Valley)

1. Tap/click an existing ChestShop sign. Left/right click and sneaking no longer immediately trade. The chooser identifies the item and offers **Buy from shop** and/or **Sell to shop**, according to the existing sign.
2. Choose a direction, then a whole **item quantity**. Legacy quantity is items, while Spawn Shop quantity remains bundles. Java has quantity buttons; Bedrock has numeric entry with an inventory fallback.
3. Review the effective item quantity, custom item name/metadata, counterparty, original sign bundle price, effective per-item price and exact total. A fractional per-item display is labeled approximate; the exact total is authoritative. Existing partial-transaction/discount rules can change the available quantity or total, so inspect the returned quote.
4. Continue to the separate final **Confirm Buy** or **Confirm Sell** screen. Only that action can invoke the bound legacy trade. Cancel, Back, a closed view or disconnect before confirmation moves no money or items.
5. The final action revalidates sign, owner, inventory, permissions, protection, price, provider and quote state. A change requires a new review. A free legacy offer is shown as zero; native ExcelShops listing prices still must be positive.

The bridge leaves ChestShop's own single/double-chest, finite/admin-stock, account and protection rules intact. It does not rewrite old signs, alter prices or convert shops into the native registry. Owners retain upstream management access. The guard stays **default-deny** if ExcelShops is absent, disabled, incompatible or recovery-paused; ChestShop remains loaded to protect its existing shops. An independently dispatched or uncancelled raw transaction cannot bypass its one-shot guard.

This initial guard supports the verified **EssentialsX 2.22.0 Vault economy provider** with ChestShop `TAX_AMOUNT=0`, `SERVER_TAX_AMOUNT=0`, `PRICE_PRECISION=2` and no configured server economy account/name/UUID. Item counts are 1–2304, exact-cent totals are at most $100,000,000, and the player must remain online, in the same world and within six blocks. Unsupported precision/server-account settings, nonzero ChestShop taxes, unsupported economy providers/versions, or an unsupported ChestShop guard version fail closed before preview can invoke money operations. Do not change live taxes or permissions to make a blocked shop pass without an explicit review. These are deliberate restrictions, not a migration of the server's economy policy.

## In-place inventory buttons (introduced in 0.1.3, retained in 0.1.4)

The installed update changes Java/fallback price and quantity slots in the same inventory instead of closing and reopening it after each plus/minus click. Queued clicks read the current value, so rapid adjustments accumulate correctly and Review/Use reads the latest amount. Existing price/quantity bounds, session invalidation, review and separate final confirmation remain in place. Native Bedrock number forms are unchanged. The setup success message now correctly mentions both chest and attached sign.

This addresses the server-side window-recreation cause; the approved installation and startup checks have completed. A human Java cursor check remains pending, so this is not yet a physical mouse acceptance pass. Ordinary Java chat entry still clears inventory context and is not a workaround. See [the update verification and retest checklist](CURSOR-FIX-0.1.3.md).

## Owners: make a shop

1. Use **one single normal or trapped chest** in a GriefPrevention claim you actually own. Both chest and sign must be inside your claims. Build/container trust is not ownership. Do not use another player's recorded chest.
2. Fill it with the items you want to sell.
3. Crouch/sneak while placing a wall sign against the chest, or a standing sign on top. Finish the normal sign editor. No special sign text or price syntax is needed.
4. The prompt asks **Make this a shop?** Choose Yes to start, or No to keep ordinary storage and its label.
5. Select each distinct item in the chest and choose its **price for one item**. The setup screen shows the actual stock count. All matching physical stock is offered at that unit price; items are never copied into a separate virtual stock store.
6. Review your owner name, each item, price and stock count, then separately **Confirm and publish shop**. Until then, the chest is ordinary storage.
7. The sign is labeled with the shop and owner. Other players can now browse and buy through confirmation.

To resume after canceling, look at your ordinary sign and run `/excelshop setup`. If the chest is empty, add stock first, then resume setup. Existing stock/sign/claim changes during setup require a fresh review. Setup and price edits never move money or stock.

**Native Excel shops use single chests only.** Double chests are refused with guidance; existing double-chest ChestShop shops retain their storage and protection and use the guarded legacy confirmation flow. Another sign next to the chest or a ChestShop-formatted sign is refused to avoid taking over an existing shop. Use a separate chest for the new flow.

### Restock, change prices or close

Open your own shop chest/sign for its owner menu, or look at it and use `/excelshop manage`.

- **Open stock chest:** move your own items in and out normally. Current matching stock is what buyers can purchase. New item types need a price through Edit item prices before they appear for sale.
- **Edit item prices:** choose prices, review and confirm. Sold-out existing listings remain available to restock.
- **Close shop:** separately confirm returning it to ordinary storage. All items stay put. Its shop label is cleared, and then you can break the sign/chest.

Active shop chests/signs cannot be broken, exploded, piston-moved, extended into double chests, automated with hoppers, or opened directly by buyers. Owner access is revalidated. Do not grant public container trust as a workaround.

## Spawn Shop

The companion DeluxeMenus menu becomes a **display-only launcher**. For normal shopping, **open the compass menu → Spawn Shop**, or tap the existing Spawn Shop villager. Use the item/quantity/review/confirmation controls; no chat command is needed. `/shop` and `/excelshop` remain optional fallback entry points.

Spawn stock remains unlimited. Prices retain the original **bundle** economics: for example 16 bread costs 40; two bundles deliver 32 bread for 80. The UI explicitly labels bundle size, bundle price and final item count. This avoids silently changing unit prices that do not divide into whole cents. Enchanted books are built with the server item API, never unverified console `give` syntax.

The player command selects only a server-side offer ID. It cannot supply a price, item or recipient. Spawn purchases use the same final-confirm checkout and inventory capacity checks as chest purchases.

## Remaining server-wide purchase paths

The read-only preflight also found an enabled Jobs 5.2.6.3 item shop (four offers, one costing 10,000 points plus $10,000) and the purchase-bearing DeluxeMenus `advanced_menu` and `requirements_menu` examples (the advanced example gives a head, then runs `eco take 666`). They remain outside this pair's confirmation coverage. The owner approved a reversible pause; the separately approved October 12 deployment applied it, as recorded in [the live receipt](DEPLOYMENT-2026-10-12.md). The included [offline generator and change spec](PURCHASE-PAUSE-SPEC.md) stage exact, minimal replacements from fresh private exports without changing permissions or example files. The installed pauses cover these enumerated paths, not an unlimited claim about every purchase route; see [the deployment scope gate](DEPLOYMENT.md#outstanding-whole-server-confirmation-gate). The pack ships only the pause script/spec, never complete exported Jobs/DeluxeMenus configurations or credentials.

The observed 18 live Spawn Shop prices matched this baseline; still recheck immediately before any approved write. Essentials buy/sell/trade signs were disabled and no paid command costs were found. These are read-only observations, not deployment approval.

## Administrator setup and migration

**Current native/Spawn-only setup:** ExcelShops runs without ChestShop. The paired/legacy steps below apply only when guarded third-party sign compatibility is separately required. Do not reinstall that plugin merely to follow an old paired plan. Keep the matched Spawn menu/catalog, native data and journals, provider and claim checks.

A possible `plugins/WRVShops/` folder is **not discarded**. Before creating defaults, ExcelShops safely copies the entire legacy folder to `plugins/ExcelShops/`, preserving the original folder untouched. Registry, config, catalog, both `checkout/` and `legacy-checkout/` histories and their `active-session` recovery markers travel together. Preserve both journals even when only one flow appears affected. A dirty legacy journal remains dirty and pauses checkout. A conflicting target, incomplete migration, registry/recovery marker without its journal, old plugin still loaded, held journal lock, linked file or changed source fails closed. The old WRVShops 0.1.0 POSIX lock is not reliable proof of process exit: migration requires an independently verified fully stopped old server. A copied dirty marker still blocks checkout. A successful migration receipt permits ordinary new-folder progress on subsequent starts while refusing a subsequently changed legacy source. Details and rollback restrictions are in [DEPLOYMENT.md](DEPLOYMENT.md).


1. **Do not hot-install this on production.** Back up plugin files, world/chest data, player inventories and economy data together. Use a disposable copy with the intended exact plugin versions first.
2. Build with `./build.sh` at the repository root or `./WRVShops/build.sh`; Java 21+ and Maven 3.9+ are required. The build bootstraps the official hash-pinned ChestShop binary/source, compiles the provided API overlay first, and packages the full guarded binary plus corresponding source. Outputs include `WRVShops/target/ExcelShops-1.0.jar` and `ChestShop-ConfirmationGuard/target/ChestShop-3.13-pre-1-build474-confirmation.1.jar`.
3. Stage both reviewed JARs with Vault and the supported EssentialsX provider. Replace the original ChestShop JAR with the full guarded JAR; never load both or install the overlay-only build artifact. Retain the entire ChestShop data folder. Defaults require working GriefPrevention ownership checks. Geyser is optional for native Bedrock forms.
4. Complete the read-only live preflight in DEPLOYMENT.md first. The bundled catalog contains repository baseline prices, not newly verified live prices. If a legacy WRVShops folder exists, follow the migration branch of that plan rather than pre-populating a conflicting target. Then install the matching generated `SpawnShop-DeluxeMenus/spawnshop.yml` into DeluxeMenus' `gui_menus/` and `SpawnShop-DeluxeMenus/spawn-catalog.yml` into `plugins/ExcelShops/`. Keep DeluxeMenus' existing menu registration and ShopVillager target.
5. **Replace the old immediate-purchase menu as part of the same maintenance change.** Merely adding the JAR does not remove `[takemoney]`/`minecraft:give` from an old live DeluxeMenus file. Merely replacing the menu without the plugin fails closed but cannot open checkout. Compare both deployed files to the reviewed candidate.
6. Start the isolated server and verify the provider name, no recovery lock, correct prices, normal-player permissions, claim ownership, Java/Bedrock controls and real purchase/refund behavior with disposable money/items. Exact live economy/claims/Geyser combination must still be tested.
7. Only after review and explicit deployment approval, repeat the controlled installation on the live server. Never silently convert legacy ChestShop signs or migrate their stock/prices.

Permissions: `wrvshops.use` and `wrvshops.create` default to true. `wrvshops.use` also gates the legacy confirmation UI; existing ChestShop permissions and protection are still independently rechecked. No LuckPerms permission writes are performed. Claim ownership and recorded placer checks still apply. No operator bypass steals or adopts another player's shop. Set these permissions explicitly in the existing permission manager if access should be restricted.

Config: `quote-seconds` is 1–300 (default 60); `bedrock-forms` defaults true; `require-grief-prevention` defaults true. Setting the latter false is for isolated testing or a separately reviewed protection setup; it permits recorded-placer wilderness shops and removes the default owned-claim safeguard. No live reload command is provided: restart normally so quotes are invalidated and persistence is checked.

Retune Spawn Shop bundles with:

```sh
python3 SpawnShop-DeluxeMenus/make_spawnshop.py 1.25 \
  --output /staging/spawnshop.yml \
  --catalog-output /staging/spawn-catalog.yml
```

Review and deploy both outputs together, then restart the staged plugin/server. The original friendly rounding remains; 1.25 is not exactly 25% for every final rounded price. No client-provided price is trusted.

## Failures, crashes and reconciliation

Vault does not provide an atomic transaction across two balances, player data and Minecraft world saves. This candidate explicitly does **not** claim crash-proof atomic purchases.

- A forced, checksummed journal records evidence and intent before each money/item operation. It includes transaction ID, buyer/seller, offer/revision, exact quoted total, item metadata, inventory before/after snapshots and starting balances.
- Definite failures before mutation charge nothing. A definite failed seller payout restores both inventories and attempts one buyer refund. A failed refund, thrown/ambiguous provider call, uncertain inventory mutation or journal failure pauses all new checkouts and shop-management actions for administrator review. It never guesses and blindly repeats a charge or refund.
- An unclean process stop keeps an active-session marker. **Even apparently completed purchases require review after an unclean restart**, because Vault, player and world durability are independent. A normal shutdown saves touched player/world state before clearing the clean-session marker. No pending quote survives a restart/reload.
- Startup failures leave the plugin in protection-only mode when its code can still load. Both chest and sign carry persistent shop tags, so a damaged/missing registry does not intentionally expose raw stock. If ExcelShops itself is removed or cannot load at all, GriefPrevention's own rules apply: container-trusted players may still access claim chests. Keep the plugin present and avoid public container trust; don't treat plugin-absent stock as universally theft-proof.
- A claim transfer or corrupt/missing identity blocks trades/owner edits. Preserve the stock and ask an administrator to reconcile it; there is no automatic ownership reassignment.

For recovery: stop the server, preserve the entire `plugins/ExcelShops/` folder plus contemporaneous economy/player/world backups, and inspect both `checkout/transactions.log` and `legacy-checkout/transactions.log` with the corresponding saved inventories/balances. A legacy `LEGACY_INTENT` must end in `LEGACY_COMMITTED`, `LEGACY_NO_CHANGE`, or an explicitly unresolved `RECOVERY_REQUIRED`; an unfinished or uncertain record locks all trading. Legacy completion does not promise atomic economy/world persistence. Reconcile every uncertain transaction exactly once with the owner, documenting the correction. Keep the original evidence. Only after verified corrections, archive each complete reconciled journal directory, then prepare reviewed empty replacements for the affected `checkout/transactions.log` and/or `legacy-checkout/transactions.log` while the server is stopped before starting a fresh session. This preserves the migrated-core-file requirement; keep the archived journal and recovery markers as evidence. Do not delete only `active-session`, fabricate terminal journal records or retry purchases as a recovery technique. Recovery edits/payments are administrator actions, never automatic plugin behavior.

Journal size is capped at 256 MiB; over-limit state pauses checkout rather than dropping evidence. Archive only offline after verifying terminal records and a clean shutdown. Storage caps and single-chest scope suit this small server; high-volume use needs a separately designed persistent economy/storage system.

## Verification and remaining gates

The current paired-candidate checks are recorded by CI and the [real-ChestShop runtime harness](verification/legacy-runtime-harness/README.md). Its disposable server uses actual **EssentialsX 2.22.0**, ChestShop and GriefPrevention, plus real Paper inventory implementations. **Vault 1.7.3-b131 was a compatibility substitute in the isolated harness.** The later live deployment identified Vault-Updated 2.0.0 and verified the guarded connection to EssentialsXEconomy; this does not establish a player transaction pass. No test establishes byte identity of the whole live plugin stack. Human Java and Bedrock phone input/rendering, Geyser/Floodgate, real player persistence and unrelated live-plugin interactions remain explicit acceptance gates.

Historical 0.1.2 native-shop acceptance on its final JAR passed **11 boots / 852 assertions**, including 614 candidate assertions and 238 genuine old-plugin baseline assertions; see the [0.1.2 native summary](verification/runtime-harness/evidence-excel-0.1.2/summary.json). The final paired real-plugin suite passed **10 boots / 6,136 assertions with zero failures** using actual EssentialsX/GriefPrevention/guarded ChestShop and synthetic players; see its [runtime harness and evidence](verification/legacy-runtime-harness/README.md). Together these are 21 boots / 6,988 assertions; the separate 27-assertion immediate-trade reproduction is not counted as acceptance. Both suites bind the final ExcelShops SHA `009f321a99f30024bcaf6742e9b2dc9dfba23571eaddddad1eee9699d6fe9edc`; paired evidence also binds guarded ChestShop SHA `5e7bbf6014484ff87b5decced2df32e36b05b3aaa6c26ba03d57c72314cdd814`.

Historical 0.1.1 evidence is preserved unchanged under [the earlier native-shop harness](verification/runtime-harness/README.md): 330 Java tests, 23 Python checks, and 11 complete boots with 852 assertions using synthetic economy/claims/player fixtures. Its accepted JAR hash and results apply only to that earlier artifact. The later real-ChestShop baseline intentionally reproduced immediate legacy trading; that baseline is evidence of the original gap, not release acceptance. Do not apply an old test count/hash to the new pair.

For the installed 0.1.3 cursor update, use the new artifact-specific results in [CURSOR-FIX-0.1.3.md](CURSOR-FIX-0.1.3.md); historical runtime counts and hashes above do not certify this new JAR.

For the installed 0.1.4 typed-entry update, see [its verification and compatibility limits](AMOUNT-ENTRY-0.1.4.md); earlier results do not certify it.

CI uploads the nine custom JARs, the distinctly named full guarded ChestShop JAR with complete LGPL corresponding source, and a reproducible `ExcelShops-1.0-install-candidate.zip`. The installation pack contains exactly the two deployable shop JARs, unchanged matching 18-offer menu/catalog, legal/source material, the offline pause generator/spec, hashes and these instructions. Packaging verifies the official upstream hash, preserves every unmodified upstream entry, checks guard/source correspondence, and rejects an original or overlay-only JAR. It performs no server access.

After bootstrap/build, reproduce the pair from independently reviewed artifact hashes:

```sh
python3 WRVShops/tools/package_candidate.py \
  --jar WRVShops/target/ExcelShops-1.0.jar \
  --expected-sha256 VERIFIED_EXCELSHOPS_SHA256 \
  --guarded-jar ChestShop-ConfirmationGuard/target/ChestShop-3.13-pre-1-build474-confirmation.1.jar \
  --expected-guarded-sha256 VERIFIED_GUARDED_CHESTSHOP_SHA256 \
  --guard-sources ChestShop-ConfirmationGuard/target/ChestShop-3.13-pre-1-build474-confirmation.1-sources.zip \
  --expected-sources-sha256 VERIFIED_CORRESPONDING_SOURCE_SHA256 \
  --source-commit FULL_SOURCE_COMMIT_SHA \
  --output /staging/ExcelShops-1.0-install-candidate.zip
```

The output must not exist; packaging never overwrites a prior candidate. Runtime evidence must bind both final JAR hashes. Before live use, complete the read-only preflight and human checklist with the installed Vault/Essentials/GriefPrevention/Geyser/Floodgate combination. The earlier preflight reported Essentials 2.22.0 warning about an unsupported server version; the guard does not remove that warning. For a deployment actually using legacy ChestShop trading, rollback must retain the guard or disable that trading. Standalone native/Spawn-only ExcelShops does not require ChestShop. Restoring instant purchases is not a safe silent rollback.

