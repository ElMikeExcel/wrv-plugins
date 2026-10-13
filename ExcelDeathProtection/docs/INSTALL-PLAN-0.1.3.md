# ExcelDeathProtection 0.1.3: rollout plan and verification gates

This source candidate and its tested artifact are separate from a verified server installation. Version 0.1.2 is the last verified installed version at October 13, 2026, 23:40:51 UTC. Follow the owner's separately granted rollout authorization; this document grants none. Actual Java and Bedrock-phone acceptance and any real-balance test purchase are separate gates.

## Proposed scope

1. Recheck the actual installed version, player activity, economy provider and exact active files. The prior observed 0.1.2 JAR SHA-256 was `24b8866853fea7f86c70d8d002a9200fbe36340a8d1284bf76f28bc8684af7f4`; the active menu was `be96b3bc272d9a1004e4b4364ed8eb903686727a7a5e365f0c4afc6c19ed947d`. These are historical observations, not permission to overwrite later changes.
2. Coordinate approved maintenance and confirm an appropriate consistent recovery point. A full stopped-instance managed backup was provider-reported completed at 23:39:15 UTC, with Restore exposed afterward; it captures the instance after 0.1.2 installation. No restore or archive-integrity test was performed. Recheck any subsequent player/economy activity before relying on it. Preserve the earlier 0.1.2 failed-attempt history, and do not substitute a partial file copy for a consistent snapshot.
3. Preserve the current JAR, menu, config, journal, player data and economy evidence. Do not reset or delete `purchases.journal` or player protection records. Do not alter balances.
4. Replace only the loadable ExcelDeathProtection JAR with the reviewed 0.1.3 artifact: 58,521 bytes, SHA-256 `f0224d18a51e430b81ec8fb02f9408fd6038e64b893d84d738368fd288cb4093`. Retain a non-loading rollback copy. Keep the existing active config: the price keys and values are unchanged. The packaged default-config comment is versioned, but no active-config replacement is required.
5. Merge the two reviewed instruction-line changes into the freshly checked live menu. Slots 31/34 and the commands remain unchanged. The candidate changes only “Tap twice within 20 seconds to buy.” to “Tap to review 1. No charge yet.” Do not overwrite unrelated later menu edits or use the historical repository menu as the baseline.
6. Start normally; verify loaded version 0.1.3, JAR/menu hashes, configured prices $15,000/$30,000, Vault/Essentials provider, menu registration, player health and relevant logs. No global reload.
7. Separately verify real Java and Bedrock phone review appearance, balance/shortfall readability, delayed inventory transition, expiry, Back into DeluxeMenus `/shop`, and no-charge open/close behavior. A separately approved controlled purchase is required to establish actual-provider transaction/activation behavior. Synthetic fixture tests are not that approval or proof.

## Candidate behavior

- Every tier-specific or legacy buy command opens a fixed-quantity-1 native inventory review. Repeated commands cannot confirm a purchase.
- Opening, refreshing, Back and close make no payment or grant. A current enabled Confirm performs the existing journaled purchase path after all current checks.
- Short balances show the missing amount. Available balances show the projected remainder. Live preview runs about once per second.
- A 20-second expired review, changed price or unarmed quote requires Refresh review and a distinct Confirm. Back can cancel a queued Confirm before it executes; it cannot reverse an already completed purchase.
- A known failed delivery retains the existing verified rollback/refund path. Ambiguous outcomes remain locked for manual reconciliation.

## Rollback constraints

This candidate does not change the entitlement or journal formats introduced by 0.1.2. Rollback still requires a consistent, authorized decision based on actual transactions and server evidence. Do not blindly downgrade to 0.1.1: it interprets new basic active records as premium and does not implement the current journal safety model. Never clear a safety lock simply to resume purchases.

## Remaining integration caveat

The ordinary menu/generator source must eventually retain the two reviewed instruction lines. This source publication does not edit the live generator/menu, repository `SpawnShop-DeluxeMenus/spawnshop.yml` or `make_spawnshop.py`, website, or ExcelShops catalog. The tested companion YAML was based on the authoritative installed 0.1.2 menu snapshot, not a fresh live read. The [unassigned source template](../integration/spawnshop-death-protection.template.yml) is a review aid, not a loadable full menu. See [current evidence and operations](VERIFICATION-RESULTS-0.1.3.md).
