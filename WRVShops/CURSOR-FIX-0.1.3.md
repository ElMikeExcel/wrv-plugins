# ExcelShops 0.1.3 cursor fix candidate

Status: built and tested off-server; not merged or installed. Live remains 0.1.2 until explicit maintenance approval.

## Scope

- Keep the same Java/fallback inventory window while changing native/Spawn purchase quantity or owner item price.
- Apply rapid queued adjustments to current state; Review and Use consume the latest value.
- Preserve bounds, next-tick server scheduling, stale-session rejection, cancellation, separate review and final confirmation, and one-shot checkout.
- Correct publication feedback: buyers may right-click the chest or attached sign.
- Remove the misleading chat-command hint from Java adjustment screens. The command-context issue is not changed.
- Native Bedrock numeric forms, optional legacy ChestShop UI, economy/XP/Jobs settings, permissions, menu prices, stock and persistent schemas are unchanged.

## Verification

- Root `./build.sh`: **631 Java tests + 55 Python checks**, zero failures/errors/skips. This includes **77 new ShopUi regression cases**.
- Two local builds produce the same ExcelShops 0.1.3 JAR: SHA-256 `37ac2dbbf084c0a1c1d57dbff2f886ccf2194a439de955c74765a97f1a9160a4`.
- Exact Paper 26.2 build 123 / Temurin 25.0.4+7: **4 complete isolated boots / 757 assertions / zero failures**, covering the final JAR and final harness source. All child processes exited cleanly; the loopback port was free afterward.
- Native Paper Inventory and InventoryView identity, open/close counts and synthetic cursor-item preservation remain stable across queued price/quantity adjustments. Human pointer coordinates are not measured.
- Independent static review found no blocking issue. Existing native checkout continues to require a separate final confirmation and consumes it once.
- [Build/source hash summary](verification/runtime-harness/evidence-excel-0.1.3/build-summary.json) and [runtime evidence](verification/runtime-harness/evidence-excel-0.1.3/summary.json).

Historical 0.1.2 migration/legacy-stack results are not counted as new 0.1.3 runtime verification. This change does not certify the optional legacy ChestShop path or the exact live plugin stack.

## Menu-first human retest after approved installation

1. Open compass menu → Spawn Shop → an item. Move the pointer over +1; click several times without moving it. The pointer should stay there and each click should update quantity/total. Try minus and a bound.
2. Choose Review, then Change quantity and Back/Cancel. Check the current quantity and total; these steps must not charge money or give items.
3. With a deliberately small agreed test purchase, continue to the separate final confirmation. Rapidly click Confirm; check exactly one charge and the matching item count.
4. Open your chest/sign → Edit item prices → an item (or set up an ordinary sign-on-chest shop). Rapidly use the price +/− buttons. Check the pointer stays in place and Use/Review show the final price. Cancel first; publish only if you intend that price.
5. A second player should be able to open either the chest or its attached sign. The owner sees management; buyers see the read-only catalog.
6. Bedrock: verify native exact-number quantity/price entry, review and cancellation, plus the inventory fallback when applicable.

Automated checks use synthetic players. They cannot prove physical mouse behavior, real client packets, installed Geyser/Floodgate, the exact live provider stack or production state.

## Deployment boundary

Install only the reviewed ExcelShops 0.1.3 JAR during separately approved stopped-server maintenance with coordinated backup and startup verification. Do not install the optional guarded ChestShop JAR for this standalone server. Preserve the complete existing ExcelShops data folder, both journals, stock, balances, menus and configuration. Never hot-reload a replacement JAR. No live change is authorized by this document.
