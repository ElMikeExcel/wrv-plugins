# ExcelDeathProtection 0.1.2 installation evidence

## Status

Installed on Wink Reyes Valley on **October 13, 2026**, with the server ready at **22:58:41 UTC**. The exact tested JAR was installed without rebuilding it. This records deployment and read-only server health, **not Java/Bedrock-mobile UI, real-purchase, or gameplay acceptance**. A later limited phone appearance report is recorded below; full UI, purchase, and gameplay acceptance remain pending. Source PR #13 remains draft and unmerged.

Installed source identity: [`a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`](https://github.com/ElMikeExcel/wrv-plugins/commit/a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac). The older `ff224dea03d61e0f2227e65d8015d662c93c8d34` is the historical input baseline, not the installed source identity. Later Markdown changes do not change the installed artifact.

## Server-verified active files

| Active path | Bytes | SHA-256 |
| --- | ---: | --- |
| `plugins/ExcelDeathProtection-0.1.2.jar` | 42,508 | `24b8866853fea7f86c70d8d002a9200fbe36340a8d1284bf76f28bc8684af7f4` |
| `plugins/ExcelDeathProtection/config.yml` | 150 | `d6e3208761c5b08dee1687237946b85d4821dd9f3020aa39b54bfbf082115efc` |
| `plugins/DeluxeMenus/gui_menus/spawnshop.yml` | 8,457 | `be96b3bc272d9a1004e4b4364ed8eb903686727a7a5e365f0c4afc6c19ed947d` |

All three active checksums were verified through the server file-details interface before startup. The menu checksum was reverified unchanged after startup. Only one loadable ExcelDeathProtection JAR remained; the older JARs had non-loading suffixes.

## Installed configuration and menu

- `basic-price: 15000.0` and `premium-price: 30000.0` replace the legacy price key in the active configuration.
- **Death Protection**, $15,000 in-game, uses CHEST at slot 31 (`deathprotection_basic`). Confirmation activates basic immediately without giving an item or extra powers.
- **+Death Protection Potion**, $30,000 in-game, uses POTION at slot 34 (`deathpotion`). Confirmation gives a potion to drink later. Active premium retains Conduit Power: underwater breathing, clearer underwater vision, and faster mining, including on land.
- Both left/right routes call exactly one tier-aware `deathprotection buy basic|premium %player_name%` command. Both explain confirmation within 20 seconds; the menu does not make an independent withdrawal or deliver a plain item.
- Structural comparison of the exact installed input preserved all 20 unrelated shop items and all non-item top-level settings, including the balance/header, menu size, and registration/command settings.
- Both tiers keep inventory through one real death, leave XP rules unchanged, and share one active slot. Legacy active protection and recognized bottles remain premium under the implementation contract; no manual live entitlement inspection was performed.
- No ExcelShops catalog, permission, or generator change was made. The rollout used a controlled stop/start, with no global reload.

## Read-only runtime observations

- Paper `26.2-123-main@5001879` reached its ready message at 22:58:41 UTC.
- At 22:58:38, ExcelDeathProtection enabled as **0.1.2**, logging **basic $15,000.00, premium $30,000.00**.
- At 22:59:08, the version command returned **0.1.2**.
- At 22:59:25, Vault **2.0.0** reported **EssentialsX Economy**, with LuckPerms for permission/chat.
- At 22:59:26, DeluxeMenus listed `spawnshop` with `spawnshop.yml` registered; reported TPS was **20.0 / 20.0 / 20.0**.
- At 23:00:22, `purchases.journal` existed and was **0 bytes**, with no recorded unresolved transactions at that observation. This does not exercise a purchase or prove transaction durability.
- No ExcelDeathProtection startup error, price-key warning, or journal-lock warning was observed. Unrelated existing server warnings/errors remained, including provider version compatibility, optional plugin hooks, and a query-port bind error. The entire startup log is not claimed to be error-free.

## Limited phone report, October 13 at 23:14 UTC

The owner reported that both shop entries look good on his phone. He accepts that these single-item purchases do not open the quantity window used for ordinary shop items. Clear visibility of the exact amount missing when a player cannot afford the purchase remains a reported usability concern. This report does not establish a completed purchase, activation, death/respawn, Java check, or full phone UI acceptance.

## Explicitly pending

1. **Java and remaining Bedrock-mobile UI:** Java screens, exact-shortfall feedback visibility, quote/title visibility, confirmation flow, basic bossbar, premium effect UI, and HUD coexistence. The limited phone entry-appearance report above does not cover these checks.
2. **Actual provider and gameplay:** separately authorized purchase, activation, real death/respawn, inventory/XP, persistence, legacy entitlement, and cross-plugin checks. No test purchase, grant, death, inventory edit, or balance adjustment was performed during deployment verification.
3. **Manual reconciliation:** a controlled isolated rehearsal with the actual economy provider has not been evidenced. The [journal procedure](../PURCHASE-JOURNAL.md) remains an operational gap; ambiguous transactions must stay locked pending evidence and authorization.
4. **Recoverable backup:** the fresh full backup attempt did not produce a verified completed entry. Retained old JAR/config/menu files are rollback inputs, not a complete gameplay/economy recovery snapshot. Existing managed backup entries do not establish a current tested recovery point.
5. **Generator maintenance:** the historical manual `make_spawnshop.py` workflow and repository menu remain unchanged. Future regeneration can overwrite the live hand-integrated choices. Resolve that separately before regeneration, preserving ordinary products and keeping tier prices outside general scaling.

## Recovery limits

Vault storage, the plugin journal, and playerdata are not crash-atomic. A completed save call or journal entry is not proof of durable playerdata. Preserve transaction records, server save warnings, playerdata, and economy evidence before any recovery.

Do not blindly restore 0.1.1 after 0.1.2 transactions: it interprets active byte-1 basic records as premium. Plan an explicitly authorized reconciliation/migration rather than a JAR-only downgrade. No balance, entitlement, or journal correction is authorized by this document.

See [recorded automated results](VERIFICATION-RESULTS-0.1.2.md) and the [remaining acceptance checklist](VERIFICATION.md). Successful installation does not satisfy those pending checks.
