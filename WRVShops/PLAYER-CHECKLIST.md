# ExcelShops: player acceptance checklist

**Prepared candidate only. Do not assume these controls are live yet.** The owner must separately approve installation after server preflight. Existing legacy ChestShop shops keep their own current controls.

## Buy from a player

- Open the chest labeled **[Excel Shop]** or its sign. Old managed **[WRV Shop]** labels remain supported.
- Tap the item. Check its enchantments/custom lore, unit price and stock.
- Choose quantity. Bedrock phones get a number-entry form; Java has ± buttons. `/excelshop amount 19` gives exact input on an open quantity screen. Inventory controls are the fallback if forms are unavailable.
- Review **quantity × unit price = total**.
- On the separate “Are you sure?” screen, choose **Confirm purchase** once.
- Check money and delivered quantity. Cancel, Back, closing a screen and disconnecting before confirmation must change neither.

Stay within six blocks. If stock, price or ownership changed, start a fresh review. Partial matching stacks count as inventory room. A full inventory or insufficient funds must leave both money and stock unchanged. Any recovery warning means stop and ask the owner; don't repeatedly retry.

## Make a shop

- Own the actual GriefPrevention claim covering both chest and sign. Container/build trust alone does not qualify.
- Place your own **single** normal/trapped chest and stock it. Double chests and legacy ChestShop signs are not converted.
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

## Required real-player checks before wider use

Use a disposable staging economy with one Java player and one Bedrock phone player:

1. Create, decline, cancel, resume and publish a shop without special sign syntax.
2. Verify custom item lore/enchantments and two identical variants grouped as one listing.
3. Cancel on each buying screen; verify no money/item change. Buy once, then double-tap final confirmation and verify only one transaction.
4. Test insufficient money, full inventory, existing partial stack, stock/price changes, distance change and logout before confirmation.
5. Test another player's inability to open free stock, use hoppers, break the sign/chest or edit prices.
6. Restock, edit price and close as the owner; confirm stock is preserved.
7. Restart cleanly, repeat access and a disposable purchase, and verify state persists.

Automated server/fixture tests do not prove actual phone rendering or the installed economy/claims integration.
