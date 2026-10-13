# ExcelShops 0.1.5: balance display candidate

**Prepared off-server; not installed.** Live ExcelShops remains 0.1.4. This candidate adds balance visibility to existing shop interfaces. Installation and restart require separate approval. No prices, economy rates, permissions, inventories, balances or live files are changed by preparing this update.

## Player instructions

- Browse Spawn Shop or another player's chest shop. Your current balance appears first in the inventory header, so you do not need to hover over a gold nugget to find it. Native inventory screens also include a labeled balance icon. Owner setup, price entry, review, publication, management and close-shop screens show balance too.
- Choose a quantity. The display shows your balance, the purchase cost and what would remain. Plus/minus controls recalculate the preview without reopening the inventory, preserving the cursor fix.
- **Enter Amount** keeps private Java numeric text entry and Bedrock native number forms. The input screen's preview is explicitly labeled for the previously selected amount. Native text fields cannot recalculate while you type; submit the amount to see the new exact total and remaining balance at review.
- Review, then open the separate final confirmation. Both screens read your balance again. An insufficient-funds warning shows the shortfall, including a negative projected balance rather than pretending the purchase is affordable.
- Only the existing final confirmation performs the transaction. A displayed projection is not a reservation or guarantee: the checkout independently rechecks funds, price, stock, inventory room and provider at commit time. Cancel, Back, close, input, invalid-input retry and timeout do not spend money.
- Legacy ChestShop compatibility, when separately installed, shows **Purchase cost** for buying and **Sale proceeds** plus the credited projected balance for selling. Legacy quantity entry clearly says its total is calculated at review, because upstream partial-quantity and pricing rules determine the actual quote. ChestShop is not part of the live standalone installation.

### Example: five Fireworks bundles

The existing catalog price is $200 per bundle of 16. With $1,500 and five bundles selected, review shows:

```text
Current balance: $1,500.00
Purchase cost: -$1,000.00
Balance after purchase: $500.00
5 bundles = 80 × Fireworks
$200.00 per bundle
Total: $1,000.00
```

With only $900, it shows a projected -$100.00 and “Insufficient funds. Need $100.00 more.” A $25 legacy sale against a $100 balance shows **Sale proceeds: +$25.00** and **Balance after sale: $125.00**.

## Existing integrations, not a new economy

The plugin reuses its startup-bound Vault provider. Native screens read the same global player balance used by native checkout; optional legacy screens use the same world-aware account as their legacy bridge. Missing/disabled/replaced providers, missing accounts, thrown reads and non-finite values show **unavailable**, never a fabricated zero. Zero is shown only when the provider reports a valid zero balance. Dollar/two-decimal displays match the plugin's existing dollar economy; arithmetic uses decimal values and exact integer-cent quoted totals.

The companion DeluxeMenus launcher already had a Vault balance icon. Its supported title placeholder now exposes that number without hovering, and the icon refreshes once per second. `register_command: true` is preserved immediately after `open_command: shop` so compass/programmatic commands retain the repaired route. No product slots, offers, bundle sizes or prices change.

The launcher relies on the existing PlaceholderAPI Vault expansion (`%vault_eco_balance_fixed%`). It does not share the plugin's unavailable-state renderer. Verify that the placeholder resolves accurately before installing the YAML; an unresolved token or an unavailable economy must not be accepted as a balance check. DeluxeMenus documents [title placeholders and item refresh](https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/gui). Plugin-owned screens have their own defensive read handling.

## Freshness and platform limits

Balances are snapshots, not a continuously live HUD. Opening a page, rendering an amount adjustment, submitting an amount, reopening after invalid input and reaching review/final confirmation read fresh values. Inventory headers retain their opening value while that same window stays open; the balance icon and preview update with +/- clicks. Reopening only to change a title would reintroduce the cursor-reset problem. DeluxeMenus' icon refreshes on its own interval; its header is an opening snapshot.

- Java keyboard/mouse: inventory headers, balance icon, typed-entry body and purchase tooltips are implemented and API-tested.
- Bedrock touch, controller and Windows: shared inventory headers plus native form text are implemented and API-tested, with the existing Geyser/Floodgate safety routing unchanged.
- **Actual small-screen wrapping, very large balance header clipping, controller focus, native form scrolling and older-client rendering remain player checks.** Synthetic tests do not prove real-device usability. Money-first headers prioritize the balance when space is narrow.
- Opening the owner's raw stock chest leaves the plugin UI and opens Minecraft storage normally. It does not decorate or replace the player's stock slots with money controls.

## Verification and installation scope

The final candidate passed **757 Java tests and 57 Python checks**, with zero failures, errors or skipped Java tests. It also passed **four isolated exact-Paper 26.2 build 123 boots / 1,286 assertions / zero failures**, covering clean restart, missing Economy service and missing Vault alongside balance and existing transaction regressions. All test processes exited cleanly and released the loopback port. Rebuilding produced a byte-identical JAR. Artifact-specific results are recorded in `verification/balance-display-0.1.5.json`. The repository build, existing checkout regression tests, focused balance/UI tests, generator checks and packaging checks run off-server. Existing 0.1.4 runtime evidence is historical and does not certify the new binary. A real-client acceptance pass remains required.

For a separately approved standalone installation, use **ExcelShops-0.1.5.jar** and the reviewed companion launcher YAML. Preserve the current live catalog, data and journals; do not replace prices from a generated baseline without comparison. The unchanged baseline catalogs are included only for parity checking. Do not install the optional guarded ChestShop pair on the standalone server. Back up first and use a controlled restart for the JAR; retain the command-registration fix in any YAML rollout. No rollout is performed by the build or PR.
