# WRVShops 0.1.0

**Draft candidate, not installed on the live server.** This is original MIT-licensed code. It adds a guided shop layer alongside the existing ChestShop plugin; it does not adopt or uninstall existing ChestShop shops. The legacy live shop keeps its current behavior until a separately approved deployment.

## Players: buy from a chest

1. Open a chest labeled `[WRV Shop]`, or interact with its sign.
2. The read-only chest screen shows the listed items, unit prices and current stock. You cannot drag free stock into your inventory.
3. Choose an item, then a quantity. The next screen shows quantity, price and total, including enchantments/custom lore where relevant.
4. Review the purchase, then choose **Confirm purchase** on the separate “Are you sure?” screen.
5. Only the final confirmation can move money or items. Back, Cancel, closing a screen and disconnecting do not buy anything.

Stay within six blocks of the shop. Quotes expire after 60 seconds by default. If price, ownership, item metadata or chest stock changed, review a fresh quote. Full inventories, insufficient funds, unavailable accounts and unavailable protection checks fail without a purchase. A partially filled matching stack counts as available inventory room.

### Java and Bedrock controls

- Both platforms browse the physical chest's contents through a read-only inventory menu. Left and right clicks do the **same** action; no Java-only left/right buy/sell convention.
- Java uses clearly labeled quantity buttons (`−64`, `−8`, `−1`, `+1`, `+8`, `+64`) and Review/Confirm/Back/Cancel. The optional `/wrvshop amount 19` enters an exact quantity while that screen is open.
- Bedrock uses native Geyser number-entry forms for quantity and unit price, followed by separate review and final confirmation forms. Enter a whole-number quantity or a positive price with at most two decimals. If forms are unavailable, the same inventory buttons are used.
- Java price controls adjust by 0.01, 0.10, 1, 10 or 100 in either direction. `/wrvshop amount 12.50` optionally enters an exact unit price while its screen is open.
- Real Java and Bedrock phone rendering/input with the live Geyser/Floodgate builds remains a required staging check. Automated server-side/reflective tests do not prove it.

## Owners: make a shop

1. Use **one single normal or trapped chest** in a GriefPrevention claim you actually own. Both chest and sign must be inside your claims. Build/container trust is not ownership. Do not use another player's recorded chest.
2. Fill it with the items you want to sell.
3. Crouch/sneak while placing a wall sign against the chest, or a standing sign on top. Finish the normal sign editor. No special sign text or price syntax is needed.
4. The prompt asks **Make this a shop?** Choose Yes to start, or No to keep ordinary storage and its label.
5. Select each distinct item in the chest and choose its **price for one item**. The setup screen shows the actual stock count. All matching physical stock is offered at that unit price; items are never copied into a separate virtual stock store.
6. Review your owner name, each item, price and stock count, then separately **Confirm and publish shop**. Until then, the chest is ordinary storage.
7. The sign is labeled with the shop and owner. Other players can now browse and buy through confirmation.

To resume after canceling, look at your ordinary sign and run `/wrvshop setup`. If the chest is empty, add stock first, then resume setup. Existing stock/sign/claim changes during setup require a fresh review. Setup and price edits never move money or stock.

**Single chests only in this first candidate.** Double chests are refused with guidance; existing double-chest ChestShop shops are untouched. Another sign next to the chest or a ChestShop-formatted sign is refused to avoid taking over an existing shop. Use a separate chest for the new flow.

### Restock, change prices or close

Open your own shop chest/sign for its owner menu, or look at it and use `/wrvshop manage`.

- **Open stock chest:** move your own items in and out normally. Current matching stock is what buyers can purchase. New item types need a price through Edit item prices before they appear for sale.
- **Edit item prices:** choose prices, review and confirm. Sold-out existing listings remain available to restock.
- **Close shop:** separately confirm returning it to ordinary storage. All items stay put. Its shop label is cleared, and then you can break the sign/chest.

Active shop chests/signs cannot be broken, exploded, piston-moved, extended into double chests, automated with hoppers, or opened directly by buyers. Owner access is revalidated. Do not grant public container trust as a workaround.

## Spawn Shop

The companion DeluxeMenus menu becomes a **display-only launcher**. `/shop`, the compass menu and existing ShopVillager NPC keep their entry points after the matching menu is deployed; `/wrvshop` also opens the safe catalog directly.

