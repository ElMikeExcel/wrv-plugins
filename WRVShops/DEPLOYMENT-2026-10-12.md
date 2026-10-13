# ExcelShops installation: October 12, 2026

## Current status: ExcelShops 0.1.4 installed; real-device checks pending

The approved typed-amount update is installed standalone. Java gains **Enter Amount → private text field → Use amount**, followed by the existing review and separate final confirmation. Bedrock retains its native numeric forms. Existing in-place plus/minus controls remain available. Installation and startup are verified; none of these server checks establishes human Java, Bedrock touch/controller or Bedrock Windows acceptance.

### Identity and completed live checks

- Source: [`0225e77d3a85201c21ca5371e7d7fb8b3806939c`](https://github.com/ElMikeExcel/wrv-plugins/commit/0225e77d3a85201c21ca5371e7d7fb8b3806939c), preserved in draft PR #4. Only the standalone ExcelShops JAR was installed; ChestShop remains absent.
- Artifact: `ExcelShops-0.1.4.jar`, 202,230 bytes. Server-computed SHA-256 `565ae9e91e7a4516908e2d245f87ef1936aae23d055f084dc4159112e445f83c` matched the tested CI artifact at **01:12:16 UTC October 13 (8:12:16 p.m. CDT October 12)**.
- **01:13:15 UTC October 13 (8:13:15 p.m. CDT October 12):** ExcelShops 0.1.4 enabled against existing data with EssentialsX Economy and legacy ChestShop `unavailable/absent`. The panel was Online by **01:13:21**; an independent version query at **01:13:28** confirmed 0.1.4.
- The runtime list showed 28 plugins, without ChestShop. Vault 2.0.0 reported EssentialsX Economy and the existing LuckPerms permission/chat provider.
- **01:13:50 UTC:** the same three menus loaded: `basics_menu`, `server_menu`, `spawnshop`. Read-only checks reconfirmed Normal in the Overworld, Nether and End.
- **01:15:29 UTC (8:15:29 p.m. CDT October 12):** settled TPS was `20.0 / 20.0 / 20.0`; average tick time was 0.3 ms with no players online. This is an empty-server health observation, not a load test.
- No ExcelShops startup exception/error was observed. Existing unrelated Essentials server-version, terminal, optional-plugin and query-listener warnings remain; network settings were not changed.

The owner-approved maintenance included a fresh player check, advance warning, all-dimension save and clean stop before replacement. Complete shop data and both checkout journals were preserved. No live purchase, publication, balance, stock, catalog, permission or unrelated setting change was made during deployment. Existing Jobs settings/menu pauses, map and XP behavior were not changed. This docs-only update preserves source, binaries and artifact-specific automated evidence and does not merge the PR.

### Player acceptance still required

Use [the typed-entry instructions and device matrix](AMOUNT-ENTRY-0.1.4.md#cross-platform-acceptance-matrix). Start through **compass menu → Spawn Shop** or **your chest/sign → Edit item prices**. Verify a `0.50` price, exact whole-number quantity, invalid-input retry, Cancel/Back, Java's 60-second entry timeout, stale responses, and the separate final confirmation. Physical Java cursor behavior, phone/controller field focus and legibility, older-client compatibility, second-player chest purchases, exact buyer/seller balances and stock, and broader gameplay/load cases remain pending. Earlier 0.1.2 player reports below do not certify these new controls.

## Earlier 0.1.3 installation; human cursor retest still pending

The owner-approved inventory-button update is installed on the standalone server. Quantity and owner-price plus/minus adjustments now update the existing inventory instead of reopening it. Automated server-side checks and live startup passed; physical Java pointer behavior and Bedrock input still need player acceptance. Earlier 0.1.2 player reports below remain limited to that version's tested flows.

### Artifact and live verification

- Tested source: [`22d47bd11ba179fba5f8637278eb712d44f3d871`](https://github.com/ElMikeExcel/wrv-plugins/commit/22d47bd11ba179fba5f8637278eb712d44f3d871), preserved in draft PR #4. This update changes documentation only and does not merge the PR.
- Installed artifact: `ExcelShops-0.1.3.jar`; server-computed SHA-256 `37ac2dbbf084c0a1c1d57dbff2f886ccf2194a439de955c74765a97f1a9160a4` matches the tested build. Only one ExcelShops JAR is active; ChestShop remains absent.
- **22:57:37 UTC (5:57:37 p.m. CDT), October 12:** ExcelShops 0.1.3 enabled against existing data with legacy compatibility `unavailable/absent` and economy `EssentialsX Economy`. Runtime version was independently queried at **22:57:50**.
- **22:57:54 UTC:** native readiness returned its normal in-game-only response rather than a recovery/paused state.
- **22:58:02–22:58:28 UTC:** Vault 2.0.0 reported the EssentialsX Economy connection; the same three menus (`basics_menu`, `server_menu`, `spawnshop`) and 28 plugins loaded, without ChestShop.
- **22:58:59–22:59:08 UTC:** read-only checks reconfirmed Normal in the Overworld, Nether and End. Existing Jobs points/menu settings were not edited. mcMMO XP-2 and WeekendXP 1.0.2 loaded; the weekday vanilla rate remained 1.0×. ExcelPlayerMap was untouched.
- **22:59:15 UTC (5:59:15 p.m. CDT):** TPS was `20.0 / 19.9 / 19.9`, a point-in-time health check. A player rejoined afterward; this is not a cursor acceptance result.
- No new shop startup error was observed. Pre-existing terminal-feature and Essentials server-version warnings remain.

The warned maintenance stopped and saved the server normally. Shop data and checkout journals were retained; there were no configuration, catalog, price, permission, stock or balance edits. No live trade was performed by the installer. Existing native/Spawn review and separate final confirmation remain mandatory. The source-specific automated results are in [CURSOR-FIX-0.1.3.md](CURSOR-FIX-0.1.3.md); historical 0.1.2 harness results do not certify the new artifact.

### Remaining player checks

Start with **compass menu → Spawn Shop → an item** and **your chest/sign → Edit item prices**. Keep the pointer over plus/minus and click repeatedly; verify it stays in place, the current amount reaches Review/Use, and Back/Cancel does not purchase or publish. See the [full menu-first retest checklist](CURSOR-FIX-0.1.3.md#menu-first-human-retest-after-installation). Human Java cursor acceptance is pending. Second-player chest purchases, exact buyer/seller balances and stock, Bedrock native/fallback controls, edge cases and sustained-load checks are also pending. Normal Java chat number-entry still clears the inventory context and is not a proven workaround.

## Earlier standalone installation: ExcelShops 0.1.2

**ExcelShops 0.1.2 was installed to run native player shops and the matching Spawn Shop without ChestShop.** The owner confirmed the legacy sign plugin was unnecessary and approved its removal. That approval did not authorize restoring instant purchases or deleting shop data.

- **21:10:29 UTC (4:10:29 p.m. CDT):** guarded ChestShop was removed from the active plugin load set. Its data and the ExcelShops checkout journals were retained; the journals were empty and clean before restart.
- **21:10:42 UTC (4:10:42 p.m. CDT):** normal standalone start requested.
- **21:11:04 UTC (4:11:04 p.m. CDT):** ExcelShops 0.1.2 enabled with the expected legacy-compatibility status `unavailable/absent` and economy `EssentialsX Economy`.
- **21:11:07 UTC (4:11:07 p.m. CDT):** startup reached `Done`. At **21:11:41**, the runtime list showed 28 plugins, with no ChestShop.
- **21:12:00 UTC (4:12 p.m. CDT):** ExcelShops readiness was unpaused. **21:12:05:** the live menu list still contained only `basics_menu`, `server_menu`, and `spawnshop`.
- **21:14:08 UTC (4:14:08 p.m. CDT):** the final point-in-time TPS reading was `20.0 / 20.0 / 20.0`; this is not a long-session load test.
- **21:12:44 UTC:** Jobs configuration readback matched the staged file exactly, including `jobs-shop-enabled: false`.
- **21:13:03 UTC:** the Java compass `server_menu` Spawn Shop button was confirmed to run the `shop` player route from either click. BedrockMenu 1.4.2 loaded with native Bedrock dialogs; its live configuration contains only the auto-open setting, so routing needs a separate source check. This source/config check was not a human shopping UI pass. The later limited player report is recorded below.

The ExcelShops JAR, native shop controls, matching Spawn Shop prices/catalog, Jobs item-shop pause and scoped example-menu pauses were not changed by the removal. The optional guarded ChestShop source, legal notices and earlier artifact hash below remain historical provenance. Current gameplay instructions lead with **compass menu → Spawn Shop**; chat commands are optional fallbacks.

A limited player report arrived later on October 12, as recorded below. Startup, menu-route inspection and a registered command alone are not a real-player purchase, cancellation or balance/item acceptance pass.

## Earlier paired installation and provenance

ExcelShops 0.1.2 and guarded ChestShop were initially installed together at 20:58 UTC (3:58 p.m. CDT). The guarded compatibility plugin was subsequently removed as described above; the 0.1.2 artifact remained installed until the later 0.1.3 update recorded above.

The deployed files match the approved build from [`d082342b1d8f9a6adbb987d86c9f1e2758fec6bf`](https://github.com/ElMikeExcel/wrv-plugins/commit/d082342b1d8f9a6adbb987d86c9f1e2758fec6bf), preserved in [draft PR #4](https://github.com/ElMikeExcel/wrv-plugins/pull/4). This documentation update does not change source, binaries or build evidence, and does not merge the PR.

| Artifact | Runtime version | Host-computed SHA-256 |
| --- | --- | --- |
| `ExcelShops-0.1.2.jar` | `0.1.2` | `009f321a99f30024bcaf6742e9b2dc9dfba23571eaddddad1eee9699d6fe9edc` |
| `ChestShop-3.13-pre-1-build474-confirmation.1.jar` | `3.13-pre-1 (build 474)+confirmation.1` | `5e7bbf6014484ff87b5decced2df32e36b05b3aaa6c26ba03d57c72314cdd814` |

At this initial paired installation, both runtime versions and host hashes were independently checked. Exactly one active JAR for each shop plugin was observed. Existing ChestShop data was retained; no prior WRVShops/ExcelShops installation was present.

## Earlier paired-install live checks

Times are October 12 UTC, followed by America/Chicago daylight time (UTC−5).

- **20:58:29 (3:58:29 p.m. CDT):** ExcelShops enabled and reported the legacy confirmation guard connected, using `EssentialsX Economy`.
- **20:58:32 (3:58:32 p.m. CDT):** normal startup completed; server Online.
- **Through 21:02 (4:02 p.m. CDT):** both installed JAR hashes, versions, stored settings and menu load state were checked. TPS was **20.0 at 21:02:23 (4:02:23 p.m. CDT)**; this is a point-in-time reading, not a long-session performance result.
- The 18 pre-install Spawn Shop material IDs, bundle sizes and both click prices matched the frozen catalog. The replacement display/checkout menu was reopened and matched the reviewed file exactly.
- Jobs `jobs-shop-enabled: false` persisted after restart. The scoped edit preserved every unrelated configuration byte; normal Jobs earning/progress was not removed.
- Only the DeluxeMenus `advanced_menu` and `requirements_menu` registrations were removed. Their example files were retained. Runtime `dm list` showed exactly `basics_menu`, `server_menu`, and `spawnshop`; the two purchasing examples did not regenerate in that list.
- `/excelshop` returned its normal in-game-only console response, rather than a paused/recovery response. This checks command registration, not a player screen or purchase.
- Vault 2.0.0 identified itself as `roydogman/Vault-Updated`; `vault-info` reported EssentialsX Economy and the existing LuckPerms integration. EssentialsX 2.22.0 loaded. This confirms the startup provider connection; it does not establish successful player debits, credits or delivery.
- No permissions, claims, tax policy, economy balances, stock or sign data were intentionally changed. The previously checked ChestShop zero-tax, two-decimal and empty server-account policy was retained.
- No shop startup/recovery error was observed in the reviewed startup block. Existing unrelated Essentials server-version and terminal-feature warnings remain.

## Earlier 0.1.2 player report and remaining acceptance

At about **22:12 UTC (5:12 p.m. CDT) on October 12**, the owner relayed a player's results (later confirmed as Java on a gaming PC):

- Spawn Shop menu access and displayed prices worked.
- Cancelling did not purchase.
- A torch purchase completed with the expected result.
- The player created and published their own chest shop through the owner setup flow.

These are user-reported results, not an independently observed session. The owner later confirmed that the tester was using Java on a gaming PC. The exact client version was not supplied; Bedrock phone acceptance remains pending. The report does not establish every item price, exhaustive cancellation/repeated-confirmation behavior, cross-platform acceptance or long-session reliability.

**Still pending:** a second player buying from the newly published shop, including chest and attached-sign entry, exact buyer/seller balance changes, stock decrement and protection. Inventory capacity, insufficient funds, distance/stale sessions, reconnect/restart persistence and broader platform checks remain open. No live trade was performed by the installer; the later player report is separate evidence.

**Issue reported on 0.1.2:** the player reported the cursor moving/resetting while using price plus/minus controls. Source review found price and quantity button handlers recreating and closing/reopening their inventory. The later installed 0.1.3 update keeps these adjustments in the same inventory; human pointer verification is still pending. Native Bedrock number forms and inventory fallback are distinct UI paths. Do not present `/excelshop amount` through normal Java chat as a proven workaround: closing the inventory clears its number-entry context.

Both the shop chest and its attached sign are supported entry points. The owner gets management; another player gets the purchase catalog. The earlier 0.1.2 setup success message mentioned only the chest. Installed 0.1.3 now mentions both chest and attached sign; this does not claim second-player sign purchases have passed.

Use the [player and seller checklist](PLAYER-CHECKLIST.md), prioritizing the compass menu for normal shopping. Further money/item-changing checks need the owner's authorization.

The pre-deployment isolated harness used Vault 1.7.3. Its original evidence and the full guarded ChestShop corresponding-source/legal bundle are preserved unchanged. The live Vault 2.0.0 connection is additional startup evidence only. The scoped Jobs/example-menu pauses do not prove that every possible server purchase route is covered or that unrelated trading systems have been tested.

Current player instructions are maintained on the [existing website](https://elmikeexcel.github.io/winkreyesvalley/#shops-preview). The [deployment plan](DEPLOYMENT.md) and [pause specification](PURCHASE-PAUSE-SPEC.md) remain operational guidance, not new authorization to change the server or reopen unconfirmed purchase routes.
