# Spawn Shop entry menu

**Draft confirmation upgrade; not installed live.** The generated DeluxeMenus file is now display-only. Every item opens ExcelShops' server-side quantity, review and final-confirmation flow. Neither click side withdraws money or issues a give command.

Read [ExcelShops usage and deployment instructions](../WRVShops/README.md) for the complete Java/Bedrock buyer flow, guided chest-shop setup, preserved bundle prices, administrator migration and safety limits.

Generate the paired files with `python3 make_spawnshop.py [multiplier]`. Outputs default to `spawnshop.yml` and `spawn-catalog.yml` in the current directory; optional `--output` and `--catalog-output` select separate paths. Review and install both together: the menu goes to DeluxeMenus, and the authoritative catalog goes to ExcelShops. Restart the staged plugin/server after catalog changes.

The old live menu must be replaced during approved deployment. Installing this plugin alone does not rewrite old immediate-purchase commands. Existing ChestShop signs and physical stock are not migrated.

Current branded entry command: `/excelshop`; `/wrvshop` remains a compatibility alias. See the [exact ExcelShops deployment plan](../WRVShops/DEPLOYMENT.md). The repository catalog is not proof of current live prices: compare the live menu/catalog read-only before approving installation.