Spawn stock remains unlimited. Prices retain the original **bundle** economics: for example 16 bread costs 40; two bundles deliver 32 bread for 80. The UI explicitly labels bundle size, bundle price and final item count. This avoids silently changing unit prices that do not divide into whole cents. Enchanted books are built with the server item API, never unverified console `give` syntax.

The player command selects only a server-side offer ID. It cannot supply a price, item or recipient. Spawn purchases use the same final-confirm checkout and inventory capacity checks as chest purchases.

## Administrator setup and migration

1. **Do not hot-install this on production.** Back up plugin files, world/chest data, player inventories and economy data together. Use a disposable copy with the intended exact plugin versions first.
2. Build with `./build.sh` at the repository root or `./WRVShops/build.sh`; Java 21+ and Maven 3.9+ are required. Candidate output: `WRVShops/target/WRVShops-0.1.0.jar`.
3. Stage the JAR with Vault and an enabled economy provider. Defaults require working GriefPrevention ownership checks. Geyser is optional for native Bedrock forms. ChestShop remains installed for legacy shops.
4. Install the matching generated `SpawnShop-DeluxeMenus/spawnshop.yml` into DeluxeMenus' `gui_menus/` and `SpawnShop-DeluxeMenus/spawn-catalog.yml` into `plugins/WRVShops/`. Keep DeluxeMenus' existing menu registration and ShopVillager target.
5. **Replace the old immediate-purchase menu as part of the same maintenance change.** Merely adding the JAR does not remove `[takemoney]`/`minecraft:give` from an old live DeluxeMenus file. Merely replacing the menu without the plugin fails closed but cannot open checkout. Compare both deployed files to the reviewed candidate.
6. Start the isolated server and verify the provider name, no recovery lock, correct prices, normal-player permissions, claim ownership, Java/Bedrock controls and real purchase/refund behavior with disposable money/items. Exact live economy/claims/Geyser combination must still be tested.
7. Only after review and explicit deployment approval, repeat the controlled installation on the live server. Never silently convert legacy ChestShop signs or migrate their stock/prices.

Permissions: `wrvshops.use` and `wrvshops.create` default to true. Claim ownership and recorded placer checks still apply. No operator bypass steals or adopts another player's shop. Set these permissions explicitly in the existing permission manager if access should be restricted.

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
- Startup failures leave the plugin in protection-only mode when its code can still load. Both chest and sign carry persistent shop tags, so a damaged/missing registry does not intentionally expose raw stock. If WRVShops itself is removed or cannot load at all, GriefPrevention's own rules apply: container-trusted players may still access claim chests. Keep the plugin present and avoid public container trust; don't treat plugin-absent stock as universally theft-proof.
- A claim transfer or corrupt/missing identity blocks trades/owner edits. Preserve the stock and ask an administrator to reconcile it; there is no automatic ownership reassignment.

For recovery: stop the server, preserve the entire `plugins/WRVShops/` folder plus contemporaneous economy/player/world backups, and inspect `checkout/transactions.log` and the corresponding saved inventories/balances. Reconcile every uncertain transaction exactly once with the owner, documenting the correction. Keep the original evidence. Only after verified corrections, archive the reconciled checkout directory and start with a fresh journal. Do not delete only `active-session`, fabricate terminal journal records or retry purchases as a recovery technique. Recovery edits/payments are administrator actions, never automatic plugin behavior.

Journal size is capped at 256 MiB; over-limit state pauses checkout rather than dropping evidence. Archive only offline after verifying terminal records and a clean shutdown. Storage caps and single-chest scope suit this small server; high-volume use needs a separately designed persistent economy/storage system.

## Verification and remaining gates

Unit tests cover coordinator/economy boundaries, durable journal/restart behavior, registry persistence/claims/identity and menu helpers. The portable isolated runtime harness lives under `verification/runtime-harness/`; it uses exact Paper 26.2 #123 / Temurin 25.0.4+7, native chest inventories and item metadata, and explicitly fake economy/claims/player fixtures. It does not contact production.

See the harness evidence for the final tested candidate and test counts. Before live use, run two real players (Java and Bedrock phone) through setup, price editing, cancellation, final confirm, changed stock, full inventory, logout, protection failures and clean restart with the installed Vault/Essentials/GriefPrevention/Geyser/Floodgate versions. Essentials 2.22.0 currently reports the server version unsupported in the live preflight; this candidate does not remove that upstream compatibility warning.
