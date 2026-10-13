# ExcelDeathProtection 0.1.3: candidate verification and current operations

**The exact candidate was prepared, independently reviewed, and subsequently installed October 13, 2026.** Loaded 0.1.3 was verified at 23:51:04 UTC; see [separate installation evidence](INSTALLATION-0.1.3.md). PR #13 source commit `18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd` publishes the tested implementation and remains draft/unmerged. This page records source/artifact tests and operations, not player acceptance. Candidate preparation used no live server, real balances, entitlements, inventories, or website changes. The isolated fixture's accepted EULA was copied unchanged; no new agreement was accepted.

## Final artifact

- `ExcelDeathProtection-0.1.3.jar`, 58,521 bytes.
- SHA-256: `f0224d18a51e430b81ec8fb02f9408fd6038e64b893d84d738368fd288cb4093`.
- Baseline: installed 0.1.2 implementation, source commit `a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`; baseline JAR `24b8866853fea7f86c70d8d002a9200fbe36340a8d1284bf76f28bc8684af7f4`.
- Build: Java 21.0.12.1, Maven 3.9.9, existing pinned dependencies, offline `build.sh` with fresh target. All packaged classes target Java 21 (major 65). No test or fixture classes are packaged.
- Independent clean builds reproduce the final JAR checksum exactly.

## Review flow

Each basic/premium shop tap or buy command opens one native 27-slot inventory review with quantity fixed at 1. Product, price, current available balance, and projected remaining balance or shortfall are shown in item display names/lore. Basic remains $15,000 and activates immediately after confirmed purchase. Premium remains $30,000 and delivers a drink-later potion. Premium review text includes Conduit benefits and land mining.

Only the current owner's enabled Confirm can reach the original transaction path. Repeating buy commands, opening, previewing, refresh, Back and close do not purchase. The read-only preview updates about once per second without arming/extending tokens. An insufficient, expired, changed-price, or funded-but-unarmed review requires a fresh review and separate Confirm. Back supersedes a queued Confirm before it executes, but cannot undo a completed purchase.

Interactions use exact holder/player/inventory/session/action identities. Display-item movement, drags, shifted, double, creative, hotbar, offhand, drop, bottom-inventory and outside clicks are canceled. Stale/dead/disconnected/replaced/closed reviews and queued actions are retired without charging. Shutdown closes owned reviews.

The existing provider rechecks, shared protection slot, inventory capacity checks, one-second successful-purchase cooldown, journal locks, withdrawal validation, verified rollback/refund and legacy premium interpretation remain. `PurchaseJournal.java` and the financial transaction core from transaction UUID creation onward are byte-identical to 0.1.2. This does not make Vault and player-data persistence atomic.

## Automated tests

**99 candidate tests passed; 0 failures, 0 errors, 0 skips.**

- Review-menu adapter: 34.
- Purchase coordinator: 51, including 17 new quote/preview/confirm boundary tests.
- Purchase journal: 11.
- Resource configuration: 3.

Independent review added eight interruption/lifecycle cases: **107 tests passed in total**, including the same 99, with no failures/errors/skips. Do not add 99 and 107 as separate coverage totals.

The initial development tests caught a creative-click classification loophole, corrected with an exact LEFT/RIGHT allowlist. Independent tests caught stale pending-session/token cleanup, corrected with identity-scoped cleanup and validity-before-pending checks. Back superseding unexecuted Confirm and the funded-but-unarmed Refresh label were improved during review. Final suites pass after these fixes; superseded binaries are not release artifacts.

## Isolated Paper runtime

**196 assertions passed across seven final runs on the exact final JAR:**

| Mode | Assertions | Scope |
| --- | ---: | --- |
| normal | 74 | Existing tier delivery, effects, metadata, item retention, duplicate blocking and gameplay regression |
| restart | 12 | Separate-JVM persistence and restoration |
| native | 23 | Native potion consumption and real server death/respawn methods |
| no-economy | 6 | Missing provider safe failure |
| faults | 10 | Existing delivery/save/rollback/refund uncertainty handling |
| recovery | 3 | Journal locks survive restart |
| review | 68 | Actual buy-command dispatch, scheduled native inventory open, both-tier Confirm, price/balance/shortfalls, disabled Refresh, Back/close, canceled inventory actions, expiry/repricing and stale-action cleanup |

All seven processes exited 0 and released loopback port 25589. Every final run enforces the final JAR checksum before starting. Prior-hash development runs are archived separately and excluded from the final total.

