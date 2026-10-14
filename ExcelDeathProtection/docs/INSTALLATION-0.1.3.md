# ExcelDeathProtection 0.1.3 installation evidence

## Status and source identity

**Installed on Wink Reyes Valley October 13, 2026.** The plugin enabled at **23:50:52 UTC** and the runtime version command confirmed **0.1.3 at 23:51:04 UTC**. The active JAR matches the exact independently tested artifact. This records an authorized installation and narrowly scoped read-only file/server checks, **not real Java, Bedrock-phone, actual-purchase, or gameplay acceptance**. All those acceptance checks remain pending. PR #13 remains draft and unmerged.

Installed implementation/source publication: [`18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd`](https://github.com/ElMikeExcel/wrv-plugins/commit/18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd). This source commit contains the exact tested implementation, resources, build inputs and tests. Later installed-status documentation does not change or rebuild the artifact. [The manifest](SOURCE-MANIFEST-0.1.3.json) distinguishes the original tested snapshot, initial publication and current documentation.

## Server-verified active files

| Active path | Bytes | SHA-256 |
| --- | ---: | --- |
| `plugins/ExcelDeathProtection-0.1.3.jar` | 58,521 | `f0224d18a51e430b81ec8fb02f9408fd6038e64b893d84d738368fd288cb4093` |
| `plugins/ExcelDeathProtection/config.yml` | 150 | `d6e3208761c5b08dee1687237946b85d4821dd9f3020aa39b54bfbf082115efc` |
| `plugins/DeluxeMenus/gui_menus/spawnshop.yml` | 8,449 | `9f328011cb78bf11d86fdbf15519db96c86ea3638b0672137c7eed242d2124b2` |

The active JAR/menu hashes were checked before startup; the JAR check was recorded at 23:50:26 UTC. The unchanged active configuration hash was checked again at 23:51:54 UTC and the menu hash at 23:52:12 UTC. Exactly one loadable ExcelDeathProtection JAR remained; older artifacts had non-loading rollback suffixes. The JAR matches the frozen final artifact; the menu matches the separately reviewed two-instruction-line delta. File checks do not establish client rendering or transaction behavior.

## Narrow installed change

- Install the exact reviewed 0.1.3 plugin artifact and replace only the two product instructions with **“Tap to review 1. No charge yet.”**
- Retain basic **Death Protection**, CHEST at slot 31, **$15,000**, and premium **+Death Protection Potion**, POTION at slot 34, **$30,000**. Keep existing tier-specific left/right command routes unchanged.
- Preserve all 20 unrelated shop items and non-item settings. The menu hash matches the reviewed companion whose structural comparison established that narrow delta.
- Retain the existing active configuration with `basic-price: 15000.0` and `premium-price: 30000.0`. The packaged default-config comment differs by version, but the active configuration did not require replacement.
- The native review fixes quantity at one and requires its current enabled Confirm to reach the existing journaled purchase path. Opening, repeating buy commands, refreshing, Back and closing have no purchase side effect under the tested implementation. Actual Java/phone behavior remains a separate gate.
- The historical repository menu and manual generator remain unchanged. Regeneration can overwrite the installed integration; resolve that in a separately scoped change before regeneration. No ExcelShops catalog/payment route is introduced.

## Read-only runtime observations

- **23:50:52 UTC:** ExcelDeathProtection enabled as 0.1.3; startup prices were **basic $15,000.00 / premium $30,000.00**.
- **23:51:04 UTC:** runtime version confirmed 0.1.3.
- Vault **2.0.0** reported **EssentialsX Economy**; the Spawn Shop remained registered.
- At **23:51:40 UTC**, `purchases.journal` was unchanged at **0 bytes** with its original startup timestamp; it was not reset or rotated. This is not a purchase test, persistence test, or proof of durable delivery.
- At **23:52:28 UTC**, read-only health checks reported **TPS 20.0 / 20.0 / 20.0**. No players were online during the reported runtime verification.
- No ExcelDeathProtection error was observed in the relevant startup checks. This does not claim the whole server log is error-free or certify unrelated plugins.

## Backup and recovery limits

A full stopped-instance managed backup was provider-reported completed at **23:39:15 UTC**, and a Restore option was exposed afterward. It captures the instance after 0.1.2 installation and before this 0.1.3 rollout. No restore, downloaded-archive integrity check, or per-file archive-manifest verification was performed. Provider completion is not independently proven recovery integrity. Recheck later gameplay/economy activity before relying on any recovery point.

The original 0.1.2 JAR and prior menu were retained under non-loading rollback filenames; the active configuration remained byte-identical. The rollout used a controlled stop/start, with no global reload or new backup/deletion. The earlier unsuccessful backup attempt remains accurately preserved in [0.1.2 installation history](INSTALLATION-0.1.2.md). Retained individual plugin/config/menu files do not substitute for a consistent full gameplay/economy recovery snapshot.

Vault, journal and playerdata persistence remain non-atomic. A normal player-save return, empty journal or completed journal entry does not prove disk durability. Preserve transaction evidence and obtain authorization before any balance, entitlement, journal or rollback correction. Do not blindly downgrade to 0.1.1 or clear an unresolved lock.

## Acceptance still pending

1. Real **Java** review appearance and transitions, balance/shortfall readability, Confirm, refresh/expiry, Back through the actual DeluxeMenus `/shop`, and no-charge open/close behavior.
2. Real **Bedrock-phone/Geyser** rendering, lore readability, tap behavior and the same complete review flow. The owner's earlier 0.1.2 report covered only the appearance of the two shop entries and acceptance of fixed-one purchases without a quantity dialog. It does not validate this new review. Exact-shortfall visibility remains pending real-phone verification.
3. Separately authorized real-provider purchases and activation, basic/premium gameplay, actual death/respawn, inventory/XP behavior, persistence, legacy compatibility and cross-plugin checks. No real purchase, item grant, balance adjustment, player death, or inventory modification was performed as a test. No such result is inferred from startup.
4. Controlled actual-provider manual reconciliation, recovery integrity, and the menu-generator maintenance gap.

See [automated results](VERIFICATION-RESULTS-0.1.3.md), [remaining checklist](VERIFICATION.md), [rollout procedure](INSTALL-PLAN-0.1.3.md), and [journal limitations](../PURCHASE-JOURNAL.md). The 99 candidate tests, 107 including reviewer additions, and 196 isolated Paper assertions remain distinct from these unrun live acceptance checks.
