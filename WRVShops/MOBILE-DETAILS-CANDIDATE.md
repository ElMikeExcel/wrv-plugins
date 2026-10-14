# Mobile details and quantity hub: candidate implementation

## Status

Off-server preparation only. No publication, installation, configuration deployment, or live restart. The mobile candidate uses the separately chosen quoted version `1.1`. This is distinct from the behavior-identical version-only 1.0 JAR.

Final artifact verification: 760 Java tests and 47 Python checks pass; an independent clean rebuild is byte-identical. Four pinned Paper 26.2 build 123 boots pass 1,299 assertions. A separate 50-assertion native-plugin run verifies official binary API/Cumulus builder compatibility and actual DeluxeMenus asynchronous holder registration and same-holder cleanup using synthetic players and fake economy. All owned JVMs exited 0 and were waited.

The native run loaded official DeluxeMenus 1.14.1, Geyser 2.11.3-b1251 (commit 50a464d), Floodgate 2.2.5-b141 (commit 81b65cc) and PlaceholderAPI 2.12.3. Publisher hashes were checked. These versions/commits match live logs; production-byte identity was not yet compared. Geyser encryption setup could not resolve Minecraft discovery/auth endpoints in the isolated JVM. No actual Bedrock session, delivered form packet, phone gesture, live purchase or full special-plugin checkout acceptance is claimed.

## Navigation

- Root: five category buttons, Back to Menu (the fixed existing `menu` player command), Cancel.
- Category: item-first name with “view details,” separate bundle size and price; Back to categories and Cancel.
- Ordinary item: full original lore, current balance, selected count, item total, and projected balance. Enter exact amount / Review this quantity / Back to the originating category / Cancel.
- Exact amount: a native CustomForm input, with the same item details in its existing input label. Input remains index zero. Valid submission returns to that item's hub with the updated quantity. Bad numeric text gives feedback and a fresh input token for the same item/count. Close or invalid protocol response cancels. A numeric keyboard is not promised.
- Review and final Confirm: existing charge/checkout code is preserved. Their mobile quantity-change/Back route returns to the same item hub. Final Confirm is still separate, server-owned, single-use, and revalidated.
- Special products: full original item-specific lore, fixed quantity one, Open guarded review / Back to Potions & Protection / Cancel. Commands remain `mobveil buy <verified player name>`, `deathprotection buy basic <verified player name>`, and `deathprotection buy premium <verified player name>`. Existing special review Back returns `/shop`, hence native root; no category is inferred from private plugin internals.

## Preservation and security

The six `mobile-menus` resources are exact copies of the verified installed menus, including all 21 product blocks, original descriptions/lore, amounts, prices and original click routes. The Java model validates the fixed allowed IDs and commands but never evaluates arbitrary commands from YAML. It maps only three fixed special routes and validates the server-provided player name before dispatch (Unicode letters are preserved; whitespace and control characters are rejected). It never grants permissions or OP.

The existing live catalog remains authoritative for ordinary transactions. Mobile display price/bundle values must match its selection or the form closes with an explanation. No `saveResource`, file replacement, catalog rewrite, or DeluxeMenus configuration mutation is used for these display resources. Checkout, storage, ShopAdapter, SpawnCatalog, legacy shop code, the original Java quantity inventory implementation, and the final charging closure remain unchanged.

Each form is one-shot through the existing BedrockForms gate and bound to a server-side Page identity. Replaced products/pages, duplicate responses, old timeouts, quit, world change, teleport, other inventory openings, and shutdown cannot use that page's late callbacks to reopen or buy. Mobile-origin form failure cancels rather than falling through to an unread inventory; Confirm failure invalidates its new quote. Null-origin Java/chest fallback behavior is retained.

## Source-reviewed DeluxeMenus entry bridge

Pinned DeluxeMenus 1.14.1's click listener ignores prior click cancellation. The adapter therefore cancels a positively identified Bedrock shop InventoryOpenEvent at HIGHEST before Paper sends an open-screen packet. It identifies the actual plugin-loaded MenuHolder class and verifies holder/viewer/inventory identity plus one of the six exact allowed menu names. Java and unrelated inventories are untouched.

DeluxeMenus registers the holder even when opening was cancelled. The next-tick handoff uses public `Menu.getMenuHolder(Player)` and `Menu.closeMenu(plugin, player, false, false)` only when the currently registered holder is the exact intercepted object. It does not close another inventory or run close actions. A generation, unchanged previous inventory, current native UI token, online state, enabled plugin, and exact holder cleanup gate the form opening. Stale handoffs may clean only their own matched holder and then stop. Shutdown clears pending requests.

This reflection contract is based on DeluxeMenus release 1.14.1 source and exact Paper 26.2 build 123 source. The separate official-binary run now confirms actual registry behavior after a synthetic cancelled event, repeated cleanup, and two asynchronous opens with stale-holder protection. It does not simulate actual network clients or all event races. Unexpected missing identity APIs cannot safely be inferred from inventory titles, so unsupported detection/interfaces remain an integration limitation to resolve before release.

## Compatibility boundaries

- Paper 26.2 build 123: four isolated regression boots passed; real-client acceptance remains pending.
- DeluxeMenus 1.14.1: official binary public methods, cancelled-open registration, exact cleanup and stale-holder protection passed with synthetic player events. Actual player timing and reload/disconnect races remain pending.
- Geyser 2.11.3 build 1251 and Floodgate 2.2.5 build 141: official binary public API signatures, actual Cumulus form builders and absent-connection rejection passed. Positive live session mapping and delivered forms remain pending.
- Local Geyser transport is required by the existing form sender. Floodgate-only/proxy detection can identify Bedrock but unavailable form transport closes safely rather than opening unread controls.
- ExcelDeathProtection 0.1.3 and ExcelMobVeil 0.1.0: original guarded dispatch commands preserved; their own guarded review is preserved and Back remains root.
- Other plugins' versions and upstream schemes are unchanged.

## Controlled rollout and remaining acceptance

The final 1.1 JAR SHA-256 is `f28c5c4ba5d231219735228d1d556b39de57cc8a965d5fb62eddb31dbceab9e4`. This source is for a separately approved bounded pilot; it does not authorize installation.

Freshly verify the current 1.0 JAR, six organized menus, current catalog/configuration, bridge versions, checkout health and zero online players. Stage only the candidate JAR with a non-.jar suffix. Shut down cleanly, preserve 1.0 under a disabled filename, activate 1.1, verify its hash, then start and check loaded versions and new errors. Hold the restart whenever any player is online.

Remaining acceptance includes actual positive Bedrock session mapping, readable delivered forms, Back/Cancel/X, rapid or duplicate input, special-plugin review/Confirm and Java/chest-shop client smoke checks. Automated checks do not substitute for this.

If rollback is needed during an approved empty-server window, shut down cleanly, preserve 1.1 with a disabled suffix, restore the saved 1.0 JAR filename and hash, then start and check health. Restore no player data or transaction journal: doing so could undo purchases. Never deploy the historical paired ChestShop pack or bundled display fixtures over live files.
