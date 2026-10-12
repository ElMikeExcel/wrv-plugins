# ExcelShops: player acceptance checklist

**Prepared candidate only. Do not assume these controls are live yet.** The owner must separately approve installation after server preflight. The approved package must install ExcelShops 0.1.2 and the full guarded ChestShop JAR together.

## Buy from a player

- Open the chest labeled **[Excel Shop]** or its sign. Old managed **[WRV Shop]** labels remain supported.
- Tap the item. Check its enchantments/custom lore, unit price and stock.
- Choose quantity. Bedrock phones get a number-entry form; Java has ± buttons. `/excelshop amount 19` gives exact input on an open quantity screen. Inventory controls are the fallback if forms are unavailable.
- Review **quantity × unit price = total**.
- On the separate “Are you sure?” screen, choose **Confirm purchase** once.
- Check money and delivered quantity. Cancel, Back, closing a screen and disconnecting before confirmation must change neither.

Stay within six blocks. If stock, price or ownership changed, start a fresh review. Partial matching stacks count as inventory room. A full inventory or insufficient funds must leave both money and stock unchanged. Any recovery warning means stop and ask the owner; don't repeatedly retry.

## Existing ChestShop: Buy or Sell

- Tap an existing ChestShop sign. Choose **Buy from shop** or **Sell to shop** explicitly; the available directions come from the sign. Left/right click and crouching must never immediately trade.
- Choose an **item count**, not a bundle count. Phone numeric entry, Java buttons and `/excelshop amount` must agree.
- Review the effective quantity, total, item metadata and direction. If stock/capacity permits only a partial amount, that adjusted quantity must be visible before approval.
- Continue to the separate final screen and choose **Confirm Buy** or **Confirm Sell** once. Buy spends money and receives items; Sell gives items and receives money.
- Cancel, Back, close each screen or disconnect before confirmation. Verify both accounts and both inventories are unchanged. Reopening a view or submitting an old phone form must not revive a cancelled trade.
- Verify existing sign prices (including free offers), owner accounts, permissions, chest protection and both halves of a double chest are preserved. No sign rewrite or conversion is required.
- If confirmation is unavailable or recovery is required, stop. Guarded ChestShop must deny trades while ExcelShops is missing or disabled and continue protecting the chest. Never swap back to an unguarded JAR to bypass this state.

## Make a shop

- Own the actual GriefPrevention claim covering both chest and sign. Container/build trust alone does not qualify.
- Place your own **single** normal/trapped chest and stock it. Native setup refuses double chests and existing ChestShop signs; legacy shops keep their original single/double-chest storage and use the separate guarded flow.
- Sneak/crouch while placing an ordinary wall sign against it, or a standing sign on top. Finish the normal sign editor.
- At **Make this a shop?**, choose Yes. No keeps ordinary storage and its label.
- Select every distinct item type and set a **price for one item**. Phones use numeric entry; Java uses price buttons or `/excelshop amount 12.50` on that price screen.
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

After coordinated installation, `/shop`, the compass and ShopVillager retain their normal entry points; `/excelshop` directly opens the confirmed catalog. Check **bundle** size: the baseline offer is 16 bread for $40, so two bundles mean 32 bread for $80. Current live prices must be verified separately before installation.

## Scope warning

This pair does not itself add confirmation to the Jobs item shop or the `advanced_menu`/`requirements_menu` DeluxeMenus purchase examples found in preflight. Reversible pause preparation is approved, but their actual stopped-server configuration installation remains a separate approved step. Verify after restart that Jobs cannot purchase, both examples are unloaded, the three other menu registrations still work, and no examples were automatically restored. Do not treat this checklist as proof that every server purchase route is confirmed.

## Required real-player checks before wider use

Use a disposable staging economy with one Java player and one Bedrock phone player:

1. Create, decline, cancel, resume and publish a shop without special sign syntax.
2. Verify custom item lore/enchantments and two identical variants grouped as one listing.
3. Cancel on each buying screen; verify no money/item change. Buy once, then double-tap final confirmation and verify only one transaction.
4. Test insufficient money, full inventory, existing partial stack, stock/price changes, distance change and logout before confirmation.
5. Test another player's inability to open free stock, use hoppers, break the sign/chest or edit prices.
6. Restock, edit price and close as the owner; confirm stock is preserved.
7. Test both legacy Buy and Sell, free and paid signs, double chests, limited stock/capacity, owner balance changes and both accounts' exact balance/item deltas. Rapid repeat Confirm must trade only once.
8. Verify unsupported providers/versions, nonzero ChestShop taxes, non-two-decimal price precision and a configured server economy account fail closed. Do not alter real server policy to bypass the refusal.
9. On the disposable server, test ExcelShops absent/disabled, stale phone callbacks and dirty restart. Confirm default-deny trading and preserved ChestShop protection.
10. Restart cleanly, repeat native/legacy access and a disposable Buy and Sell, and verify state persists.

Automated tests do not prove human client rendering/input or the exact installed stack. The isolated legacy harness uses actual EssentialsX 2.22.0 and GriefPrevention but substitutes official Vault 1.7.3-b131; the live Vault 2.0.0 identity remains unknown. Human Java and Bedrock phone checks, real player saves and Geyser/Floodgate remain pending until recorded directly.
