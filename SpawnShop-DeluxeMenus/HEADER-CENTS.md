# Spawn Shop header cents correction

Config-only follow-up to ExcelShops 0.1.5. No JAR rebuild, catalog update or economy change.

The original launcher used %vault_eco_balance_fixed%. The official Vault expansion implementation formats that placeholder as a whole number: 352.82 becomes 353. Use %vault_eco_balance_2dp% in both menu_title and the balance icon display_name instead. This preserves cent precision without thousands abbreviation. The expansion uses a maximum of two decimal places, so trailing zeros may be omitted (352.80 can appear as 352.8); native plugin screens retain two fixed decimals.

Source: https://github.com/PlaceholderAPI/Vault-Expansion/blob/master/src/main/java/at/helpch/placeholderapi/expansion/vault/EconomyHook.java

## Controlled application
1. Confirm the installed Vault expansion supports balance_2dp by parsing it for an existing player and comparing the known balance. Never set money to manufacture a test. Stop if unresolved.
2. Save the current plugins/DeluxeMenus/gui_menus/spawnshop.yml privately. Replace exactly the two %vault_eco_balance_fixed% tokens with %vault_eco_balance_2dp%. Preserve all products/prices/commands and register_command: true.
3. Under owner approval use the scoped DeluxeMenus menu reload: dm reload spawnshop. Close and reopen the menu. No JAR replacement or full restart is needed. Never use Bukkit /reload.
4. Verify /shop and compass entry on phone/Bedrock and Java. 352.82 should agree across launcher and review. Zero, one-cent and trailing-zero balances should be checked when naturally available. Navigation/cancel must not charge. No purchase is needed.
5. If validation fails, restore the saved menu and repeat scoped reload. Leave economy and shop data untouched.

## Long balances and verification limits
Money remains first and unabridged. Adding cents can lengthen the title by three characters. Very large balances may be clipped by Java/Bedrock client layout; the balance icon and native review/form text provide the detailed fallback. Static tests prove no string truncation or K/M abbreviation, not physical screen fit.

All 15 menu-generator tests pass, including generated-menu parity, unchanged catalog/prices, registered command route and large/negative-value title substitution. An exact comparison with the reviewed 0.1.5 menu proves only two placeholder tokens changed. Record live expansion parsing, scoped reload and real-device acceptance separately. Historical 0.1.5 runtime tests certify the unchanged JAR, not the new installed placeholder.