The six historical gameplay modes call the plugin's new purchase gateway reflectively to preserve their earlier synchronous policy regression coverage; they are not passed off as UI acceptance. The additional review mode exercises actual buy command dispatch and native inventory events. Synthetic server players and a fake Vault provider are used; packet output is not a real Java or Bedrock network client. The Back route records the requested `shop` command through the synthetic player; it does not render the production DeluxeMenus menu. See runtime report for exact detail.

## Menu/config preservation

The separately packaged companion menu is based on the server-verified installed 0.1.2 menu, SHA `be96b3bc272d9a1004e4b4364ed8eb903686727a7a5e365f0c4afc6c19ed947d`. Exactly two lore instructions change to “Tap to review 1. No charge yet.” All 20 unrelated items, non-item settings, slots 31/34, prices and route commands are unchanged. No independent money withdrawal or delivery is added. The separately packaged companion config matches that recorded installed config exactly; no active-config change is required.

[SOURCE-MANIFEST-0.1.3.json](SOURCE-MANIFEST-0.1.3.json) identifies the exact implementation, tests, resources, and build inputs copied from the tested candidate. It separately records the original tested documentation snapshot and the reconciled publication docs; edited documentation bytes are not claimed to match the original snapshot. The original source patch applies to its supplied 0.1.2 candidate baseline, not automatically to later documentation commits. Initial candidate publication `18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd` was based on PR #13 head `721ebcfb02bde4edf283fffdc2cb6e8fa4681730`. This installed-status documentation follow-up is based on `18dd40ae73e1a5a01bb99f5c5f8afb2969c466cd` and changes no implementation/build/resource/test bytes. All three historical 0.1.2 evidence files, including [its manifest](SOURCE-MANIFEST-0.1.2.json), remain byte-identical to that head.

## Remaining gates

1. Recorded installation is separate from player acceptance. Keep PR #13 draft/unmerged and all actual Java, Bedrock-phone, live-purchase, and gameplay gates open; source publication and server startup do not satisfy them.
2. Real Java and Bedrock-phone readability, inventory transition, Geyser tap behavior and Back navigation through actual DeluxeMenus remain untested.
3. Actual economy-provider purchase/activation/gameplay tests require separate authorization and controlled test data. No real-balance purchase was made here.
4. Existing non-atomic save/journal boundaries, untested recovery integrity, actual-provider reconciliation rehearsal and generator-maintenance gaps remain. The later provider-completed backup below updates the earlier missing-backup status; a review window does not resolve these other limits.
5. Any future rollout must recheck live hashes and changes. The original companion YAML was based on the earlier installed snapshot; [0.1.3 installation evidence](INSTALLATION-0.1.3.md) records the separate live checks.

See [the candidate rollout plan](INSTALL-PLAN-0.1.3.md), [remaining checks](VERIFICATION.md), and [source identities](SOURCE-MANIFEST-0.1.3.json). This sanitized summary reports the frozen final candidate build, independent review, static checks and isolated runtime evidence. Prior-hash runs are excluded. Runtime used cached Paper 26.2 build 123 and Temurin Java 25.0.4 on loopback only, with no production worlds or players. Its clock/polling shortcuts and synthetic player/payment limitations remain as described above; real online-player polling cadence and client packet ordering were not established.


## Current operations update, October 13, 2026

- The provider reported a full stopped-instance managed backup completed at 23:39:15 UTC, after the earlier 0.1.2 installation. The Restore option was exposed afterward. This supersedes a current claim that no fresh completed backup exists.
- No restore test, downloaded-archive integrity check, or per-file archive-manifest verification was performed. Provider-reported completion is not independently proven recovery integrity. This snapshot predates any later 0.1.3 rollout; recheck subsequent gameplay/economy changes before relying on it.
- The earlier unsuccessful fresh-backup attempt remains accurately recorded in the unchanged [0.1.2 installation history](INSTALLATION-0.1.2.md). Retained JAR/config/menu copies alone are not a consistent full recovery snapshot.
- ExcelDeathProtection was reconfirmed as 0.1.2 at 23:40:51 UTC, then updated through the separately authorized rollout. Loaded 0.1.3 was verified at 23:51:04 UTC. [Installation evidence](INSTALLATION-0.1.3.md) records the exact active artifact and narrowly scoped file/runtime checks; none establish real-client/provider acceptance.
- The owner's prior phone report concerns only the appearance of both 0.1.2 Spawn Shop entries and acceptance of fixed-one purchases without a quantity dialog. Exact-shortfall feedback motivated 0.1.3; that concern remains pending real-phone verification. No Java, phone review-window, live-purchase, or gameplay acceptance is inferred.
