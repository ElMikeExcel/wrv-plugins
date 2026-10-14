# ExcelMobVeil 0.1.0 installation

Installed October 13, 2026, America/Chicago (October 14 UTC). The server enabled ExcelMobVeil at **02:18:11 UTC** and completed startup at **02:18:14 UTC**. This is installation evidence, separate from automated verification and real-player acceptance.

## Verified payload and shop entry

- Installed production JAR SHA-256, verified on the server: `66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc`.
- Exact reviewed source is published in [draft PR 14](https://github.com/ElMikeExcel/wrv-plugins/pull/14), initially at `be7baf81e7ad3353ab4697b67ebe1e3b74796178`. This addendum changes documentation only.
- The latest live Spawn Shop was rechecked, then one Veil Potion entry was appended in the available **slot 28**. Existing entries, including Death Protection slots 31 and 34, were retained.
- Prior menu SHA-256: `9f328011cb78bf11d86fdbf15519db96c86ea3638b0672137c7eed242d2124b2`.
- Installed menu readback SHA-256: `44b714b9a46d0f18fadaa787b3b0019746cecb7b492fa39e851f59dc3918f52a`.
- Only the public new-entry snippet is committed at [integration/deluxemenus-item.installed.yml](integration/deluxemenus-item.installed.yml); the private full menu and rollback copies are not published.
- Both click routes open the plugin's own review with `mobveil buy <player>`. The menu does not perform its own debit or grant an ordinary potion.

## Live startup checks

An empty-server check preceded the saved-world shutdown. The installed plugin reported version 0.1.0 and the $2,000 price; its configuration matched the authoritative defaults. All three existing menus and `/shop` loaded. The new purchase journal was empty, and no real purchase was performed. No ExcelMobVeil warning or error was observed.

ExcelDeathProtection remained 0.1.3 with basic $15,000 / premium $30,000 prices. ExcelShops remained 0.1.5 with EssentialsX Economy, and WealthBoard remained mobile.7. Initial empty-server TPS was 19.9/19.9/19.9; this is not a load test. Existing unrelated Essentials version-support, Query UDP bind, DeluxeMenus NMS/deprecation, optional EliteMobs model and Paper-update notices remain.

## Acceptance and rollback boundaries

Real Java/Bedrock/mobile screens, actual purchases/withdrawal/delivery, live drinking/gameplay, full production-plugin interactions and production load are unverified. Startup and the isolated 180-test/150-assertion matrix do not establish those checks. The first-version EliteMobs/dragon and damage limits in the player guide still apply. The source PR remains draft and unmerged.

Rollback copies were retained before the change. An authorized rollback should stop the server, preserve any new MobVeil player state/purchase journal for reconciliation, remove only this plugin and its one menu entry, and retain every unrelated plugin and menu item. Do not blindly replay purchases/refunds or discard uncertain transaction evidence.
