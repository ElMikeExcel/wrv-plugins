# ExcelShops: player acceptance checklist

**ExcelShops 0.1.3 is installed standalone; human cursor retest pending.** The October 12 update keeps quantity and owner-price adjustments in the same inventory window. Runtime identity and the economy connection were checked. An earlier owner-relayed Java PC report on 0.1.2 passed Spawn Shop menu/prices/cancel/torch purchase and owner setup/publication; it does not certify the new cursor behavior. ChestShop remains absent. The actions below remain a full checklist, not a claim that every case passed. See the [deployment record](DEPLOYMENT-2026-10-12.md).

## Priority retest: Java plus/minus controls

Open **compass menu → Spawn Shop → an item**. Keep the pointer over +1 and click several times without moving it; quantity/total should update without the pointer jumping. Try minus, Review, Change quantity and Back/Cancel. Then repeat price adjustments in **your chest/sign → Edit item prices**. Cancel before any purchase or publication unless you intend it. The full [0.1.3 retest checklist](CURSOR-FIX-0.1.3.md#menu-first-human-retest-after-installation) also covers bounds, one-shot confirmation, buyer entry and Bedrock. No human result is recorded yet.

## Buy from a player

- Open the chest labeled **[Excel Shop]** or its sign. Old managed **[WRV Shop]** labels remain supported.
- Tap the item. Check its enchantments/custom lore, unit price and stock.
- Choose quantity. Bedrock phones get a number-entry form; Java has ± buttons. Do not rely on chat-command quantity entry as a Java workaround; closing the inventory clears its input context. Inventory controls are the fallback if forms are unavailable.
- Review **quantity × unit price = total**.
- On the separate “Are you sure?” screen, choose **Confirm purchase** once.
- Check money and delivered quantity. Cancel, Back, closing a screen and disconnecting before confirmation must change neither.

Stay within six blocks. If stock, price or ownership changed, start a fresh review. Partial matching stacks count as inventory room. A full inventory or insufficient funds must leave both money and stock unchanged. Any recovery warning means stop and ask the owner; don't repeatedly retry.

## Optional ChestShop compatibility checks (not active on Wink Reyes Valley)

This section applies only if the full guarded ChestShop integration is separately installed. Current standalone ExcelShops uses the native and Spawn Shop instructions instead.

- Tap an existing ChestShop sign. Choose **Buy from shop** or **Sell to shop** explicitly; the available directions come from the sign. Left/right click and crouching must never immediately trade.
- Choose an **item count**, not a bundle count. Phone numeric entry, Java buttons must show the same effective quantity.
- Review the effective quantity, total, item metadata and direction. If stock/capacity permits only a partial amount, that adjusted quantity must be visible before approval.
- Continue to the separate final screen and choose **Confirm Buy** or **Confirm Sell** once. Buy spends money and receives items; Sell gives items and receives money.
- Cancel, Back, close each screen or disconnect before confirmation. Verify both accounts and both inventories are unchanged. Reopening a view or submitting an old phone form must not revive a cancelled trade.
- Verify existing sign prices (including free offers), owner accounts, permissions, chest protection and both halves of a double chest are preserved. No sign rewrite or conversion is required.
- If confirmation is unavailable or recovery is required, stop. Guarded ChestShop must deny trades while ExcelShops is missing or disabled and continue protecting the chest. Never swap back to an unguarded JAR to bypass this state.

## Make a shop

- Own the actual GriefPrevention claim covering both chest and sign. Container/build trust alone does not qualify.
- Place your own **single** normal/trapped chest and stock it. Native setup refuses double chests and existing ChestShop signs; legacy shops keep their original single/double-chest storage and use the separate guarded flow only where that optional ChestShop compatibility is installed.
- Sneak/crouch while placing an ordinary wall sign against it, or a standing sign on top. Finish the normal sign editor.
- At **Make this a shop?**, choose Yes. No keeps ordinary storage and its label.
- Select every distinct item type and set a **price for one item**. Phones use numeric entry; Java uses price buttons. The installed 0.1.3 update keeps these adjustments in the same window; physical cursor behavior still needs a human retest.
- Review owner, each unit price, current stock and stock value. Separately choose **Confirm and publish shop**.
- After canceling or adding stock to an empty chest, look at the ordinary attached sign and use `/excelshop setup` to resume.

All matching physical stock is for sale at the chosen unit price. There is no copying stock into a virtual chest.

## Maintain your shop

- Open your own shop or look at it and run `/excelshop manage`.
- **Open stock chest:** restock/remove your items normally. Give newly introduced item types a price before they sell.
- **Edit item prices:** review and confirm again. Sold-out listings can be restocked.
- **Close shop:** confirm returning to ordinary storage. Every stock item stays in the chest. Only then break its chest/sign.

`/wrvshop` still works as an alias. No permission grants or claim-trust changes are needed for the rename.

## Spawn Shop

For normal shopping, **open the compass menu → Spawn Shop**, or tap the Spawn Shop villager. Choose the item and quantity in the menus, review, then confirm; no chat command is needed. `/shop` and `/excelshop` are optional fallback entry points. Check **bundle** size: the baseline offer is 16 bread for $40, so two bundles mean 32 bread for $80. Current live prices must be verified separately before installation.

## Scope warning

This pair does not itself add confirmation to the Jobs item shop or the `advanced_menu`/`requirements_menu` DeluxeMenus purchase examples found in preflight. The separately approved October 12 installation applied the reversible Jobs item-shop and example-menu pauses. Stored settings and the three retained menu registrations were checked; player attempts, including aliases and alternate entry points, remain acceptance tests. Do not treat this checklist as proof that every server purchase route is confirmed.

## Required real-player checks before wider use

Use a disposable staging economy with one Java player and one Bedrock phone player:

1. Create, decline, cancel, resume and publish a shop without special sign syntax.
2. Verify custom item lore/enchantments and two identical variants grouped as one listing.
3. Cancel on each buying screen; verify no money/item change. Buy once, then double-tap final confirmation and verify only one transaction.
4. Test insufficient money, full inventory, existing partial stack, stock/price changes, distance change and logout before confirmation.
5. Test another player's inability to open free stock, use hoppers, break the sign/chest or edit prices.
6. Restock, edit price and close as the owner; confirm stock is preserved.
7. Only if guarded ChestShop compatibility is separately installed, test both legacy Buy and Sell, free and paid signs, double chests, limited stock/capacity, owner balance changes and both accounts' exact balance/item deltas. Rapid repeat Confirm must trade only once.
8. For that optional compatibility path, verify unsupported providers/versions, nonzero ChestShop taxes, non-two-decimal price precision and a configured server economy account fail closed. Do not alter real server policy to bypass the refusal.
9. On the disposable server, test stale phone callbacks and dirty restart. If guarded ChestShop compatibility is installed, also test ExcelShops absent/disabled and confirm default-deny legacy trading with preserved ChestShop protection.
10. Restart cleanly, repeat native/Spawn access and an authorized disposable purchase, and verify state persists. Only when guarded ChestShop compatibility is installed, also repeat legacy access and a disposable Buy and Sell.

Automated tests do not prove human client rendering/input or the exact installed stack. The isolated legacy harness uses actual EssentialsX 2.22.0 and GriefPrevention but substitutes official Vault 1.7.3-b131; the later live receipt identifies Vault-Updated 2.0.0 and a successful guarded EssentialsXEconomy connection, not completed player trade acceptance. Human Java and Bedrock phone checks, real player saves and Geyser/Floodgate remain pending until recorded directly.
