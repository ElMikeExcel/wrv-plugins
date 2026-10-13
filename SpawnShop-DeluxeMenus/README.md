# Spawn Shop entry menu

**Installed on Wink Reyes Valley on October 12, 2026.** The matching menu/catalog was installed with ExcelShops; it continues to work with standalone ExcelShops after ChestShop was removed. See the [live deployment record](../WRVShops/DEPLOYMENT-2026-10-12.md). A later player report passed menu access/prices/cancellation and a torch purchase. This is limited user-reported Java acceptance on a gaming PC; wider UI/transaction checks remain pending. The generated DeluxeMenus file is now display-only. Every item opens ExcelShops' server-side quantity, review and final-confirmation flow. Neither click side withdraws money or issues a give command.

Read [ExcelShops usage and deployment instructions](../WRVShops/README.md) for the complete Java/Bedrock buyer flow, guided chest-shop setup, preserved bundle prices, administrator migration and safety limits.

Generate the paired files with `python3 make_spawnshop.py [multiplier]`. Outputs default to `spawnshop.yml` and `spawn-catalog.yml` in the current directory; optional `--output` and `--catalog-output` select separate paths. Review and install both together: the menu goes to DeluxeMenus, and the authoritative catalog goes to ExcelShops. Restart the staged plugin/server after catalog changes.

The October 12 deployment replaced the old live menu. For any future approved deployment, the matching menu must also be replaced. Installing this plugin alone does not rewrite old immediate-purchase commands. Existing ChestShop signs and physical stock are not migrated.

Current branded entry command: `/excelshop`; `/wrvshop` remains a compatibility alias. See the [exact ExcelShops deployment plan](../WRVShops/DEPLOYMENT.md). The repository catalog is not proof of current live prices: compare the live menu/catalog read-only before approving installation.

## Server Menu shortcut cleanup

On October 13, 2026, the separate Java `server_menu.yml` stopped showing the
redundant **Jobs & Money** shortcut. Its `jobs browse` action was removed; the
Spawn Shop shortcut, shop balance display, actual job earnings and enrollment
were unchanged. Java's saved menu was reread and `dm reload server_menu`
succeeded at 02:22:39 UTC. Existing menu slots remain unchanged.

The native Bedrock change is a separate, prepared plugin hotfix and is not yet
installed. See [the exact menu-only candidate and checks](../BedrockMenu/MENU_REMOVAL.md).
