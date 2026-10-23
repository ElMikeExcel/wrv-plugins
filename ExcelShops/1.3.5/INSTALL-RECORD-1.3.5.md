# ExcelShops 1.3.5 — purchase history log (purchases.csv) + /excelshop stats

Built Oct 23, 2026 (box, JDK 25.0.4.1, javac --release 21). STAGED, NOT INSTALLED.

## Why
Mike, Oct 23, 2026: "Lets create a log of the buy history for the shop moving forward too so we can track market trends and adjust prices as needed."
Existing logging checked first: checkout/transactions.log (FileJournal) is a fail-closed reconciliation journal
(tab-separated, base64 offer ids, buyer UUID only, no names, item ids or balances, 256 MB safety cap that pauses shops).
It is not a trend log and must not be repurposed, so 1.3.5 adds a separate evidence-only CSV.

## Base
ExcelShops 1.3.4 source zip (Drive 11Inq1GzaX0e6PYoZFIqKumYVI_5OaOA4, SHA-256 527a8a9e0d90d962c8c1bb36741227293c3c547d8ede001b64a6e8c959e5d522).
Base proven on the box first: 412/412 tests, every compiled class byte-identical to ExcelShops-1.3.4.jar
(SHA-256 8c38f67e929af4e7a4975cb0752e5652e30982d23c86b58e10253a0b449f9005).
CAVEAT: records show 1.3.3 as the last INSTALLED version (WRV-108, Oct 20). No 1.3.4 install record was found.
1.3.5 therefore also carries 1.3.4's Back-button/affordability soft-block changes. Check the live jar SHA before installing.

## Change (7 files)
- NEW log/PurchaseLog.java: appends to plugins/ExcelShops/purchases.csv on ONE private daemon writer thread
  (no interleaving, server thread never touches disk). Each line opens with APPEND, writes, closes (crash loses at most one line;
  no rotation state, safe across midnight/long uptimes). Header written once when the file is new/empty. RFC 4180 quoting,
  spreadsheet formula injection neutralised. Failures only warn; never block or alter a purchase. close() drains up to 5 s on disable.
- NEW log/Platform.java: java/bedrock via the Floodgate API through floodgate's own class loader (reflection, no compile dep);
  "java" when floodgate is absent, "unknown" on lookup error.
- ShopUi.java: one call after a SUCCESS checkout (spawn and player chest shops).
- WRVShops.java: PurchaseLog lifecycle, logPurchase (balance read on the server thread via BalanceDisplay), itemId
  (enchanted books carry stored enchants, e.g. minecraft:enchanted_book[mending:1]), /excelshop stats [days] (default 7, 1-3650;
  reads the CSV on the writer thread, replies on the server thread; total, buyers, spawn vs player revenue, top 10 by revenue).
- plugin.yml: version 1.3.5, usage, new permission wrvshops.admin (default: op).
- config.yml: purchase-log: true (set false to disable). An existing live config.yml without the key defaults to true.
- tests: log/PurchaseLogTest.java (7 tests: format/zone, 8x250 concurrent appends no interleave, escaping, stats window/top/bad lines,
  missing file + closed log, write failure warns, platform detection via the Floodgate fixture).
Not logged: legacy ChestShop sign trades (LegacyShopBridge), by design for this version.

## CSV columns
timestamp,player,uuid,platform,shop,item,qty,bundle_size,unit_price,total,balance_after,offer_id
timestamp = ISO-8601 with offset in the server zone; qty = bundles bought; unit_price = price per bundle; balance_after blank if unreadable.

## Verification
- Tests: 419/419 (412 base + 7 new).
- Reproducible jar: two packagings byte-identical; clean-room rebuild from the source zip byte-identical (see SHA256SUMS).
- Rig boot (scratch Paper 26.2 build 133, port 25711, offline, Vault + proxy economy, staged 47-offer catalog): ExcelShops 1.3.5 enabled,
  purchases.csv created with header + row, item id minecraft:enchanted_book[mending:1], console /excelshop stats 7 printed
  "1 purchases by 1 players, $2,000.00 / Spawn shop $2,000.00 / 1. spawn:mending". Evidence: rig-1.3.5/.

## Install (empty-server window, RESTART required; do not hot-load)
1. Confirm server empty. Record live ExcelShops jar name + SHA-256.
2. Back up: plugins/ExcelShops-<live>.jar -> .bak-<date>; plugins/ExcelShops/ folder copy.
3. Remove the old jar from plugins/, upload ExcelShops-1.3.5.jar (SHA-256 864d14d238ef03cd34fb35e1ab3574812bc487740ce6cbe00caabc49b6020156).
4. Restart. Expect "ExcelShops enabled" and no "protection-only" line.
5. Run /excelshop stats from console: expect 0 purchases. After the first real purchase, purchases.csv appears.
Rollback: stop, restore the .bak jar, restart. purchases.csv is inert to older versions.
